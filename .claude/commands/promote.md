---
description: Promote a verified release/* build to main — fast-forward main, refresh CLAUDE.md to the new frontier, push main + tag, delete the release branch.
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
     but the tag does and is already an ancestor of `main`, this version is already promoted — say
     so and stop, except that you should still run step 4 (the doc may have been left stale by an
     earlier promote) and push if it changes anything.
   - The tag points at the release branch HEAD (they should be the same commit).
   - The merge is a clean fast-forward: `git merge-base --is-ancestor main <tag>` must be true.
     If it is NOT (main has diverged), stop and report — do **not** create a merge commit or force
     anything; surface it for the owner to resolve.

2. **Advance `main` (fast-forward only), without disturbing the working tree.**
   The owner's tree often has uncommitted inbox edits (`FEATURE_REQUESTS.md`) — leave them alone.
   - If currently on `main`: `git merge --ff-only <tag>`.
   - Otherwise, advance the pointer without checkout: `git branch -f main <tag>` (safe — it's a
     fast-forward and `main` is not checked out).

3. **Get onto `main`** — the doc commit in step 4 must land there, not on the release branch.
   If you are currently on `release/<target>`, `git checkout main` (safe — after the fast-forward
   the two branches are the same commit, so any uncommitted inbox edits carry over cleanly).

4. **Refresh `CLAUDE.md` to the new frontier — before any push.**
   `CLAUDE.md` is loaded into every future session's context, so a stale frontier actively
   misleads the next session (and every fresh clone). Promoting is exactly the moment it changes.
   - Read the **"Where the repo stands"** section. Update whatever states the frontier so that it
     reads as of *after* this promotion:
     - the **latest promoted** version becomes `v<target>`, with its tag, its `versionName` /
       `versionCode`, and the promotion date (today);
     - **nothing is awaiting promotion** — say so plainly, and that the release-serialization
       guard is therefore clear and the next `/build-next-version` is free to start.
   - **Read `versionName` / `versionCode` out of `android/app/build.gradle.kts`.** Never infer
     them from the version number — the builder bumps `versionCode` independently.
   - Then scan the rest of the file for any other claim this promotion just falsified (a
     "`/promote <target>` is the missing step" note, a local-only-release caveat naming this
     version, a frontier cited in passing). Leave the *generic* guidance about unpromoted
     releases alone — it stays true for the next one.
   - Don't rewrite the section's shape or prose beyond what the promotion changed; if the file has
     no such section at all, say so in your report and skip the commit rather than inventing one.
   - Commit **only** this file:
     - `git add CLAUDE.md` (never `git commit -a` — it would sweep up the inbox edits)
     - `git commit -m "docs(CLAUDE.md): v<target> is promoted — refresh the frontier table"`
   - If the doc already reads correctly, make **no** commit and note that in the report.

5. **Push `main` and the tag** (separate Bash calls). `main` now carries the release commits **and**
   the `CLAUDE.md` refresh, so one push ships both:
   - `git push origin main`
   - `git push origin <tag>`

   Note `main` is now one commit ahead of `<tag>`; that is expected and correct — the tag marks the
   build, the doc commit describes the promotion of it.

6. **Delete the release branch.**
   - `git branch -d release/<target>` (use `-d`, the merged-only delete; if git refuses, the merge
     didn't actually happen — stop and investigate rather than forcing with `-D`).
   - If the branch exists on origin, delete it too: `git push origin --delete release/<target>`.

7. **Leave inbox/working-tree changes untouched** — new `FEATURE_REQUESTS.md` entries are the
   groomer's input; never commit or discard them here. `CLAUDE.md` in step 4 is the *only* file
   this command may commit.

8. **Report.** Print a short summary: version promoted, the `main` push range (`<old>..<new>`),
   what changed in `CLAUDE.md` (or that it was already current), whether the tag was newly pushed,
   the release branch(es) deleted (local/remote), and a note that promoting this version unblocks
   the builder's next run (the release-serialization guard now sees the highest tag as an ancestor
   of `main`).

Stop for any pre-check failure or a non-fast-forward situation — those are the owner's to resolve.
