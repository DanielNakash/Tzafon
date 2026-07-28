---
description: Turn new feature requests into coded requirements in the next-version requirements doc.
---

You are the **groomer** in Tzafon's feature pipeline. Text-only job — do NOT build or run gradle.
Run from the repo root (`/Users/danielnakash/LocalWorkshop/Tzafon`).

## Steps

0. **Account gate — do this first, before reading anything else.** Run
   `zsh feature-requests/scripts/assert-account.sh GROOM` from the repo root. If it exits
   non-zero, **stop immediately** and report the reason it printed. The pipeline is pinned to one
   Claude account; a non-zero exit means this session is not it. Do **not** re-pin, edit
   `.pipeline-account`, edit `.env`, or work around the gate — only the owner does that, by running
   `pin-account.sh` deliberately. (The launchd wrapper runs this same gate, so a scheduled run
   normally never reaches this step; it matters when someone types `/groom-requests` by hand.)

1. **Honor the switch.** Read `feature-requests/FEATURE_REQUESTS.md`. If it contains
   `pipeline: paused`, append a `GROOM skipped paused` line to `feature-requests/PIPELINE_LOG.md`
   and stop.

2. **Collect work.** Find every request block with `status: new`. **Ignore the template/example
   block in the file header** — it lives inside a ``` code fence and is documentation, not a real
   request. Only real request blocks (outside any code fence, below the "Add requests below this
   line" marker) count. If there are no real new requests, log `GROOM noop no-new-requests`, do not
   commit, and stop.

3. **Compute the target version.** The current released version is the highest `git tag`
   (e.g. `v2.0.0`). The next target is the next **minor** bump (`2.0.0` → `2.1.0`). All
   currently-groomed-but-unshipped requests share this one target version until it ships.
   The requirements file is `v<target> Requirements.md` (e.g. `v2.1.0 Requirements.md`).

4. **Absorb the philosophy before writing.** Read `CLAUDE.md`, `PLAN.md`, and the most recent
   `v*.* Requirements.md`. Internalize the app's identity and hard constraints — especially:
   - **No streaks / points / badges / adherence %** (`DM-NOT`, `DEC-2/3/4`). Reject or reframe any
     request that reintroduces gamification; note the reframing in the requirement text.
   - The "compass / chosen direction" thesis, the Den Material 3 theming, local-first-with-clean-seam
     persistence, the state-machine and recurrence model.
   Requirements must read as if written by the same author as the v2.0.0 doc.

5. **Author requirements.** For each new request, write a detailed requirements section using the
   existing code conventions (`DM-*` for data-model changes, `FR-<AREA>-n` for functional
   requirements, acceptance criteria, and an out-of-scope note where relevant). Cross-reference the
   request id. If a request is ambiguous or conflicts with the spec, still write the requirement but
   add a `> [DECISION]` callout stating the assumption you made (matching the doc's callout style).

   **Rejection path.** If a request fundamentally violates a locked decision and cannot be reframed
   (e.g. it demands streaks/points/badges/adherence — `DM-NOT`), do **not** invent a requirement.
   Instead set that request's `status: rejected` and add a `reason:` line to its block explaining
   why, citing the code. Rejected requests are never built. Mention them in the log/commit.

6. **Write the target doc.** If `v<target> Requirements.md` does not exist, create it by mirroring
   the structure/front-matter of the current requirements doc (title, version = `<target> (target)`,
   status `Requirements — auto-groomed`, date = today `2026-07-08` style). Add each new
   requirement section, and maintain a `## Milestones` list so the builder has an ordered plan.

   **Idempotent re-groom.** A request set back to `status: new` after it was previously groomed
   (the user edited it) may already have a section in the target doc. Before appending, search the
   doc for that request id — if a section for it already exists, **replace it in place** rather than
   appending a duplicate, and update the corresponding `## Milestones` entry. One request id must
   never produce two requirement sections.

7. **Update the inbox.** For each groomed request, set `status: groomed` and `target: <target>`
   in `feature-requests/FEATURE_REQUESTS.md`. Do not touch `implemented:`.

8. **Log + commit.** Append one `GROOM groomed <ids> → v<target>` line to `PIPELINE_LOG.md` (note
   any rejected ids). Prefix the line with a **local-time** timestamp from
   `date +%Y-%m-%dT%H:%M:%S%z` (not UTC), matching the shell-written lines.
   `PIPELINE_LOG.md` is git-ignored — do not stage it. Stage **only the two
   tracked pipeline files** — never `git add -A`, so unrelated working-tree changes are never swept
   into the commit:
   `git add "v<target> Requirements.md" feature-requests/FEATURE_REQUESTS.md`
   then `git commit -m "chore(groom): <ids> → v<target> requirements"`. The commit message is the
   permanent audit record, so list every request id it grooms/rejects.
   Stay on `main` — grooming is text-only and safe to commit directly.
   End with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.

Keep the run tight. Do not implement code. Do not modify any prior released requirements doc.
