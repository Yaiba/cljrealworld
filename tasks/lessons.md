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
