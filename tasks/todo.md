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
- [ ] Login page: form fields bound with `data-bind`, submit with `data-on-submit="@post('/login')"`
- [ ] Server validates credentials, signs a JWT, sets it as an `HttpOnly` cookie (`Set-Cookie: token=...; HttpOnly; Path=/`)
- [ ] Write Ring middleware for HTML routes: reads JWT from cookie, verifies it, injects `:identity` into request — no `Authorization` header needed
- [ ] Protect HTML routes with the middleware; redirect to `/login` if cookie is absent or invalid
- [ ] Note: existing `/api/*` JSON routes keep `Authorization: Token` header auth unchanged (Hurl tests stay green)

### 5.5 Stage 5 mini-app
- [ ] Login page → authenticate → redirect to home
- [ ] Home page: server renders article feed HTML, tags sidebar
- [ ] Navigation between pages via normal `<a>` links (no JS router needed)

**Stage 5 complete when**: You can log in and see the article feed rendered by the server.

---

## Stage 6: Frontend Complete
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
**Goal**: Understand the ClojureScript build pipeline. Get re-frame's event loop in your head. You have already built the same UI in Hiccup+Datastar — use that mental model as a reference point.
**Estimated time**: ~1 week

### 7.1 shadow-cljs setup
- [ ] Add `shadow-cljs.edn` with `:app` build
- [ ] Add Reagent, re-frame, reitit to ClojureScript deps
- [ ] Start `npx shadow-cljs watch app` — get browser connected
- [ ] Open ClojureScript browser REPL from editor
- [ ] Evaluate a ClojureScript expression that modifies the DOM live

### 7.2 Reagent basics
- [ ] Write a component as a function returning hiccup
- [ ] Use `r/atom` for local state
- [ ] Understand: when does a component re-render?
- [ ] Build a standalone counter component from REPL

### 7.3 re-frame fundamentals
- [ ] Understand the 6-domino cycle: event → handler → db → subscription → view → DOM
- [ ] Compare to Datastar: `app-db` ≈ signals, subscriptions ≈ derived signals, event handlers ≈ SSE responses
- [ ] Define `app-db` shape for the whole app
- [ ] Write first event handler with `reg-event-db`
- [ ] Write first subscription with `reg-sub`
- [ ] Wire a view component to a subscription

### 7.4 HTTP calls
- [ ] Add re-frame-http-fx
- [ ] Write an event that fires an HTTP GET to your backend `/api/tags`
- [ ] Store response in `app-db`, display in a component

### 7.5 Routing
- [ ] Configure reitit-frontend with `push-state`
- [ ] Define routes as data (mirrors backend routing style)
- [ ] Navigate between two pages from REPL: `(rf/dispatch [:navigate :home])`

### 7.6 Stage 7 mini-app
- [ ] Login page: form → POST `/api/users/login` → store JWT in `app-db` + localStorage
- [ ] Home page: fetch + display global article feed
- [ ] Routing between login ↔ home

**Stage 7 complete when**: You can log in and see the article feed in the browser via the ClojureScript SPA.

---

## Stage 8: ClojureScript Frontend Complete
**Goal**: Build all 7 pages in re-frame. Wire up all API calls. Compare the experience against the Datastar version you already built.
**Estimated time**: ~2–3 weeks

### 8.1 Page: Home (`/`)
- [ ] Global feed tab + personal feed tab (auth required)
- [ ] Tag sidebar from `GET /api/tags`
- [ ] Tag filter: click tag → filtered feed
- [ ] Pagination

### 8.2 Page: Auth (`/login`, `/register`)
- [ ] Login form with error display
- [ ] Register form with error display
- [ ] Redirect to home on success
- [ ] Persist JWT to localStorage, restore on page reload

### 8.3 Page: Settings (`/settings`)
- [ ] Pre-fill form from current user data
- [ ] `PUT /api/user` on submit
- [ ] Logout button (clear app-db + localStorage)

### 8.4 Page: Editor (`/editor`, `/editor/:slug`)
- [ ] Create new article: `POST /api/articles`
- [ ] Edit existing article: `PUT /api/articles/:slug`
- [ ] Tag input (add/remove tags as chips)

### 8.5 Page: Article (`/article/:slug`)
- [ ] Render article body as Markdown (JS interop with `marked`)
- [ ] Display comments
- [ ] Post comment / delete comment
- [ ] Favorite / unfavorite button
- [ ] Follow / unfollow author button

### 8.6 Page: Profile (`/profile/:username`)
- [ ] User's articles tab
- [ ] Favorited articles tab
- [ ] Follow / unfollow button

### 8.7 Polish
- [ ] Loading states for all async operations
- [ ] Error states with user-friendly messages
- [ ] Auth-gated routes (redirect to login if not authenticated)
- [ ] 404 page

**Stage 8 complete when**: All 7 pages work end-to-end via the ClojureScript SPA hitting the same backend.

---

## Review

_Fill this in when the project is complete._

- What worked well?
- What was hardest?
- What would you do differently?
- What Clojure patterns became second nature?
