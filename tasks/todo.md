# RealWorld Clojure/ClojureScript — Learning Progress

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
- [ ] Understand how aliases compose: `clj -M:dev:test`

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
- [ ] Add Ring, http-kit, Reitit, Muuntaja to `deps.edn`
- [ ] Write a bare Ring handler: `(defn handler [req] {:status 200 :body "ok"})`
- [ ] Call it directly from the REPL (no server needed!)
- [ ] Start http-kit server from the REPL, stop it from the REPL
- [ ] Understand: request map, response map, middleware = function wrapping a function

### 2.2 Reitit routing
- [ ] Define routes as a data vector (not macros)
- [ ] Add Muuntaja for JSON ↔ EDN coercion
- [ ] Add query param and path param coercion
- [ ] Handle 404 and method-not-allowed

### 2.3 In-memory state
- [ ] Use a Clojure `atom` as a fake database
- [ ] Write pure functions that take/return the atom's value (not side-effectful)

### 2.4 Build first 4 endpoints (in-memory, no DB yet)
- [ ] `POST /api/users` — register (store in atom)
- [ ] `POST /api/users/login` — login (return fake token)
- [ ] `GET  /api/user` — get current user
- [ ] `GET  /api/tags` — return hardcoded tags

### 2.5 REPL skill: test handlers without HTTP
- [ ] Call `(handler (mock-request :get "/api/tags"))` from REPL
- [ ] Assert response shape from the REPL

**Stage 2 complete when**: All 4 endpoints return correct JSON, tested from REPL.

---

## Stage 3: Backend — Database & Auth
**Goal**: Replace atom with Postgres. Add real JWT auth. Complete all 20 endpoints.
**Estimated time**: ~2 weeks

### 3.1 Database setup
- [ ] Install PostgreSQL locally (or run via Docker)
- [ ] Add next.jdbc + HoneySQL to `deps.edn`
- [ ] Write `db/connection.clj` with a connection pool (HikariCP)
- [ ] Write first query with HoneySQL: `{:select [:*] :from [:users]}`
- [ ] Understand: `execute!` vs `execute-one!`, result maps

### 3.2 Schema + migrations
- [ ] Add Migratus or Flyway for migrations
- [ ] Write SQL migrations for: users, articles, comments, tags, favorites, follows

### 3.3 Integrant system
- [ ] Add Integrant to `deps.edn`
- [ ] Define system config in `core.clj`: `:db/pool`, `:server/http`
- [ ] Write `dev/user.clj` with `(reset)`, `(start)`, `(stop)` helpers
- [ ] Practice: change a handler, call `(reset)`, re-test — never restart the JVM

### 3.4 JWT authentication
- [ ] Add buddy-auth to `deps.edn`
- [ ] Write `auth.clj`: `sign-token`, `verify-token`
- [ ] Write Ring middleware: extract `Authorization: Token ...` header
- [ ] Inject `:identity` into request map for downstream handlers

### 3.5 Validation with malli
- [ ] Define malli schemas for User, Article, Comment
- [ ] Plug malli into Reitit coercion
- [ ] Return spec-compliant error responses on validation failure

### 3.6 Complete all 20 endpoints
- [ ] Users: register, login, get current user, update user
- [ ] Profiles: get profile, follow, unfollow
- [ ] Articles: list (with filters), feed, get, create, update, delete
- [ ] Comments: add, get, delete
- [ ] Favorites: favorite, unfavorite
- [ ] Tags: list

**Stage 3 complete when**: All endpoints work with Postgres, JWT auth is enforced.

---

## Stage 4: Testing
**Goal**: Learn Clojure testing idioms. Validate against the official RealWorld test suite.
**Estimated time**: ~3–4 days (parallel with Stage 3)

### 4.1 clojure.test basics
- [ ] Write first `deftest` for a pure function (no HTTP, no DB)
- [ ] Use `is`, `are`, `testing` blocks
- [ ] Run tests from the REPL: `(clojure.test/run-tests)`

### 4.2 kaocha test runner
- [ ] Add kaocha, configure `tests.edn`
- [ ] Run `clj -M:test` in watch mode
- [ ] Understand kaocha plugins (output, diff)

### 4.3 Handler tests
- [ ] Test Ring handlers directly (no HTTP): call handler with mock request map
- [ ] Use `with-redefs` to mock DB calls
- [ ] Test middleware in isolation

### 4.4 Official RealWorld test suite
- [ ] Install Hurl (`brew install hurl`)
- [ ] Run the official Hurl test suite against your server
- [ ] Fix any failing tests
- [ ] All Hurl tests passing ✓

**Stage 4 complete when**: `clj -M:test` passes and Hurl suite is green.

---

## Stage 5: ClojureScript Frontend Foundation
**Goal**: Understand the ClojureScript build pipeline. Get re-frame's event loop in your head.
**Estimated time**: ~1 week

### 5.1 shadow-cljs setup
- [ ] Add `shadow-cljs.edn` with `:app` build
- [ ] Add Reagent, re-frame, reitit to ClojureScript deps
- [ ] Start `npx shadow-cljs watch app` — get browser connected
- [ ] Open ClojureScript browser REPL from editor
- [ ] Evaluate a ClojureScript expression that modifies the DOM live

### 5.2 Reagent basics
- [ ] Write a component as a function returning hiccup
- [ ] Use `r/atom` for local state
- [ ] Understand: when does a component re-render?
- [ ] Build a standalone counter component from REPL

### 5.3 re-frame fundamentals
- [ ] Understand the 6-domino cycle: event → handler → db → subscription → view → DOM
- [ ] Define `app-db` shape for the whole app
- [ ] Write first event handler with `reg-event-db`
- [ ] Write first subscription with `reg-sub`
- [ ] Wire a view component to a subscription

### 5.4 HTTP calls
- [ ] Add re-frame-http-fx
- [ ] Write an event that fires an HTTP GET to your backend `/api/tags`
- [ ] Store response in `app-db`, display in a component

### 5.5 Routing
- [ ] Configure reitit-frontend with `push-state`
- [ ] Define routes as data (mirrors backend routing style)
- [ ] Navigate between two pages from REPL: `(rf/dispatch [:navigate :home])`

### 5.6 Stage 5 mini-app
- [ ] Login page: form → POST `/api/users/login` → store JWT in `app-db` + localStorage
- [ ] Home page: fetch + display global article feed
- [ ] Routing between login ↔ home

**Stage 5 complete when**: You can log in and see the article feed in the browser.

---

## Stage 6: Frontend Complete
**Goal**: Build all 7 pages. Wire up all API calls. Polish error states.
**Estimated time**: ~2–3 weeks

### 6.1 Page: Home (`/`)
- [ ] Global feed tab + personal feed tab (auth required)
- [ ] Tag sidebar from `GET /api/tags`
- [ ] Tag filter: click tag → filtered feed
- [ ] Pagination

### 6.2 Page: Auth (`/login`, `/register`)
- [ ] Login form with error display
- [ ] Register form with error display
- [ ] Redirect to home on success
- [ ] Persist JWT to localStorage, restore on page reload

### 6.3 Page: Settings (`/settings`)
- [ ] Pre-fill form from current user data
- [ ] `PUT /api/user` on submit
- [ ] Logout button (clear app-db + localStorage)

### 6.4 Page: Editor (`/editor`, `/editor/:slug`)
- [ ] Create new article: `POST /api/articles`
- [ ] Edit existing article: `PUT /api/articles/:slug`
- [ ] Tag input (add/remove tags as chips)

### 6.5 Page: Article (`/article/:slug`)
- [ ] Render article body as Markdown (JS interop with `marked`)
- [ ] Display comments
- [ ] Post comment / delete comment
- [ ] Favorite / unfavorite button
- [ ] Follow / unfollow author button

### 6.6 Page: Profile (`/profile/:username`)
- [ ] User's articles tab
- [ ] Favorited articles tab
- [ ] Follow / unfollow button

### 6.7 Polish
- [ ] Loading states for all async operations
- [ ] Error states with user-friendly messages
- [ ] Auth-gated routes (redirect to login if not authenticated)
- [ ] 404 page

**Stage 6 complete when**: All 7 pages work end-to-end with the real backend.

---

## Review

_Fill this in when the project is complete._

- What worked well?
- What was hardest?
- What would you do differently?
- What Clojure patterns became second nature?
