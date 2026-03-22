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
- [ ] Add Hiccup to `deps.edn` for HTML templating
- [ ] Add a `/` route to your Reitit router that returns `text/html`
- [ ] Write a `views/layout.clj` with a base HTML shell that loads Datastar from CDN (`<script type="module" src="https://cdn.jsdelivr.net/npm/@starfederation/datastar">`)
- [ ] Verify: hitting `/` in the browser returns a styled page

### 5.2 Datastar fundamentals
- [ ] Understand the three core primitives: `data-signals` (client state), `data-on-*` (event → server call), `data-bind` (two-way input binding)
- [ ] Understand SSE responses: Datastar reads a stream of `event: datastar-merge-fragments` + `data: <fragment>` chunks
- [ ] Write a hello-world: a button with `data-on-click="@get('/hello')"` that server-responds with a merged `<div>`
- [ ] Understand: Datastar replaces/merges DOM fragments by `id` — your server owns the HTML

### 5.3 SSE response helpers
- [ ] Write `sse.clj` with helpers: `merge-fragment`, `merge-signals`, `remove-fragments`
- [ ] Understand Ring's `:body` as an `InputStream` for streaming SSE
- [ ] Test from REPL: call your SSE handler and inspect the raw stream output

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

## Review

_Fill this in when the project is complete._

- What worked well?
- What was hardest?
- What would you do differently?
- What Clojure patterns became second nature?
