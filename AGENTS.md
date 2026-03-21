# RealWorld Clojure/ClojureScript — Project Context

## Purpose

This project is a **learning-by-building** exercise for someone who knows basic Clojure
syntax but has zero experience building real applications with Clojure or ClojureScript.
Throughout those exercises, user will learn more Clojure idioms, debug technics and the 
workflow of Clojure project development and deployment.

The goal is to implement the [RealWorld spec](https://docs.realworld.show/introduction/) —
a Medium.com clone — using a full Clojure stack. The project is deliberately structured
as a **staged curriculum** so each stage teaches a new layer of the real-world Clojure
ecosystem, including tooling, libraries, and REPL-driven development practices.

Never write Clojure code for user. Always ask them to write code from the REPL, and guide them through the process. The REPL is the primary interface for learning and development in this project, not a terminal or editor commands.

## What is RealWorld?

RealWorld is a standardised spec for a Medium-clone that includes:
- A **REST API backend** with 20 endpoints (auth, users, articles, comments, tags, favorites)
- A **frontend SPA** with 7 pages (home, login/register, editor, article, profile, settings)
- An official Hurl test suite that validates spec compliance

See: https://docs.realworld.show/introduction/

## What the Learner Already Knows

- Basic Clojure syntax
- **No** experience with Clojure tooling, libraries, or project structure
- **No** ClojureScript experience

## Learning Goals

Beyond just building the app, the learner wants to understand:
1. Real-world Clojure tooling (Clojure CLI, deps.edn, shadow-cljs)
2. REPL-driven development workflow
3. Backend patterns: Ring middleware, routing, DB access, auth
4. Frontend patterns: Reagent, re-frame, ClojureScript build pipeline
5. Testing idioms in Clojure

## Tech Stack

### Backend (Clojure)
- **Clojure CLI + deps.edn** — build tool
- **Ring** — HTTP abstraction
- **Reitit** — data-driven routing + coercion
- **Muuntaja** — JSON/EDN content negotiation
- **http-kit** — HTTP server
- **next.jdbc** — JDBC database wrapper
- **HoneySQL** — SQL as Clojure data
- **Integrant** — system lifecycle management
- **buddy-auth** — JWT authentication
- **malli** — schema validation

### Frontend (ClojureScript)
- **shadow-cljs** — ClojureScript build tool
- **Reagent** — React bindings (hiccup syntax)
- **re-frame** — state management (event/subscription model)
- **reitit** — client-side routing
- **re-frame-http-fx** — HTTP effects

### Tooling / Dev
- **nREPL** — editor-connected REPL
- **Calva / CIDER / Cursive** — editor integration
- **Portal** — data visualisation in REPL
- **kaocha** — test runner
- **Hurl** — official API test suite

## Project Structure (target)

```
cljrealworld/
├── AGENTS.md               ← this file
├── deps.edn                ← backend deps + aliases
├── shadow-cljs.edn         ← frontend build config
├── src/
│   ├── backend/
│   │   ├── core.clj        ← entry point + Integrant system config
│   │   ├── routes.clj      ← all API routes
│   │   ├── auth.clj        ← JWT middleware
│   │   ├── db/             ← next.jdbc queries, one ns per domain
│   │   └── handlers/       ← request handlers, one ns per domain
│   └── frontend/
│       ├── core.cljs       ← app entry, re-frame init
│       ├── routes.cljs     ← reitit frontend routes
│       ├── events.cljs     ← re-frame event handlers
│       ├── subs.cljs       ← re-frame subscriptions
│       └── views/          ← one ns per page
├── test/
│   └── backend/
└── dev/
    └── user.clj            ← REPL dev helpers (integrant reset etc.)
```

## Workflow Rules for Claude

- Always check `tasks/todo.md` for current stage and progress before suggesting next steps
- Always update `tasks/todo.md` checkboxes as items are completed
- Record any new patterns, mistakes, or corrections in `tasks/lessons.md`
- Prefer REPL-verifiable examples — show how to test things from the REPL, not just run commands
- Explain *why* a library/pattern exists before showing how to use it
- Stage gates: do not move to next stage until the current stage's exercises are working
