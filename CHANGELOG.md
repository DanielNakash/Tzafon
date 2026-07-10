# Changelog

All notable changes to **Tzafon** (צפון — *north*), the native Android task &
intention manager by The Fox Works. This log starts at the v2.0.0 native rebuild.

The format follows [Keep a Changelog](https://keepachangelog.com/); versions use
[Semantic Versioning](https://semver.org/). Requirement codes (`FR-*`, `DM-*`) refer
to the versioned requirements documents in the repo.

---

## [2.2.0] — 2026-07-10

### Added
- **Edit a habit** (`FR-HAB-7`) — each habit card gains a pencil affordance that opens
  the editor in edit mode, pre-filled with the habit's name, kind, target and cue, with
  **Save** and **Delete this habit**.
- **Add a Goal from Directions** (`FR-DIR-7`) — the Directions hub's **Create** button now
  opens a "What are you adding?" chooser (**Add a goal** / **Add a theme**); the goal form
  carries the full goal detail (finish line, kind, steps, theme, commitment). Theme creation
  is preserved.
- **Global menu (hamburger)** (`FR-NAV-3`) — a traditional ☰ menu replaces the fox-logo
  menu trigger, present at a consistent position on **all five tabs** (Today, Planning,
  Habits, Directions, Journey) and opening the "Around the den" menu (All Tasks, Backlog,
  Settings). The fox mark remains as branding on identity surfaces.

### Fixed
- The full task editor opened from **Today** (via *Expand* or **+**) now defaults its
  **To Do Date to today**, matching the inline quick-add (extends `FR-TODAY-7`). Other
  capture surfaces stay undated (`FR-CAPTURE-2`).
- The hamburger sat at different heights on Today vs Planning and was missing from Habits,
  Directions and Journey — it is now anchored consistently and present on every tab.

## [2.1.0] — 2026-07-10

### Added
- **Today quick-add defaults to today** (`FR-TODAY-7`) — a task captured from Today lands
  dated for today so it appears in the Today list immediately.
- **Collapsed habit cards** (`FR-HAB-5`) — the Habits view now shows compact cards by
  default (name + log + expand), so several habits stay scannable at a glance.
- **Right-to-left text support** (`FR-DESIGN-3`) — user-authored text (task and habit
  titles, etc.) follows its own content direction, so Hebrew and other RTL text reads and
  aligns correctly.

### Fixed
- **Create-habit confirm button** (`FR-HAB-6`) — the confirmation button was unreadable /
  invisible once a name was entered. Root cause was a graphics-layer alpha dropping the
  button fill (not the color tokens); opacity is now baked into the colors so the enabled
  button always paints, meeting WCAG AA contrast.
- **Today RTL alignment** — Hebrew task titles in the Today list now right-align (matching
  the Habits list) instead of hugging the left.

## [2.0.0] — 2026-07-07

Full native Android rebuild — Kotlin + Jetpack Compose, Material 3, the "Den" theme —
carrying forward the behavioural and design spec of the TaskManager v1.1.0 web app under
the new name **Tzafon**. Local-first persistence (Room) behind repository interfaces.

### Added
- **Foundations & task model** — native app scaffold with v1.1.0 recurrence parity, the
  task state machine, and the action layer: Today, Planning, All Tasks, and quick capture
  (`M0`–`M2`).
- **Backlog** — a someday/maybe space, parked and unscheduled (`M3`, `FR-BACKLOG`).
- **Habits & Cues** — forgiving weekly-rate habits anchored to cues; no streaks, points,
  badges or adherence % by design (`M4`, `DM-HABIT`, `DM-CUE`).
- **Goals & attribution** — goals (stepped / amount / directional) that tasks and habits
  can serve (`M5`, `DM-GOAL`, `DM-ATTR`).
- **Themes & the Directions hub** — seasonal directions with a why; the compass thesis made
  literal (`M6`, `DM-THEME`, `FR-DIR`).
- **Review & Focus loops** — weekly review and focus flows, plus settings (`M7`).
- **Journey** — a reflective mirror of milestones, arcs and directions over time (`M8`,
  `FR-JOURNEY`).
- **Release readiness** — notifications, offline support, accessibility, and a signed
  release build (`M9`).
- **Cloud sync (behind the seam)** — Firebase Auth (Google sign-in) and offline-first
  Firestore sync, kept behind the repository interfaces (`M9b`).

[2.2.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.2.0
[2.1.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.1.0
[2.0.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.0.0
