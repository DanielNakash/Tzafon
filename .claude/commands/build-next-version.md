---
description: Implement the next groomed version end-to-end, build the APK, and mark requests shipped.
---

You are the **builder** in Tzafon's feature pipeline. This job runs **locally** (needs the Android
SDK, the `Pixel_8` emulator, and the owner keystore). Run from the repo root
(`/Users/danielnakash/LocalWorkshop/Tzafon`). Follow the build/verify loop and shell-command style
in `CLAUDE.md` exactly (bare `adb`/`emulator`/`./gradlew`, one command per Bash call, no `&&`).

## Autonomy boundary (decided)
Work on a `release/*` branch and **tag** the version. **Do NOT merge to `main`** — the owner
reviews and merges. Never mark requests implemented unless the tag was created.

## Steps

1. **Honor the switch.** Read `feature-requests/FEATURE_REQUESTS.md`. If `pipeline: paused`, log
   `BUILD skipped paused` to `PIPELINE_LOG.md` and stop.

2. **Pick the version.** Find the lowest version that has `status: groomed` requests and no matching
   `git tag`. Call it `<target>` (e.g. `2.1.0`), with requirements in `v<target> Requirements.md`.
   If there is nothing groomed to build, log `BUILD noop nothing-groomed` and stop.

3. **Guard against a dirty tree.** `git status --short` must be clean and you must be on `main`
   (up to date). If not, log `BUILD aborted dirty-tree` and stop — do not risk unrelated changes.

3b. **Release serialization guard.** Confirm the previous release is merged before starting a new
   one: the highest existing `v*` tag must be an ancestor of `main`
   (`git merge-base --is-ancestor <highest-tag> main`). If it is NOT, a prior release branch is
   still awaiting the owner's merge — building on top of `main` would branch from stale code and
   lose that release's work. Log `BUILD blocked unmerged-release <tag>` and stop. (The wrapper
   script also checks this, but verify it yourself.)

4. **Branch.** `git checkout -b release/<target>` (or check it out if it already exists and resume).

5. **Implement milestone-by-milestone** per the `## Milestones` list in `v<target> Requirements.md`,
   using the standard per-change loop from `CLAUDE.md`:
   - `cd android && ./gradlew assembleDebug`
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
   - `adb shell am start -n com.thefoxworks.tzafon/.MainActivity`
   - `adb exec-out screencap -p > /tmp/tz.png` → view, compare to design
   - exercise interactions; `adb logcat -d | grep -iE 'AndroidRuntime|FATAL'` for crashes.
   (Emulator boot/shutdown is handled by the wrapper script; if no device is attached, start
   `emulator -avd Pixel_8` yourself and wait for boot.)

6. **Tests must be green.** `cd android && ./gradlew testDebugUnitTest`. If red, fix before shipping.
   On an unrecoverable failure: commit WIP to the branch, log `BUILD failed <target> <reason>`,
   leave the requests as `groomed`, and stop **without tagging**.

7. **Bump version.** In `android/app/build.gradle`, set `versionName = "<target>"` and increment
   `versionCode` by 1.

8. **Release build.** `cd android && ./gradlew assembleRelease` (signs with the owner keystore).
   Verify the APK exists and is signed. Copy it to `feature-requests/releases/Tzafon-<target>.apk`.

9. **Mark shipped.** For each request built into this version, set `status: implemented` and
   `implemented: <target> / <short-sha>` in `FEATURE_REQUESTS.md`.

10. **Commit + tag.** Stage the implementation plus the tracked pipeline files (code changes are
    expected on this branch, so `git add -A` is acceptable here — you are on an isolated `release/*`
    branch; `PIPELINE_LOG.md` is git-ignored and won't be staged). Commit `release: v<target>
    (<ids>)`; then `git tag v<target>`. Append `BUILD shipped v<target> <ids> <sha>` to
    `PIPELINE_LOG.md` (local ledger only — not committed). Stay on the `release/<target>` branch —
    do not merge. End commit messages with
    `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.

11. **Report.** Print a short summary: version, requests shipped, test result, APK path, tag,
    and the branch awaiting the owner's merge.

Stop only for a genuine blocker or a design-vs-spec contradiction, per `CLAUDE.md`.
