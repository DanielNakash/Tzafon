# Tzafon v2.0.0 — native Android build

Tzafon (צפון, "north") is a native Android task/intention manager: Kotlin + Jetpack Compose,
Material 3, "Den"-themed. This is the full v2.0.0 build from a **final, reviewed** spec — proceed
autonomously, milestone by milestone, without approval gates.

## Authoritative sources (read before coding)
- **`v2.0.0 Requirements.md`** — the contract. Final. Requirement codes: `DM-*` (data model),
  `FR-*` (functional), `M0`–`M9` (milestones), acceptance criteria in §9, out-of-scope in §10.
- **`PLAN.md`** — the build plan: Room schema, architecture, design-system mapping, and the
  M0→M9 definitions of done. Every implementation item cites its requirement code.
- **`task-manager/README.md`** — the Claude Design handoff instructions.
- **`task-manager/project/Tzafon v2.0.html`** + its `tz/*.jsx` imports — the primary design. Read
  the HTML/JSX/CSS **source** for exact dimensions/colors/spacing; do NOT render design files in a
  browser. Cross-reference `task-manager/project/screenshots/`.
- **Oracle (reference impl):** the v1.1.0 web app at `~/LocalWorkshop/Task Manager/PROJECT/src/`
  (`utils/recurrence.js` + `recurrence.test.js`, `services/series.js`). The Kotlin recurrence
  engine must mirror its behavior; JUnit parity tests port `recurrence.test.js`.

## Shell command style (important — reduces permission prompts)
Only these command prefixes run without a permission prompt (see `.claude/settings.json`):
`adb`, `emulator`, `./gradlew`, `sdkmanager`, `avdmanager`, `keytool -list`, `cd`, `sleep`, `grep`.

- `adb`, `emulator`, and `./gradlew` are already on PATH (`env.PATH` in settings.json). Call them
  as **bare commands** — NEVER prepend `export PATH=...`.
- Do **not** chain steps with `&&`. Emit each command as its **own separate Bash tool call**
  (one `adb shell input tap X Y`, then a separate `adb exec-out screencap -p > FILE`). A chained
  command forces a prompt because glue segments (`export`, `echo`, …) aren't allow-listed.
- Don't wrap commands in `echo … &&` or trailing `&& echo OK`; check the tool's exit code/output
  instead. `export` and `echo` are deliberately NOT allow-listed.
- Need a prefix outside the list above? Expect a prompt — that's intended.

## Toolchain (already set up — don't rediscover)
- SDK: `~/Library/Android/sdk`. AVD **`Pixel_8`** (arm64, API 37): `emulator -avd Pixel_8`.
- Build JDK: Android Studio's bundled **JBR 21** (already `JAVA_HOME`; `ANDROID_HOME` also exported).
- Locked versions: **Gradle 9.6.1, AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01.**
  AGP 9 has built-in Kotlin — no `kotlin.android` plugin; only `org.jetbrains.kotlin.plugin.compose`.
- Gradle project lives in **`android/`** (space-free). Run Gradle from there: `cd android && ./gradlew …`.

## Locked decisions (do not revisit)
- Native Android; `applicationId com.thefoxworks.tzafon` (brand "The Fox Works").
- SDK levels: **compileSdk 37, targetSdk 36, minSdk 26.**
- **No streaks / points / badges / adherence %** anywhere (`DM-NOT`, `DEC-2/3/4`).
- **Local-first persistence (Room)** behind repository interfaces. **Firebase (Auth + Firestore
  sync) is deferred** to M9b — keep the repository seam clean so it can be added later; do NOT wire
  Firebase before then.
- **DI: manual** — an `AppContainer` on the `Application` (no Hilt; single module, ~8 repositories).

## Architecture & layout (`android/app/src/main/kotlin/com/thefoxworks/tzafon/`)
- `domain/` — `model/` (entities, repository interfaces, state machine), `recurrence/` (Kotlin
  engine mirroring the oracle), `dates/`, `action/`.
- `data/` — `db/` (Room), `repo/` (Room-backed repository impls), `settings/`.
- `ui/` — MVVM + Compose Navigation. Screens: `today/`, `alltasks/`, `capture/`, `editor/`,
  `welcome/`; shared `components/`, `theme/` (Den Material 3 theme), `nav/`.
- Dates are ISO `yyyy-MM-dd` strings. IDs are client UUIDs, except series occurrences which use the
  deterministic id `{seriesId}__{occurrenceDate}` (idempotent generation, carried from v1.1.0).

## Build / verify loop (run after every meaningful change; each line = separate Bash call)
1. `cd android && ./gradlew assembleDebug`
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb shell am start -n com.thefoxworks.tzafon/.MainActivity`
4. `adb exec-out screencap -p > /tmp/tz.png` → view, compare vs design/screenshots
5. `adb shell input tap X Y` to exercise interactions, then re-screenshot
6. `adb logcat -d | grep -iE 'AndroidRuntime|FATAL'` → catch crashes

## Testing (`NFR-TEST-1`)
- JUnit domain tests: recurrence oracle-parity suite (port `recurrence.test.js`) + state-machine
  transition tests; run via `cd android && ./gradlew testDebugUnitTest`.
- Compose tests where UI is behavioral. Each milestone is done only when its tests are green AND
  verified on the emulator against the design.

## Workflow
Implement **milestone by milestone, M0 → M9, in order, without approval gates**. Per milestone:
implement → tests → `assembleDebug` → install → launch → screenshot vs design → logcat check →
short progress note (what changed, against which codes) → **git commit** referencing the milestone
+ codes. Only stop for a genuine blocker or an unresolvable design-vs-spec contradiction.

## Status
- **M0** — native foundation + v1.1.0 parity — ✅ committed (`b46ea9b`).
- **M1** — task state machine (`DM-TASK-1/2/3`, `FR-REC-5`, `FR-ALL-3`) — ✅ committed (`3b6394a`).
- **M2** — action layer (`FR-NAV-1/2`, `FR-TODAY-1..6`, `FR-PLAN-1/2/3`, `FR-ALL-2/4/5`,
  `FR-CAPTURE-1/2`, `FR-REC-3`) — ✅ committed.
- **M3** — Backlog state (`FR-BACKLOG-1..4`) — ✅ committed.
- **M4** — Habits + Cues (`DM-HABIT`, `DM-CUE`, `FR-HAB`) — ✅ committed.
- **M5** — Goals + attribution (`DM-GOAL`, `DM-ATTR`) — ✅ committed.
- **M6** — Themes + Directions hub (`DM-THEME`, `FR-DIR`) — ✅ committed.
- **M7** — Review + Focus loops (`DM-REVIEW`, `DM-FOCUS`, `FR-LOOP`, `FR-SET`) — next.
Check `git log` and `PLAN.md §5` for the current frontier before continuing.
