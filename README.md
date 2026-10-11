# Tzafon (צפון)

**Tzafon** — Hebrew for *north* — is a native Android task and intention manager. The name is the
thesis: the app is a compass that keeps you pointed at a chosen direction, wiring long-term intent
down to today's action. It is built to help one person **plan things, actually do them, hold
habits, and move in a chosen direction** — and to *feel* that progress as identity, not score.

- **Version:** 2.13.0 (built, awaiting the owner's verification; 2.12.0 is the latest promoted) — see [`CHANGELOG.md`](CHANGELOG.md)
- **Platform:** Native Android — Kotlin + Jetpack Compose, Material 3 ("Den" theme)
- **Application ID:** `com.thefoxworks.tzafon` (brand: *The Fox Works*)
- **SDK levels:** compileSdk 37 · targetSdk 36 · minSdk 26

Tzafon v2.0.0 was a re-platform of **TaskManager v1.1.0** (a React/Firestore web app) onto native
Android under a new name. It inherited v1.1.0's behavioural and design spec — including its full
recurrence engine — but **not** its data: it stands up a fresh, local-first store. Since v2.1.0 it
ships one minor version per batch of feature requests; v2.13.0 is the Task Editor Redesign.

## Design philosophy

Tzafon is deliberately **not** gamified. There are **no streaks, points, badges, or adherence
percentages** anywhere in the app. Motivation is framed through three durable needs from
Self-Determination Theory — autonomy, competence, and relatedness — with a guiding rule that the
goal is the user's life improving, *even when that means needing the app less*. Every state is
non-punitive: one miss is normal, and themes are designed so you can't fail them.

## Features

- **Today & Planning** — low-friction capture (a trailing `HH:MM` becomes a cue), if-then cues,
  and a visible daily plan with search.
- **Task editor** — three panes, Task · Timing · Alignment: one shared calendar for the to-do date
  and the deadline, an inline repeat card (weekly, monthly, every N days; a per-occurrence
  deadline N days after each to-do date), and the cue, habit and goals edited in place. For a
  repeating task, Save shows what you changed and asks once where it should land.
- **Status** — five non-punitive statuses (Open, Done, Closed, Frozen, Backlog). A tap on a list
  checkbox marks done or open with an Undo toast; holding it opens the status sheet, which
  explains each status and, for a repeating task, can freeze, thaw, close or reopen the series.
- **Backlog** — a holding area for intentions that aren't yet scheduled.
- **Habits + Cues** — cue-based habits with a forgiving in-period rate and a long arc.
- **Goals + attribution** — goals link to the tasks and habits that advance them.
- **Themes + Directions hub** — the "north" layer: chosen life directions that bridge to action.
- **Review + Focus loops** — weekly/monthly reflection and re-planning.
- **Journey** — an identity mirror that reflects progress back as who you're becoming.
- **Notifications** — cue-driven, local.
- **Five palettes**, week start and other per-device settings; JSON export / import.
- **Google sign-in + sync** — required since v2.7.0; the phone stays fully usable offline.
- **Offline-first**, accessible (TalkBack), and right-to-left aware.

## Architecture

Local-first MVVM. Room is the device's source of truth, behind repository interfaces; dependency
injection is manual via an `AppContainer` on the `Application` (no Hilt). **Firebase is in:** Google
sign-in through Credential Manager (required since v2.7.0 — Welcome is the entry gate) and an
offline-first Firestore **mirror** at `users/{uid}/{collection}/{id}` that starts and stops with the
sign-in. Room is at **schema version 6**; migrations are additive only.

```
android/app/src/main/kotlin/com/thefoxworks/tzafon/
├── domain/   model (entities, repository interfaces, state machine),
│             recurrence (Kotlin engine), editor (draft diff, shared calendar, repeat card,
│             Alignment rules, editor session), status (status sheet, tap rule, undo),
│             dates, action, habits, attribution, themes, review, journey, notify
├── data/     db (Room), repo (Room-backed impls), settings (DataStore), auth, sync
│             (Firestore mirror), transfer (export / import), audio
├── notify/   channel, alarms, boot receiver, WorkManager top-up
└── ui/       MVVM + Compose Navigation — today, planning, habits, directions, journey (the five
              tabs); alltasks, backlog, review, settings, about, capture, welcome, goals;
              editor (TaskPane, TimingPane + TimingCalendar + TimingRepeatCard, AlignmentPane),
              status (status sheet, undo toast); shared components, theme (five palettes), nav
```

**Conventions:** dates are ISO `yyyy-MM-dd` strings; IDs are client UUIDs, except series
occurrences, which use the deterministic id `{seriesId}__{occurrenceDate}` for idempotent
generation.

## Toolchain

- **Gradle project** lives in `android/`.
- Versions are pinned in `android/gradle/libs.versions.toml` (Gradle 9.6.1, AGP 9.2.1, Kotlin
  2.2.10, Compose BOM 2026.02.01, Room 2.8.2, Firebase BOM 34.15.0). AGP 9 has built-in Kotlin — no
  `kotlin.android` plugin; only `org.jetbrains.kotlin.plugin.compose`, plus KSP and `google-services`.
- `android/app/google-services.json` and the release keystore are git-ignored: a fresh checkout
  builds debug and falls back to an unsigned release.
- Build JDK: Android Studio's bundled **JBR 21**.
- Emulator target: AVD `Pixel_8` (arm64, API 37).

## Build & run

Run Gradle from the `android/` directory:

```bash
cd android

# Build the debug APK
./gradlew assembleDebug

# Install and launch on a connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.thefoxworks.tzafon/.MainActivity
```

## Testing

```bash
cd android
./gradlew testDebugUnitTest          # 526 JVM unit tests, 44 suites
./gradlew connectedDebugAndroidTest  # 107 instrumented tests, 19 suites (needs an emulator)
```

Unit tests cover the pure domain: the recurrence engine (an oracle-parity suite ported from the
v1.1.0 web app's `recurrence.test.js`), the state machine and series-scoped status, the editor's
diff, scopes, calendar, repeat card and Alignment rules, the status sheet and undo, habits,
attribution, journey, review, notifications, palettes, bidi and export / import. Instrumented tests
cover behavioural UI (the editor frame and panes, scope sheets, status sheet, checkbox hold,
reorder, header menu, habits, Directions, About, content direction) and **Room migrations**:
each schema version is exported to `android/app/schemas/`, and `MigrationTest` builds the old
version from that JSON and migrates it for real.

## Project documents

| File | What it is |
|---|---|
| [`v2.0.0 Requirements.md`](v2.0.0%20Requirements.md) | The baseline contract — data model (`DM-*`), functional (`FR-*`) and non-functional (`NFR-*`) requirements, out-of-scope. |
| `v2.1.0` … [`v2.13.0 Requirements.md`](v2.13.0%20Requirements.md) | One delta document per version: new codes, refinements of existing ones, acceptance criteria and milestones. For any code, the newest document that touches it wins. |
| [`CHANGELOG.md`](CHANGELOG.md) | What shipped in each version, in user-facing language, citing requirement codes. |
| [`CONTEXT.md`](CONTEXT.md) · [`docs/adr/`](docs/adr/) | The domain glossary, and the architecture decision records. |
| [`PLAN.md`](PLAN.md) | The v2.0.0 build plan — Room schema, architecture, design-system mapping. |
| [`CLAUDE.md`](CLAUDE.md) | Agent and build conventions, toolchain notes, where the repo stands. |
| [`feature-requests/`](feature-requests/) | The owner's feature-request log and the automated Groom → Build → Promote pipeline (`README.md` there). |
| [`Gamification Research.md`](Gamification%20Research.md) · [`Theme System Research.md`](Theme%20System%20Research.md) | Research base behind the design principles. |
| `task-manager/` | The Claude Design handoff (HTML/JSX source + screenshots). |
