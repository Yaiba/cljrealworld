# RealWorld Clojure + Datastar — Learning Progress

## Current Stage: Stage 1 — Tooling & REPL Mastery

---

## Stage 1: Tooling & REPL Mastery
**Goal**: Get a real Clojure dev environment working. Build REPL muscle memory before writing any app code.
**Estimated time**: ~1 week

### 1.1 Install & configure tools
- [x] Install Clojure CLI (`brew install clojure/tools/clojure`)
- [x] Verify: `clj --version` works
- [x] Install editor REPL integration (Calva for VS Code)
- [x] Install Portal for data inspection (via `:dev` alias in `deps.edn`)

### 1.2 Understand deps.edn
- [x] Create `deps.edn` with `:deps`, `:paths`, `:aliases`
- [x] Add a `:dev` alias with nREPL + Portal + `dev/` on classpath
- [x] Connect Calva editor to the REPL with `:dev` alias
- [x] Understand how aliases compose: `clj -M:dev:test`

### 1.3 REPL-driven development basics
- [x] Evaluate expressions inline from the editor (`Ctrl+Enter` in Calva)
- [x] Reload a changed function without restarting (`require :reload`)
- [x] Navigate to a definition from the REPL (`clojure.repl/source`, `doc`)
- [x] Inspect data in Portal: `(tap> some-data)`
- [x] Understand namespaces: `(ns ...)`, `(require '[...])`, `(in-ns '...)`

### 1.4 Stage 1 Exercise
- [x] Write `src/realworld/core.clj` with:
  - Filters articles by a tag
  - Groups them by author
  - Returns the top N most prolific authors (multi-arity)
- [x] Do ALL work from the REPL
- [x] Visualise results in Portal with `tap>`

**Stage 1 complete when**: You can evaluate, reload, and inspect code from your editor without touching the terminal.

---

## Stage 2: Backend Foundation — HTTP + Routing
**Goal**: Understand Ring's middleware model. Get a JSON API skeleton running.
**Estimated time**: ~1 week

### 2.1 Ring fundamentals
- [x] Add Ring, http-kit, Reitit, Muuntaja to `deps.edn`
- [x] Write a bare Ring handler: `(defn handler [req] {:status 200 :body "ok"})`
- [x] Call it directly from the REPL (no server needed!)
- [x] Start http-kit server from the REPL, stop it from the REPL
- [x] Understand: request map, response map, middleware = function wrapping a function

### 2.2 Reitit routing
- [x] Define routes as a data vector (not macros)
- [x] Add Muuntaja for JSON ↔ EDN coercion
- [ ] Add query param and path param coercion
- [x] Handle 404 and method-not-allowed

### 2.3 In-memory state
- [x] Use a Clojure `atom` as a fake database
- [x] Write pure functions that take/return the atom's value (not side-effectful)

### 2.4 Build first 4 endpoints (in-memory, no DB yet)
- [x] `POST /api/users` — register (store in atom)
- [x] `POST /api/users/login` — login (return fake token)
- [x] `GET  /api/user` — get current user
- [x] `GET  /api/tags` — return hardcoded tags

### 2.5 REPL skill: test handlers without HTTP
- [x] Call `(handler (mock-request :get "/api/tags"))` from REPL
- [x] Assert response shape from the REPL

**Stage 2 complete when**: All 4 endpoints return correct JSON, tested from REPL.

---

## Stage 3: Backend — Database & Auth
**Goal**: Replace atom with Postgres. Add real JWT auth. Complete all 20 endpoints.
**Estimated time**: ~2 weeks

### 3.1 Database setup
- [x] Install PostgreSQL locally (or run via Docker)
- [x] Add next.jdbc + HoneySQL to `deps.edn`
- [x] Write `db/connection.clj` with a connection pool (HikariCP)
- [x] Write first query with HoneySQL: `{:select [:*] :from [:users]}`
- [x] Understand: `execute!` vs `execute-one!`, result maps

### 3.2 Schema + migrations
- [x] Add Migratus or Flyway for migrations
- [x] Write SQL migrations for: users, articles, comments, tags, favorites, follows

### 3.3 Integrant system
- [x] Add Integrant to `deps.edn`
- [x] Define system config in `core.clj`: `:db/pool`, `:server/http`
- [x] Write `dev/user.clj` with `(reset)`, `(start)`, `(stop)` helpers
- [x] Practice: change a handler, call `(reset)`, re-test — never restart the JVM

### 3.4 JWT authentication
- [x] Add buddy-auth to `deps.edn`
- [x] Write `auth.clj`: `sign-token`, `verify-token`
- [x] Write Ring middleware: extract `Authorization: Token ...` header
- [x] Inject `:identity` into request map for downstream handlers

### 3.5 Validation with malli
- [x] Define malli schemas for User, Article, Comment
- [x] Plug malli into Reitit coercion
- [x] Return spec-compliant error responses on validation failure

### 3.6 Complete all 20 endpoints
- [x] Users: register, login, get current user, update user
- [x] Profiles: get profile, follow, unfollow
- [x] Articles: list (with filters), feed, get, create, update, delete
- [x] Comments: add, get, delete
- [x] Favorites: favorite, unfavorite
- [x] Tags: list

**Stage 3 complete when**: All endpoints work with Postgres, JWT auth is enforced.

---

## Stage 4: Testing
**Goal**: Learn Clojure testing idioms. Validate against the official RealWorld test suite.
**Estimated time**: ~3–4 days (parallel with Stage 3)

### 4.1 clojure.test basics
- [x] Write first `deftest` for a pure function (no HTTP, no DB)
- [x] Use `is`, `are`, `testing` blocks
- [x] Run tests from the REPL: `(clojure.test/run-tests)`

### 4.2 kaocha test runner
- [x] Add kaocha, configure `tests.edn`
- [x] Run `clj -M:test` in watch mode
- [x] Understand kaocha plugins (output, diff)

### 4.3 Handler tests
- [x] Test Ring handlers directly (no HTTP): call handler with mock request map
- [x] Use `with-redefs` to mock DB calls
- [x] Test middleware in isolation

### 4.4 Official RealWorld test suite
- [x] Install Hurl (`brew install hurl`)
- [x] Run the official Hurl test suite against your server
- [x] Fix any failing tests
- [x] All Hurl tests passing ✓

**Stage 4 complete when**: `clj -M:test` passes and Hurl suite is green.

---

## Stage 5: Datastar Frontend Foundation
**Goal**: Understand the Datastar model (signals + SSE fragments). Serve HTML from Clojure with no build step.
**Estimated time**: ~1 week

### 5.1 Server-side HTML setup
- [x] Add Hiccup to `deps.edn` for HTML templating
- [x] Add a `/` route to a separate HTML router (not the API router) that returns `text/html`
- [x] Write a `views/layout.clj` with a base HTML shell that loads Datastar from CDN (`<script type="module" src="https://cdn.jsdelivr.net/npm/@starfederation/datastar@1.0.0-RC.8">`)
- [x] Compose HTML router and API router with `ring/routes` — HTML router has no fallback handler so unmatched routes fall through to the API router
- [x] Verify: hitting `/` in the browser returns a page

### 5.2 Datastar fundamentals
- [x] Understand the three core primitives: `data-signals` (client state), `data-on:*` (event → server call), `data-bind` (two-way input binding)
- [x] Understand SSE responses: Datastar RC.8 uses `event: datastar-patch-elements` + `data: elements <fragment>` (not `datastar-merge-fragments`)
- [x] Write a hello-world: a button with `data-on:click="@get(\"/hello\")"` that server-responds with a patched `<div>`
- [x] Understand: Datastar patches DOM fragments by `id` — your server owns the HTML
- [x] Note: attribute names use colon separator (`data-on:click`), not hyphen (`data-on-click`)

### 5.3 SSE response helpers
- [x] Write `sse.clj` with `merge-fragment` helper using `datastar-patch-elements` event format
- [x] Understand: for a single response a plain string body works; `InputStream` needed only for true streaming

### 5.4 Auth flow with Datastar
- [x] Login page: form fields bound with `data-bind`, submit with `data-on:submit="@post(\"/login\")"`
- [x] Server validates credentials, signs a JWT, sets it as an `HttpOnly` cookie (`Set-Cookie: token=...; HttpOnly; Path=/`)
- [x] Write `make-cookie-auth-middleware` in `server.clj`: reads JWT from cookie, verifies it, injects `:identity` into request
- [x] Apply middleware to HTML router only; redirect to `/login` if cookie absent or invalid
- [x] Existing `/api/*` JSON routes keep `Authorization: Token` header auth unchanged (Hurl tests stay green)
- [x] Use official Clojure Datastar SDK (`dev.data-star.clojure/http-kit`) for SSE responses

### 5.5 Stage 5 mini-app
- [x] Login page → authenticate → redirect to home
- [x] Home page: server renders article feed HTML
- [x] Navigation between pages via normal `<a>` links (no JS router needed)

**Stage 5 complete when**: You can log in and see the article feed rendered by the server.

**Stage 5 complete ✓**

---

## Stage 6: Frontend Complete (DEFERRED — returning after Stage 7 & 8)

> **Decision**: Stage 6 requires building many new server-side handlers for each page. We're parking it to first complete the ClojureScript SPA (Stages 7–8), then come back to finish the Datastar frontend with more frontend experience.


**Goal**: Build all 7 pages as server-rendered Hiccup with Datastar for interactive fragments.
**Estimated time**: ~2–3 weeks

### 6.1 Page: Home (`/`)
- [ ] Global feed tab + personal feed tab (auth required)
- [ ] Tag sidebar from `GET /api/tags`
- [ ] Tag filter: clicking a tag fires `@get('/articles?tag=...')` → server returns updated feed fragment
- [ ] Pagination via Datastar signals + server-rendered page fragments

### 6.2 Page: Auth (`/login`, `/register`)
- [ ] Login form with inline error display (server merges error fragment on failure)
- [ ] Register form with inline error display
- [ ] On success: server sets cookie + returns redirect signal (`data-signals` `{location: '/'}`)

### 6.3 Page: Settings (`/settings`)
- [ ] Server pre-fills form from current user on page load
- [ ] Form submit → `@put('/api/user')` → server merges success/error fragment
- [ ] Logout button → server clears cookie → redirect to home

### 6.4 Page: Editor (`/editor`, `/editor/:slug`)
- [ ] Create article: form submit → `@post('/api/articles')` → redirect to article page
- [ ] Edit article: server pre-fills form, `@put('/api/articles/:slug')`
- [ ] Tag input: add/remove tags as chips using Datastar signals for local chip state

### 6.5 Page: Article (`/article/:slug`)
- [ ] Server renders article body as Markdown (use a JVM Markdown lib like `commonmark-java`)
- [ ] Comments section: server renders comments HTML
- [ ] Post comment: `@post(...)` → server returns new comment fragment merged into list
- [ ] Delete comment: `@delete(...)` → server returns `datastar-remove-fragments`
- [ ] Favorite / follow buttons: `@post(...)` → server merges updated button fragment

### 6.6 Page: Profile (`/profile/:username`)
- [ ] User's articles tab / favorited articles tab — tab switch via `@get(...)` fragment swap
- [ ] Follow / unfollow button as a mergeable fragment

### 6.7 Polish
- [ ] Loading states: use `data-on-*__loading` modifier or a global signal to show spinners
- [ ] Error fragments: consistent server-rendered error banner component
- [ ] Auth-gated routes: Ring middleware redirects unauthenticated requests
- [ ] 404 page as a standard Ring handler

**Stage 6 complete when**: All 7 pages work end-to-end, driven by server-rendered HTML and Datastar fragment merges.

---

## Stage 7: ClojureScript Frontend Foundation
**Goal**: Understand the ClojureScript build pipeline and the Replicant + DataScript + Nexus mental model. The key insight: UI is a pure function of DataScript state; Nexus dispatches actions as data; DataScript is your single queryable store.
**Stack**: Replicant (`no.cjohansen/replicant`), DataScript (`datascript/datascript`), Nexus (`no.cjohansen/nexus`), reitit-frontend
**Estimated time**: ~1–2 weeks

### 7.1 shadow-cljs setup
- [x] Add `shadow-cljs.edn` with `:app` build targeting `public/js/app.js`
- [x] Add Replicant, DataScript, Nexus, reitit to `shadow-cljs.edn` `:dependencies`
- [x] Start `npx shadow-cljs watch app` — verify browser connects and hot-reloads
- [x] Open ClojureScript browser REPL from Calva
- [x] Evaluate a ClojureScript expression from the REPL that prints to the browser console

### 7.2 Replicant basics — UI as pure data
- [x] Understand the core model: `(defn ui [state] hiccup)` — no atoms, no subscriptions, no local state
- [x] Call `replicant.dom/render` from the REPL with a hiccup vector — see it appear in the DOM
- [x] Re-call `render` with different hiccup — observe surgical DOM update (open DevTools Elements panel)
- [x] Understand: Replicant diffs previous vs current hiccup, only touches changed nodes
- [x] Compare to Datastar: server owned the HTML; now you own hiccup data on the client
- [x] Build a standalone counter: a function that takes `{:count n}` and returns hiccup with a button; drive it from REPL by calling `render` with different state maps

### 7.3 DataScript basics — Datalog as your app state
> **SKIPPED** — sticking with atom throughout; DataScript adds complexity not needed for this learning path

### 7.4 Nexus basics — actions as pure data ✓
> **Adapted** — using Nexus with atom (not DataScript); effects call `swap!` instead of `d/transact!`
- [x] Understand the split: **actions** = pure data vectors describing intent; **effects** = functions that execute side effects
- [x] Define a nexus map with `:nexus/actions` (pure handlers) and `:nexus/effects` (functions that call `swap!` or `js/fetch`)
- [x] Dispatch an action from the REPL: `(nexus/dispatch app-nexus state {} [[:app/increment]])`
- [x] Understand event placeholders: `[:event.target/value]` interpolated via `:nexus/placeholders`
- [x] Connect Replicant to Nexus: `(replicant.dom/set-dispatch! ...)`

### 7.5 Wiring it all together — the data loop with Nexus ✓
> **Adapted** — atom + add-watch + Nexus (no DataScript)
- [x] Replace inline `fn` event handlers in hiccup with Nexus action vectors
- [x] Move all `swap!` calls into Nexus effects
- [x] Verify: the full cycle works — DOM event → Nexus dispatch → effect → swap! → add-watch → render

### 7.6 Routing
- [x] Add reitit-frontend with `push-state`
- [x] Define routes as a data vector — same style as backend routes
- [x] Store current route in atom
- [x] Navigate between two pages from the REPL by swapping the atom

### 7.7 Stage 7 mini-app
- [x] Login page: form with email/password fields, submit POSTs to `/api/users/login`
- [x] On success: store JWT + user in atom, navigate to home
- [x] Home page: fetch articles from `/api/articles`, store in atom, render as hiccup
- [x] Routing between login ↔ home works via push-state

### 7.8 Testing — the stack is pure, so testing is easy
- [ ] Understand the key insight: views, queries, and action handlers are all pure functions → test them with `cljs.test` by just calling them with data
- [ ] Add a `:test` build to `shadow-cljs.edn` using `:target :node-test` — run tests with `npx shadow-cljs compile test && node out/test.js`
- [ ] **Test a view**: call your counter hiccup function with `{:count 3}`, assert the result is a hiccup vector containing `"3"` — no browser, no DOM, no mocking
- [ ] **Test a DataScript query**: create a seeded test DB with `(d/db-with (d/empty-db schema) [{:article/id "1" :article/title "Hello"}])`, call your query helper, assert the result
- [ ] **Test a Nexus action handler**: call the pure handler function directly with a state snapshot, assert it returns the expected action vector — no dispatch, no side effects
- [ ] **Test a Nexus effect** (stub pattern): build a test nexus map where the real HTTP effect is replaced with a stub that captures its args; dispatch an action, assert the stub was called with the right data
- [ ] **Integration test**: create a fresh DataScript conn, dispatch `[:user/login ...]` through Nexus using stub HTTP that returns a fixture response, assert the JWT and user are now in the DB

**Stage 7 complete when**: You can log in and see the article feed, driven by DataScript state, rendered by Replicant, wired through Nexus actions.

---

## Stage 8: ClojureScript Frontend Complete
**Goal**: Build all 7 RealWorld pages using Replicant + DataScript + Nexus. Every page is a pure hiccup function of DataScript state. Compare the experience against the Datastar version.
**Estimated time**: ~2–3 weeks

### 8.1 DataScript schema — model the whole domain
- [x] Define full schema: users, articles, comments, tags, favorites, follows — with `:db/unique`, `:db/valueType :db.type/ref`, `:db/cardinality :db.cardinality/many` where appropriate
- [x] Write `queries.cljc` namespace with named query helpers
- [x] Verify queries from the REPL with seeded test data

### 8.2 Nexus actions + effects — full API coverage
- [x] Write `actions.cljc`: pure handlers split into own namespace
- [x] Write `effects.cljs`: HTTP effect functions, each calls `d/transact!` on success
- [x] Write an `:http/request` effect that handles auth header injection (reads JWT from DataScript)
- [x] Split codebase: schema.cljc, constants.cljc, queries.cljc, actions.cljc, effects.cljs, routes.cljs, views/

### 8.3 Page: Home (`/`)
- [x] Global feed tab + personal feed tab (query DataScript for feed type)
- [x] Tag sidebar: transact tags from `GET /api/tags` on page load; query from DataScript for render
- [x] Tag filter: dispatch `[:feed/set-tag tag]` → transact filter into DataScript → hiccup re-renders
- [x] Pagination: store current page in DataScript; prev/next dispatch actions

### 8.4 Page: Auth (`/login`, `/register`)
- [x] Login form: dispatch `[:user/login fields]` → HTTP effect → transact user + JWT → navigate
- [x] Register form: dispatch `[:user/register fields]`
- [x] Inline error display: transact errors into DataScript, query in view
- [x] On page load: check DataScript for existing JWT (restored from localStorage) → skip login if present

### 8.5 Page: Settings (`/settings`)
- [x] Pre-fill form: pull current user from DataScript
- [x] Submit dispatches `[:user/update fields]` → PUT `/api/user` → retransact updated user
- [x] Logout: dispatch `[:user/logout]` → retract user entity from DataScript + clear localStorage

### 8.6 Page: Editor (`/editor`, `/editor/:slug`)
- [ ] Create: dispatch `[:article/create fields]` → POST → transact new article → navigate to article page
- [ ] Edit: pull existing article from DataScript to pre-fill form; dispatch `[:article/update slug fields]`
- [ ] Tag chips: store draft tags as a DataScript attribute; add/remove via Nexus actions

### 8.7 Page: Article (`/article/:slug`)
- [ ] Render Markdown body: use JS interop with `marked` library loaded via npm
- [ ] Comments: transact comments from `GET /api/articles/:slug/comments` on page load; query for render
- [ ] Post comment: dispatch `[:comment/create slug body]` → POST → transact new comment
- [ ] Delete comment: dispatch `[:comment/delete slug id]` → DELETE → retract entity from DataScript
- [ ] Favorite / follow: dispatch actions → optimistic DataScript update → HTTP effect

### 8.8 Page: Profile (`/profile/:username`)
- [ ] User articles tab / favorited articles tab: store active tab in DataScript
- [ ] Follow / unfollow: dispatch action → optimistic update in DataScript

### 8.9 Testing — full coverage
- [ ] **Query tests** (`queries_test.cljs`): for every query helper in `queries.cljs`, write a test that seeds a minimal DataScript DB and asserts the return value — these are the fastest, most reliable tests
- [ ] **Action handler tests** (`actions_test.cljs`): for each handler in `actions.cljs`, assert the output action sequence for a given input state — pure functions, zero setup needed
- [ ] **View snapshot tests** (`views_test.cljs`): call each page view function with a representative state map, assert the top-level hiccup structure (tag, key attributes) — catch regressions without a browser
- [ ] **Effect integration tests** (`effects_test.cljs`): for the HTTP effect, use a stub `js/fetch` (via `with-redefs` in ClojureScript) — dispatch the action, assert the fetch was called with the correct URL, headers, and body
- [ ] **Full loop integration test**: create a fresh conn, wire up a test nexus with stub HTTP, drive a multi-step user flow (e.g. login → fetch feed → favorite an article), assert final DataScript state at each step
- [ ] Run `npx shadow-cljs compile test && node out/test.js` — all tests green before Stage 8 is done

### 8.10 Polish
- [ ] Loading states: transact `:app/loading? true` before HTTP calls, retract on completion; query in views
- [ ] Error states: transact error messages into DataScript; display in views; clear on navigation
- [ ] Auth-gated routes: check DataScript for current user on route change; redirect to login if absent
- [ ] 404 page as a default route handler

**Stage 8 complete when**: All 7 pages work end-to-end via the ClojureScript SPA hitting the same backend.

---

## Review

_Fill this in when the project is complete._

- What worked well?
- What was hardest?
- What would you do differently?
- What Clojure patterns became second nature?
