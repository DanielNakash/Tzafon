---
description: Promote a verified release/* build to main — fast-forward main, push, push the tag, delete the release branch.
---

You are performing the **promote** step in Tzafon's feature pipeline — the owner's merge-gate step
that completes the verb-trio **Groom** (`/groom-requests`) → **Build** (`/build-next-version`) →
**Promote** (this). Run from the repo root (`/Users/danielnakash/LocalWorkshop/Tzafon`). Follow the
shell-command style in `CLAUDE.md` (one command per Bash call, no `&&`).

**Scope:** "Promote" means *advance `main` on GitHub to the verified release and clean up* — it is
NOT a Play Store or user-facing distribution.

**Precondition (the gate):** the owner has already **verified `vX.Y.Z` on-device**. This command
trusts that verification happened — the builder deliberately stops before merging because its
automated verify is shallow, so the human gate is what catches UX bugs. Do not re-run the build.

## Argument

`$ARGUMENTS` is the version to promote, e.g. `v2.4.0` or `2.4.0`. Normalize it: let `<target>` be
the bare version (`2.4.0`) and `<tag>` be `v<target>`. If no argument is given, infer `<target>`
from the highest `v*` tag that is **not** yet an ancestor of `main`, and confirm it in your report.

## Steps

1. **Pre-checks (fail loudly, change nothing if any fails).**
   - The tag `<tag>` exists: `git rev-parse <tag>`.
   - A `release/<target>` branch exists locally and/or on origin (`git rev-parse --verify
     release/<target>` and/or `git ls-remote --heads origin release/<target>`). If neither exists
     but the tag does and is already an ancestor of `main`, this version is already promoted — report
     that and stop.
   - The tag points at the release branch HEAD (they should be the same commit).
   - The merge is a clean fast-forward: `git merge-base --is-ancestor main <tag>` must be true.
     If it is NOT (main has diverged), stop and report — do **not** create a merge commit or force
     anything; surface it for the owner to resolve.

2. **Advance `main` (fast-forward only), without disturbing the working tree.**
   The owner's tree often has uncommitted inbox edits (`FEATURE_REQUESTS.md`) — leave them alone.
   - If currently on `main`: `git merge --ff-only <tag>`.
   - Otherwise, advance the pointer without checkout: `git branch -f main <tag>` (safe — it's a
     fast-forward and `main` is not checked out).

3. **Push `main` and the tag** (separate Bash calls):
   - `git push origin main`
   - `git push origin <tag>`

4. **Delete the release branch.**
   - If you are currently on `release/<target>`, switch off it first: `git checkout main` (now safe
     — after the fast-forward `main` and the release branch are the same commit, so any inbox edits
     carry over cleanly).
   - `git branch -d release/<target>` (use `-d`, the merged-only delete; if git refuses, the merge
     didn't actually happen — stop and investigate rather than forcing with `-D`).
   - If the branch exists on origin, delete it too: `git push origin --delete release/<target>`.

5. **Leave inbox/working-tree changes untouched** — new `FEATURE_REQUESTS.md` entries are the
   groomer's input; never commit or discard them here.

6. **Report.** Print a short summary: version promoted, the `main` push range (`<old>..<new>`),
   whether the tag was newly pushed, the release branch(es) deleted (local/remote), and a note that
   promoting this version unblocks the builder's next run (the release-serialization guard now sees
   the highest tag as an ancestor of `main`).

Stop for any pre-check failure or a non-fast-forward situation — those are the owner's to resolve.
