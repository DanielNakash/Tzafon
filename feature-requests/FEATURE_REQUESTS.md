# Tzafon — Feature Requests (inbox)

This is the **only file you edit by hand**. Append a request block, set `status: new`, save.
The automation does the rest (see `feature-requests/README.md`).

**Pipeline switch:** to pause all automation, change the line below to `pipeline: paused`.

pipeline: active

---

## How to add a request

Copy the template, give it a unique id (`FR-YYYY-MM-DD-x`), write a plain-language
description of what you want. Leave the `(auto)` fields alone — the groomer/builder fill them.

```
## FR-2026-07-08-a
status: new
target: (auto)
requested: 2026-07-08
implemented: (auto)

<one or more sentences describing the feature in your own words>
```

Status lifecycle: `new` → `groomed` (requirements written) → `implemented` (shipped in a version).
A request that conflicts with a locked decision (e.g. gamification, `DM-NOT`) is set `rejected`
with a `reason:` line and never built.

---

<!-- Add requests below this line. -->
## FR-2026-07-10-a
status: groomed
target: 2.1.0
When creating a quick task in Today, default the date to today.