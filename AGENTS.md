# RealWorld Clojure + Datastar — Project Context

## Purpose

This project is a **learning-by-building** exercise for someone who knows basic Clojure
syntax but has zero experience building real applications with Clojure.
Throughout those exercises, the user will learn Clojure idioms, debugging techniques, and the
workflow of Clojure project development and deployment.

The goal is to implement the [RealWorld spec](https://docs.realworld.show/introduction/) —
a Medium.com clone — using Clojure on both backend and frontend (no ClojureScript).
The frontend is server-rendered HTML (Hiccup) with [Datastar](https://data-star.dev) for
interactivity. The project is deliberately structured as a **staged curriculum** so each
stage teaches a new layer of the real-world Clojure ecosystem, including tooling, libraries,
and REPL-driven development practices.

Never write Clojure code for the user. Always ask them to write code from the REPL, and guide them through the process. The REPL is the primary interface for learning and development in this project, not terminal or editor commands.

## What is RealWorld?

RealWorld is a standardised spec for a Medium-clone that includes:
- A **REST API backend** with 20 endpoints (auth, users, articles, comments, tags, favorites)
- A **frontend** with 7 pages (home, login/register, editor, article, profile, settings)
- An official Hurl test suite that validates spec compliance

See: https://docs.realworld.show/introduction/

## What the Learner Already Knows

- Basic Clojure syntax
- **No** experience with Clojure tooling, libraries, or project structure
- **No** ClojureScript experience (and we are not using it)

## Learning Goals

Beyond just building the app, the learner wants to understand:
1. Real-world Clojure tooling (Clojure CLI, deps.edn)
2. REPL-driven development workflow
3. Backend patterns: Ring middleware, routing, DB access, auth
4. Frontend patterns: server-rendered Hiccup, Datastar SSE fragments, cookie auth
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
- **buddy-auth** — JWT signing/verification
- **malli** — schema validation
- **Hiccup** — server-side HTML templating

### Frontend
- **Datastar** — loaded from CDN; signals + SSE fragment merging (no build step)
- Auth via **HttpOnly JWT cookie** — server sets on login, middleware reads on every HTML request
- No ClojureScript, no npm, no build pipeline

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
├── deps.edn                ← deps + aliases
├── src/
│   └── realworld/
│       ├── core.clj        ← entry point + Integrant system config
│       ├── routes.clj      ← API routes (/api/*) + HTML routes
│       ├── auth.clj        ← JWT sign/verify + cookie middleware
│       ├── sse.clj         ← Datastar SSE response helpers
│       ├── db/             ← next.jdbc queries, one ns per domain
│       ├── handlers/       ← JSON API handlers, one ns per domain
│       └── views/          ← Hiccup page/component functions, one ns per page
├── test/
│   └── realworld/
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
