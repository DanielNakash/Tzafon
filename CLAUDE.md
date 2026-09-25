# Tzafon — native Android app

Tzafon (צפון, "north") is a native Android task/intention manager by **The Fox Works**:
Kotlin + Jetpack Compose, Material 3, "Den"-themed.

v2.0.0 was a one-shot build of a frozen spec (milestones `M0`→`M9`) — **that phase is over and its
roadmap is history.** Since v2.1.0 the app ships **one minor version per batch of user feature
requests**, through the automated pipeline below. Work the pipeline, not the milestone roadmap.

## Where the repo stands

| | |
|---|---|
| **Latest promoted** | **v2.12.0** — tag `v2.12.0`, `versionName "2.12.0"` / `versionCode 13`, promoted 2026-09-25. This is what `main` and `origin` carry. |
| **Built, awaiting promotion** | none. `main`, the highest tag and the builder's view of the frontier all agree, so the release-serialization guard is clear and the next `/build-next-version` is free to start. |
| **Full history** | `CHANGELOG.md` — every shipped version in user-facing language, each bullet citing its requirement code |

> **The remote is not the whole truth.** A finished build lives only on the machine that built it
> until `/promote` pushes it — the `release/*` branch, the `v*` tag, and the APK in
> `feature-requests/releases/` (git-ignored) are all local. A session in a fresh clone (Claude Code
> on the web, a new checkout) will therefore read a frontier one or more versions behind, and will
> see shipped requests still marked `groomed`. Say which you're looking at.

This table ages. **Re-derive the frontier at the start of a session** rather than trusting it:
`git tag | sort -V | tail -1` and `git ls-remote --tags origin` (a local-only tag is an
unpromoted release), then `grep -n '^status:' feature-requests/FEATURE_REQUESTS.md`
(statuses: `new` → `groomed` → `implemented`, or `rejected`) and `ls feature-requests/releases/`.

## The pipeline — Groom → Build → Promote

The owner's only manual authoring job is appending a plain-language block to
`feature-requests/FEATURE_REQUESTS.md` with `status: new`. Everything downstream is automated:

| Step | Command | Cadence / where | What it does |
|---|---|---|---|
| **Groom** | `/groom-requests` | hourly, launchd, text-only | `status: new` requests → coded requirements in `v<next-minor> Requirements.md`; flips them to `groomed`; commits to `main`. |
| **Build** | `/build-next-version` | daily 03:00, launchd, needs SDK + emulator + keystore | Implements the lowest groomed version milestone-by-milestone on a `release/<target>` branch, runs tests, bumps `versionName`/`versionCode`, prepends the `CHANGELOG.md` entry, builds the signed release APK, marks requests `implemented`, **tags `v<target>` — and stops. It never merges to `main`.** |
| **Promote** | `/promote <version>` | owner, after on-device verification | Fast-forwards `main` to the tag, refreshes the frontier table above, pushes `main` (release + doc commit) and the tag, deletes the `release/*` branch. |

Guardrails worth knowing before you touch any of it:

- **Pause switch** — `pipeline: paused` in `FEATURE_REQUESTS.md` makes both jobs no-op.
- **Release serialization** — the builder refuses to start version N+1 while N's tag is not yet an
  ancestor of `main` (it logs `BUILD blocked unmerged-release`). So an unpromoted release **stalls
  the whole pipeline**: newly groomed work just queues up. A pipeline that has gone quiet usually
  means a built-but-unpromoted version, not a bug — check for a local `release/*` branch first.
- **Account pin** — both jobs run `feature-requests/scripts/assert-account.sh` first and stop if the
  session isn't the pinned Claude account. Never work around it, re-pin, or edit `.pipeline-account`.
- **Merge gate** — `main` only ever moves by the owner's `/promote`.
- **Fail-safe** — a red build leaves requests `groomed` and does not tag.

Full contract: `feature-requests/README.md`. The step-by-step logic lives in `.claude/commands/`
(`groom-requests.md`, `build-next-version.md`, `promote.md`) — read the one you're executing.

## Authoritative sources (read before coding)

- **`v2.0.0 Requirements.md`** — the **baseline contract**: the data model (`DM-*`), the original
  functional surface (`FR-*`), non-functional requirements (`NFR-*`), out-of-scope in §10.
- **`v2.1.0` … `v2.12.0 Requirements.md`** — one **delta doc per version**, auto-groomed from user
  requests. They add new codes and **refine existing ones**; for any code, the *newest* doc that
  touches it wins. Each section cites its source request id and carries acceptance criteria.
  Before changing behaviour in an area, grep that area's code across the docs in version order —
  e.g. `grep -n 'FR-NAV-9' v2.*Requirements.md`.
- **`PLAN.md`** — Room schema, architecture, and the Den → Compose design-system mapping. Still
  authoritative for §2–§4 and §6; **§5's `M0`→`M9` milestone list is historical**, superseded by the
  per-version `## Milestones` lists.
- **`CHANGELOG.md`** — what actually shipped, and when.
- **Design:** `task-manager/project/Tzafon v2.0.html` + its `tz/*.jsx` imports. Read the
  HTML/JSX/CSS **source** for exact dimensions/colors/spacing; do NOT render design files in a
  browser. Cross-reference `task-manager/project/screenshots/`. The design predates several shipped
  versions — where a later requirements doc contradicts it, **the requirements doc wins**.
- **Oracle (reference only):** the v1.1.0 web app at `~/LocalWorkshop/Task Manager/PROJECT/src/`
  (`utils/recurrence.js`, `services/series.js`). The Kotlin engine and its parity tests landed in
  M0 — consult the oracle only when a recurrence question is genuinely unresolved here.

**Code conventions:** `DM-<AREA>-n` data model, `FR-<AREA>-n` functional, `NFR-<AREA>-n`
non-functional, sub-numbered for detail (`FR-HAB-11.3`). Areas in use: `ABOUT, ALL, AUDIO, AUTH,
BACKLOG, CAPTURE, CUE, DATA, DESIGN, DIR, EDITOR, HAB, JOURNEY, LOOP, NAV, NOTIF, PLAN, REC, SET,
TODAY` (FR); `ATTR, CUE, EXPORT, FOCUS, GOAL, HABIT, NOT, PREF, REL, REVIEW, TASK, THEME` (DM);
`A11Y, DATA, OFFLINE, PERF, SEC, TEST` (NFR). Every implementation item and commit cites its codes.

## Locked decisions (do not revisit)

- Native Android; `applicationId com.thefoxworks.tzafon`. **compileSdk 37, targetSdk 36, minSdk 26.**
- **No streaks / points / badges / adherence %** anywhere (`DM-NOT`, `DEC-2/3/4`). The groomer
  *rejects* requests that reintroduce gamification rather than building them.
- **Room is the device's source of truth**, behind repository interfaces. The app is fully usable
  offline (`NFR-OFFLINE-1`); Firestore is a **mirror**, never the store.
- **Firebase is in — no longer deferred.** M9b landed Auth (Google sign-in via Credential Manager)
  plus offline-first Firestore sync (`users/{uid}/{collection}/{id}`), and since **v2.7.0 sign-in is
  required** (`FR-AUTH-1`): Welcome is the entry gate, and sync starts/stops on `authState`.
- **DI stays manual** — one `AppContainer` in `TzafonApp.kt` (no Hilt). It is the single swap-point
  for what backs a repository; nothing else in the app knows.
- **Den is the default palette, not the only one** — five palettes ship (`FR-DESIGN-4`, `DM-PREF-1`).
  Never hardcode a colour: go through `ui/theme` (`Tz`, `DenType`).
- **Per-device preferences are not synced** — palette, week start, planning preset, chime live in
  DataStore and belong to the install, not the account (`FR-DESIGN-4.6`, `FR-AUTH-1.11`).
- Dates are ISO `yyyy-MM-dd` strings. IDs are client UUIDs, except series occurrences, which use the
  deterministic id `{seriesId}__{occurrenceDate}` (idempotent generation, carried from v1.1.0).

## Architecture & layout

`android/app/src/main/kotlin/com/thefoxworks/tzafon/`

```
MainActivity.kt      the NavHost and every route (tabs + alltasks/backlog/review/settings/about/editor)
TzafonApp.kt         Application + AppContainer (manual DI), notification top-up, sync gating
domain/  model/      entities, repository interfaces, StateMachine, AuthRepository
         recurrence/ the Kotlin engine (oracle parity)
         dates/ action/ attribution/ habits/ journey/ notify/ review/ themes/   pure logic, unit-tested
data/    db/         Room: Entities.kt, Daos.kt, TzafonDatabase.kt
         repo/       Room-backed repository impls
         settings/   SettingsStore (DataStore Preferences)
         auth/       FirebaseAuthRepository          sync/  FirestoreSync + codec
         transfer/   export / import (DM-EXPORT-1)   audio/ ChimePlayer
notify/              channel, alarm scheduling, boot receiver, WorkManager top-up
ui/      today/ planning/ habits/ directions/ journey/   ← the five bottom-nav tabs
         alltasks/ backlog/ review/ settings/ about/ capture/ editor/ goals/ welcome/
         components/ theme/ nav/
```

- The five tabs are defined in `ui/nav/BottomNav.kt`; All Tasks, Backlog, Settings and About ride
  the hamburger menu, not the bar (`FR-NAV-1`).
- **Room is at version 5 and migrations are additive only** (`NFR-DATA-1`). A schema change means
  bumping `version` *and* adding a `Migration(n, n+1)` in `TzafonDatabase.kt` — never a destructive
  fallback; shipped installs carry real user data.
- The Firestore mirror writes **through the DAOs on purpose**, bypassing repository business logic —
  a mirror must not re-run attribution or state-machine effects.

## Shell command style (important — reduces permission prompts)

Only these command prefixes run without a permission prompt (see `.claude/settings.json`):
`adb`, `emulator`, `./gradlew` (bare or `JAVA_HOME=… ./gradlew`), `sdkmanager`, `avdmanager`,
`keytool -list`, `cd`, `sleep`, `grep`, `git`, `cp`, `mkdir`, `sips`, and
`zsh feature-requests/scripts/assert-account.sh*`.

- `adb`, `emulator`, and `./gradlew` are already on PATH (`env.PATH` in settings.json). Call them
  as **bare commands** — NEVER prepend `export PATH=...`.
- Do **not** chain steps with `&&`. Emit each command as its **own separate Bash tool call**
  (one `adb shell input tap X Y`, then a separate `adb exec-out screencap -p > FILE`). A chained
  command forces a prompt because glue segments (`export`, `echo`, …) aren't allow-listed.
- Don't wrap commands in `echo … &&` or trailing `&& echo OK`; check the tool's exit code/output
  instead. `export` and `echo` are deliberately NOT allow-listed.
- Need a prefix outside the list above? Expect a prompt — that's intended. In a **headless** run
  (`claude -p`, i.e. the launchd jobs) there is no prompt to answer: an un-allow-listed command is
  **denied**. Allow-list it first, or the scheduled run fails.

## Toolchain (already set up — don't rediscover)

- SDK: `~/Library/Android/sdk`. AVD **`Pixel_8`** (arm64, API 37): `emulator -avd Pixel_8`.
- Build JDK: Android Studio's bundled **JBR 21** (already `JAVA_HOME`; `ANDROID_HOME` also exported).
- Gradle project lives in **`android/`** (space-free). Run Gradle from there: `cd android && ./gradlew …`.
- Versions are pinned in **`android/gradle/libs.versions.toml`** — that file is the single source of
  truth (currently Gradle 9.6.1, AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, Room 2.8.2,
  Firebase BOM 34.15.0). AGP 9 has built-in Kotlin — no `kotlin.android` plugin; only
  `org.jetbrains.kotlin.plugin.compose` (plus KSP and `google-services`).
- The app module is **`android/app/build.gradle.kts`** (Kotlin DSL) — that's where `versionName` /
  `versionCode` are bumped.
- Git-ignored and never committed: `android/app/google-services.json`, `android/keystore.properties`,
  `*.jks`, built APKs, `PIPELINE_LOG.md`. A fresh checkout builds debug fine and falls back to an
  unsigned release.
- **Sessions without the toolchain** (e.g. Claude Code on the web) have no SDK, emulator or
  keystore — the build/verify loop below simply doesn't apply. Grooming, requirements, docs and
  code reading are fine there; don't fake a verification you couldn't run.

## Build / verify loop (run after every meaningful change; each line = separate Bash call)

1. `cd android && ./gradlew assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb shell am start -n com.thefoxworks.tzafon/.MainActivity`
4. `adb exec-out screencap -p > /tmp/tz.png` — then **downscale before viewing**:
   `sips -Z 1600 /tmp/tz.png` (separate Bash call; macOS-only). The Pixel_8 renders at 1080×2400;
   the raw 2400px height trips the API's many-image 2000px-per-edge cap and silently
   kills a headless run (this is what crashed the 2026-07-13 build). `sips -Z 1600`
   shrinks the long edge in place, faithfully — real Pixel-8 layout, just fewer pixels.
   Then view `/tmp/tz.png` and compare vs design/screenshots.
5. `adb shell input tap X Y` to exercise interactions, then re-screenshot (step 4, downscale again).
6. `adb logcat -d | grep -iE 'AndroidRuntime|FATAL'` → catch crashes

**Screenshot soft cap (per milestone).** The many-image 2000px cap only *activates* past
~20 images in the conversation, so also stay lean: aim for **≤ 8 screenshots per milestone**.
Downscaling (step 4) removes the per-image edge; the cap keeps you clear of the count edge
too. Prefer text-based verification where it's authoritative — `gradlew testDebugUnitTest`,
`adb logcat`, and (for nav/back-stack questions) a temporary `Log.d` read back via
`adb logcat -d -s <TAG>` — over another screenshot. Screenshot to confirm visual/layout
outcomes against the design; don't screenshot what a log line already proves.

## Testing (`NFR-TEST-1`)

- **Unit** — `cd android && ./gradlew testDebugUnitTest`. 22 suites in
  `app/src/test/kotlin/…`, covering the recurrence oracle-parity suite (`RecurrenceTest`,
  `RecurringDateShiftTest`), the state machine, action logic, habit math, attribution, journey,
  review, notify, palettes, bidi, nav back-stack rules, and data export/import.
- **Instrumented** — `./gradlew connectedDebugAndroidTest` (needs a running emulator).
  the 8 suites in `app/src/androidTest/kotlin/…` cover behavioural UI: header menu, habit collapse/edit,
  content direction, reorder, Directions creation, About.
- A version is done only when its tests are green **and** it's verified on the emulator against the
  design. New behaviour lands with a test — regressions here are what the parity suites exist for.

## Working a version (interactive)

Same loop the builder runs, minus the branch/tag ceremony you don't want by hand:

1. Read `v<target> Requirements.md` end to end — scope (§1), the requirement sections, acceptance
   criteria (§5), and the `## Milestones` list.
2. Implement **milestone by milestone, in order, without approval gates**, per-change: implement →
   tests → `assembleDebug` → install → launch → screenshot vs design → logcat check.
3. **Commit per milestone**, referencing the milestone and its codes — the house style is
   `feat(v2.11.0 M2): FR-EDITOR-1 serves-before-cue field order`, and `fix(…)` / `docs(…)` /
   `chore(pipeline): …` as appropriate.
4. Only stop for a genuine blocker or an unresolvable design-vs-spec contradiction.

Never merge to `main` yourself, never tag a version you didn't build end to end, and never edit
another request's `status:`/`implemented:`/`requirements:` fields — those are the pipeline's ledger.
