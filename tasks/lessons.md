# Lessons Learned

Patterns, mistakes, and corrections captured during the build.
Updated after every significant learning moment or correction.

---

## The Replicant + Nexus data loop

```
DOM event
  → hiccup :on handler (data vector, not a function)
      e.g. [:app/set-email [:event.target/value]]
  → Replicant sees non-function, calls set-dispatch! handler
  → Nexus resolves placeholders ([:event.target/value] → actual string from DOM event)
  → Nexus looks up action in :nexus/actions, calls it with current state + args (pure fn)
      (fn [_state email] [[:state/assoc :email email]])
  → action returns a vector of effect vectors (pure data, no side effects)
  → Nexus looks up each effect in :nexus/effects, calls it with [ctx system & args]
      (fn [_ctx system k v] (swap! system assoc k v))
  → swap! updates the atom
  → add-watch fires with new state
  → Replicant re-renders: (d/render el (ui new-state))
  → DOM updated surgically
```

Key rules:
- Actions are **pure** — they only look at state and return data
- Effects are **impure** — they call swap!, js/fetch, rfe/push-state, etc.
- Placeholders like `[:event.target/value]` are **vectors**, not keywords
- Placeholder functions are defined in `:nexus/placeholders` and receive the replicant dispatch-data map (which contains `:replicant/dom-event`)
- `set-dispatch!` wires Replicant → Nexus: `(d/set-dispatch! (fn [replicant-data action-vec] (nexus/dispatch app-nexus state replicant-data [action-vec])))`

---

## DataScript fundamentals (Stage 8)

### conn vs db
- `conn` = mutable connection (like an atom). Call `d/transact!` on it to write data.
- `db` = immutable snapshot. Get one with `(d/db conn)`. Run all queries against `db`, not `conn`.

### Temporary IDs (tempids)
- Negative integers like `-1`, `-2` are tempids — they only exist during a transaction.
- Purpose: cross-reference entities being inserted in the same transaction.
- DataScript replaces them with real positive integer IDs on commit.
- After transact, `-1` is gone — the real entity is `1`, `2`, etc.

### Schema — only declare what's special
- You don't need to declare every attribute, only ones that need special treatment:
  - **References**: `{:db/valueType :db.type/ref}` — this attribute points to another entity
  - **Multi-valued**: `{:db/cardinality :db.cardinality/many}` — e.g. tags on an article
- Everything else (strings, numbers, booleans) just works without a schema entry.

### Reading data — three ways
- `d/pull` — get a nested map for one entity: `(d/pull db '[*] entity-id)`
  - `[*]` = all attributes; refs come back as `{:db/id N}` (just the pointer)
  - To follow a ref: use nested pattern `{:article/author [:user/username]}`
  - Cardinality-many attributes come back as a vector
- `d/q` — Datalog query, returns a set of tuples by default
- `d/entity` — lazy entity lookup (less commonly used)

### Datalog query structure
```clojure
(d/q '[:find  ?title ?username      ; what to return
       :in    $ ?filter-val          ; $ = the db, extra args after
       :where [?a :article/title ?title]   ; triple patterns: [entity attr value]
              [?a :article/author ?u]      ; variables shared across patterns = JOIN
              [?u :user/username ?filter-val]]
     db "bob")
```
- Variables start with `?` — shared variables across `:where` patterns act as joins
- Literals in patterns act as filters
- `:in` is optional — use when passing extra filter values

### UI singleton entity pattern (the "magic" entity)
- Use a fixed entity (e.g. `(def UI-ENTITY 1)`) to store all app-level UI state in DataScript.
- This keeps one store for everything — domain data and UI state — with one consistent read/write API.
- Write UI state: `(d/transact! conn [{:db/id UI-ENTITY :app/page :page/home}])`
- Read UI state: `(d/pull (d/db conn) '[:app/page :app/email] UI-ENTITY)`
- Each transact only touches the attributes you specify — other attributes are untouched, behaves like `assoc`.
- Add `:app/current-user {:db/valueType :db.type/ref}` to schema if UI entity needs to point to a domain entity.

### DataScript cannot store nested Clojure maps as attribute values
- Attributes hold scalars (strings, numbers, keywords), refs (`:db/id` pointers), or cardinality-many of those.
- To store a "nested" thing (e.g. the current user from an API response), transact it as its own entity and link via a ref.
- Pattern: use a tempid to link both entities in one transaction:
  ```clojure
  (d/transact! conn [{:db/id -1 :user/username "bob" :user/token "jwt..."}
                     {:db/id UI-ENTITY :app/current-user -1}])
  ```
- Then pull with a nested pattern to read it back:
  ```clojure
  (d/pull (d/db conn) '[{:app/current-user [:user/username :user/token]}] UI-ENTITY)
  ```

### DataScript does not store nil values
- Transacting `nil` for an attribute is a no-op or error — DataScript represents "no value" by the attribute simply not existing on the entity.
- Don't initialise optional attributes to `nil`. Either omit them, or set a real default value.
- When pulling, a missing attribute just won't appear in the result map — handle it the same as `nil` in your UI code.

### Schema design principle — which side owns the relationship?
- Store a relationship on the entity that **initiates** it, not the entity that receives it.
- Example: a user favorites an article → `:user/favorites` (on user), not `:article/favorited-by` (on article)
- Datalog can traverse refs in both directions, so queries work regardless of which side you store on.
- The choice is about semantics: who "does" the action?

### :find shapes control result shape
- `?x ?y` → set of tuples: `#{["a" 1] ["b" 2]}`
- `[?x ...]` → flat vector: `["a" "b"]`
- `?x .` → single scalar: `"a"`
- `[?x ?y]` → single tuple: `["a" 1]`

### Use `.cljc` for pure frontend namespaces to enable JVM testing
- Pure functions (views, action handlers, query helpers) have no JS interop — they can live in `.cljc` files.
- `.cljc` files compile on both JVM (Clojure) and browser (ClojureScript).
- Benefit: test views and actions from the Clojure REPL without a browser — just call the function with data and assert the hiccup structure.
- Rule of thumb: if a function uses `js/...`, `d/listen!`, `rfe/...`, or any browser API → `.cljs`. If it's pure data in / data out → `.cljc`.
- Typical split:
  - `.cljc`: `views/`, `actions.cljs`, `queries.cljs`
  - `.cljs`: `core.cljs`, `effects.cljs`, `routes.cljs`

### Use `defonce` for stateful things that must survive hot reloads
- `def conn` recreates the conn on every shadow-cljs hot reload — listeners and dispatch wiring point to the old conn, causing inconsistent state.
- `defonce conn` creates the conn once and keeps it across reloads.
- Same rule applies to any stateful top-level: router setup, event listeners, etc.
- Use `^:dev/after-load` to re-render after hot reload without re-initialising:
  ```clojure
  (defn ^:dev/after-load re-render []
    (d/render (js/document.getElementById "app") (ui (d/db conn))))
  ```
- `init` should only run once (on first load); `re-render` runs after every reload.

### When migrating from atom to DataScript conn, update ALL references
- `nexus/dispatch` takes the system as its second argument — must be `conn`, not the old atom.
- `d/set-dispatch!` callback must pass `conn` to `nexus/dispatch`, not the old `state` atom.
- `d/db` converts conn → db. Never call `d/db` on a value that is already a db — it will throw `(conn? conn)` assertion error.
- Rule: `d/db` at the boundary (in `system->state`, in `d/listen!` callback); everywhere else pass the db value directly.

### `mapcat` = map + flatten
- Use `mapcat` when each element produces a collection and you want one flat result.
- e.g. normalizing API articles: `(mapcat normalize-article articles)` — each article produces 2 entity maps, mapcat flattens all into one list for a single `d/transact!`.

### `some?` instead of `(not (nil? v))`
- `(some? x)` returns true if x is not nil — cleaner than `(not (nil? x))`.
- Common use: strip nil values from a map before transacting into DataScript:
  ```clojure
  (into {} (filter (fn [[_ v]] (some? v)) m))
  ```

### pull inside :find — the most useful UI query pattern
- Embed a `pull` expression inside `:find` to return full nested maps instead of raw values.
- `[(pull ?e [:article/title {:article/author [:user/username]}]) ...]`
- Returns a flat vector of maps — exactly the shape views need, no post-processing required.
- Combine with `:where` patterns to filter, then pull rich data for each match.
- Example:
  ```clojure
  (d/q '[:find [(pull ?a [:article/title :article/tags {:article/author [:user/username]}]) ...]
         :where [?a :article/slug _]]
       db)
  ;; => [{:article/title "Hello" :article/tags ["clojure"] :article/author {:user/username "bob"}}]
  ```

### ClojureScript dev namespace — the `user.clj` equivalent

- The JVM auto-loads `user.clj` on startup — ClojureScript has no equivalent auto-load.
- In ClojureScript, use shadow-cljs `:preloads` to load a dev namespace before `init-fn` runs.
- Use `:repl-init-ns` to start the ClojureScript REPL in that namespace (same as JVM drops into `user`).
- Name and path: if your source root is `dev/frontend/`, the file `dev/frontend/dev.cljs` → namespace `dev`. Namespace mirrors file path relative to source root.
- Because `:preloads` run before `init-fn`, don't call anything at the top level that depends on app state (like `conn`). Put it in a function and call it manually from the REPL once after the app loads.
- Typical setup:
  ```clojure
  ;; deps.edn
  :cljs-dev {:extra-paths ["dev/frontend"]
             :extra-deps  {no.cjohansen/dataspex {:mvn/version "..."}}}

  ;; shadow-cljs.edn
  :devtools {:preloads      [dev]
             :repl-init-ns  dev}

  ;; dev/frontend/dev.cljs
  (ns dev (:require [frontend.core :refer [conn]] [dataspex :as dataspex]))
  (defn setup [] (dataspex/inspect "App state" conn))
  ;; then call (dev/setup) once from the REPL
  ```

### Retracting entities from DataScript

DataScript has no "delete where" — to remove entities you must query for their IDs first, then retract.

Three retraction operations:
```clojure
[:db/retract eid :article/title "Old Title"]  ; remove one attribute value
[:db/retractEntity eid]                        ; remove entity + all its attributes
```

The pattern for "clear and replace" (e.g. replacing article feed on tag filter):
```clojure
(let [existing-ids (d/q '[:find [?a ...] :where [?a :article/slug _]] db)
      retractions  (map (fn [eid] [:db/retractEntity eid]) existing-ids)]
  (d/transact! conn (concat retractions new-entities)))
```

`d/transact!` accepts mixed tx-data — retractions and additions in one atomic transaction.

**Gotcha:** `:db/retractEntity` does NOT cascade through refs. If an article has `:article/author` pointing to a user, the user entity survives — only the ref attribute on the article is removed.

---

### Unique identity attributes enable upsert and lookup refs
- Declare `:db/unique :db.unique/identity` on natural keys like `:user/username` and `:article/slug`.
- Upsert: transacting an entity with a unique attribute that already exists merges with the existing entity — no duplicates.
- No tempids needed for entities with unique attributes — DataScript finds the existing entity by the unique key.
- Lookup refs: reference an entity by its unique attribute instead of `:db/id`:
  `[:user/username "bob"]` — works anywhere a `:db/id` would work.
- This is essential when normalizing API responses where the same author appears in multiple articles.
