# Tzafon (צפון)

**Tzafon** — Hebrew for *north* — is a native Android task and intention manager. The name is the
thesis: the app is a compass that keeps you pointed at a chosen direction, wiring long-term intent
down to today's action. It is built to help one person **plan things, actually do them, hold
habits, and move in a chosen direction** — and to *feel* that progress as identity, not score.

- **Version:** 2.0.0
- **Platform:** Native Android — Kotlin + Jetpack Compose, Material 3 ("Den" theme)
- **Application ID:** `com.thefoxworks.tzafon` (brand: *The Fox Works*)
- **SDK levels:** compileSdk 37 · targetSdk 36 · minSdk 26

Tzafon v2.0.0 is a re-platform of **TaskManager v1.1.0** (a React/Firestore web app) onto native
Android under a new name. It inherits v1.1.0's behavioural and design spec — including its full
recurrence engine — but **not** its data: it stands up a fresh, local-first store with a clean seam
for a future Firebase project.

## Design philosophy

Tzafon is deliberately **not** gamified. There are **no streaks, points, badges, or adherence
percentages** anywhere in the app. Motivation is framed through three durable needs from
Self-Determination Theory — autonomy, competence, and relatedness — with a guiding rule that the
goal is the user's life improving, *even when that means needing the app less*. Every state is
non-punitive: one miss is normal, and themes are designed so you can't fail them.

## Features

- **Today & Planning** — low-friction capture, if-then cues, and a visible daily plan.
- **Task state machine** — tasks move through explicit, non-punitive states; recurrence-aware.
- **Backlog** — a holding area for intentions that aren't yet scheduled.
- **Habits + Cues** — cue-based habits with a forgiving in-period rate and a long arc.
- **Goals + attribution** — goals link to the tasks and habits that advance them.
- **Themes + Directions hub** — the "north" layer: chosen life directions that bridge to action.
- **Review + Focus loops** — weekly/monthly reflection and re-planning.
- **Journey** — an identity mirror that reflects progress back as who you're becoming.
- **Notifications** — cue-driven, local.
- **Offline-first** and accessible.

## Architecture

Local-first MVVM. Persistence is Room behind repository interfaces; dependency injection is manual
via an `AppContainer` on the `Application` (no Hilt). Firebase (Auth + Firestore sync) is
**deferred** to a later milestone — the repository seam is kept clean so it can be added without
touching the UI or domain layers.

```
android/app/src/main/kotlin/com/thefoxworks/tzafon/
├── domain/   model (entities, repository interfaces, state machine),
│             recurrence (Kotlin engine), dates, action, habits, goals/attribution,
│             themes, review, journey, notify
├── data/     db (Room), repo (Room-backed impls), settings
└── ui/       MVVM + Compose Navigation — today, planning, alltasks, capture, editor,
              welcome, backlog, habits, goals, directions, review, journey, settings;
              shared components, theme (Den Material 3), nav
```

**Conventions:** dates are ISO `yyyy-MM-dd` strings; IDs are client UUIDs, except series
occurrences, which use the deterministic id `{seriesId}__{occurrenceDate}` for idempotent
generation.

## Toolchain

- **Gradle project** lives in `android/`.
- Locked versions: **Gradle 9.6.1, AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01.** AGP 9 has
  built-in Kotlin — no `kotlin.android` plugin; only `org.jetbrains.kotlin.plugin.compose`.
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
./gradlew testDebugUnitTest
```

JUnit domain tests cover the recurrence engine (an oracle-parity suite ported from the v1.1.0 web
app's `recurrence.test.js`) and state-machine transitions. Compose tests cover behavioural UI.

## Project documents

| File | What it is |
|---|---|
| [`v2.0.0 Requirements.md`](v2.0.0%20Requirements.md) | The contract — requirements (`DM-*`, `FR-*`), milestones `M0`–`M9`, acceptance criteria, out-of-scope. |
| [`PLAN.md`](PLAN.md) | The build plan — Room schema, architecture, design-system mapping, milestone definitions of done. |
| [`CLAUDE.md`](CLAUDE.md) | Build conventions and toolchain notes. |
| [`Gamification Research.md`](Gamification%20Research.md) · [`Theme System Research.md`](Theme%20System%20Research.md) | Research base behind the design principles. |
| `task-manager/` | The Claude Design handoff (HTML/JSX source + screenshots). |
