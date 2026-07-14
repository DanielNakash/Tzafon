# Changelog

All notable changes to **Tzafon** (צפון — *north*), the native Android task &
intention manager by The Fox Works. This log starts at the v2.0.0 native rebuild.

The format follows [Keep a Changelog](https://keepachangelog.com/); versions use
[Semantic Versioning](https://semver.org/). Requirement codes (`FR-*`, `DM-*`) refer
to the versioned requirements documents in the repo.

---

## [2.6.0] — 2026-07-14

### Added
- **Choose your colour palette** (`FR-DESIGN-4`, `FR-SET-4`, `DM-PREF-1`) — Settings gains an
  **Appearance → Palette** row with five hand-tuned palettes: **Den** (the Fox Works original and
  default), **Blue**, **Green**, **Magenta**, **Teal**. Pick one and the whole app re-colours
  instantly — no restart. Each palette is a *translated* Den, not a random hue swap: warm-paper
  ancestry in the text tones, a distinct completion-green, a distinct overdue colour, and three
  still-separable theme accents in Directions. The choice is saved on the device (not synced) and
  survives a restart. Contrast on every text pair meets WCAG AA.
- **Explicit un-log on every habit surface** (`FR-HAB-9`) — a logged day can now be cleared from
  anywhere it was logged. The today pill re-labels **“Mark today done” ⇄ “Un-mark today”** on both
  the collapsed and expanded habit rows (frequency and quantitative). The amount sheet, reopened on
  an already-logged date, shows a **“Clear this log”** row. The *Log a date…* calendar marks logged
  days and announces **“Tap to un-log …”** on them. No confirmation dialog, no streak-guilt copy —
  a mis-log is one tap to undo.

### Changed
- **Overdue no longer nags about a recurring task whose next turn is already today** (`FR-PLAN-5`) —
  when a repeating task slipped but the series’ next occurrence is dated today, Planning hides the
  slipped one from *Overdue* (it’s not a decision — the next one is already on your desk). One-off
  overdue tasks are unaffected, and the hidden occurrence is still findable in All Tasks.
- **Right-to-left titles render correctly on the Backlog list** (`FR-DESIGN-3.5`) — a Backlog task
  titled in Hebrew or Arabic now lays out right-to-left, matching every other task surface. Chrome
  and TalkBack order are unchanged.

## [2.5.0] — 2026-07-13

### Added
- **Connect an existing goal to a theme, from either side** (`FR-DIR-8`) — the goal editor
  now exposes the full theme relation in **both create and edit** modes: a **primary theme**
  selector (including *No theme · orphan*) plus an **Also serves** multi-select, so a goal can
  serve any number of themes (`DM-GOAL-6`). The expanded body of an active theme gains a
  **Connect an existing goal** row alongside *Add a goal to this theme*; its picker lists
  connectable goals (orphans first, then goals owned by other themes), and connecting an orphan
  asks once whether this theme should become its primary. Shared goals in a theme's cluster gain
  an **unlink** affordance. The link lives only on the goal (`primaryThemeId` + `themeIds`); no
  schema change, no gamification copy.

### Changed
- **Backlog is now a state-scoped surface** (`FR-BACKLOG-5`) — a task created *from* the Backlog
  view (quick-add or the expand-to-full-form route) defaults to **Backlog** state and stays
  undated, so it lands in the someday pool instead of silently dropping into Today's Open list.
  Adds from Today, Planning and All Tasks are unchanged. Adding recurrence to a Backlog-defaulted
  task auto-promotes it to Open with today's date, since a recurring series can't be "someday"
  (`FR-BACKLOG-4`), with a one-line hint.

### Fixed
- **Add-button no longer overlaps the bottom nav** (`FR-NAV-8`) — the floating **+** on All Tasks
  and Backlog now clears the bottom tab bar, matching Today and Planning.
- **Bottom-nav taps never strand you on a reference view** (`FR-NAV-9`) — opening All Tasks or
  Backlog and then tapping a tab (e.g. Today → Backlog → Habits → Today) could leave the reference
  view stuck on top of the tab you selected. Tapping a tab now always lands on that tab, while
  ordinary within-tab state (scroll position, expanded rows) is preserved.

## [2.4.0] — 2026-07-12

### Added
- **Log a habit for a chosen date** (`FR-HAB-8`) — expanded habit cards gain a *Log a date…*
  affordance under *Mark today done* / *Log today*. A calendar sheet lets you pick any past
  day (up to today; future dates are visibly disabled) and log it directly — frequency
  habits toggle, quantitative habits open the amount sheet with any existing amount for
  that date pre-filled. The rest of the habit surface (week rate, arc, fresh start,
  history grid, direct-log path on today) is unchanged.
- **Positive completion chime on Done** (`FR-AUDIO-1`) — a short, soft two-note chime
  plays when a task moves *Open → Done* via a checkbox tap on Today, Planning (dated **and**
  overdue), and All Tasks. Routed through `USAGE_ASSISTANCE_SONIFICATION`, so the device's
  silent / Do Not Disturb profile is authoritative. A new **Sounds · Completion chime**
  toggle in Settings (default **ON**, key `chime_enabled`) lets you turn it off; visible
  completion feedback (checkbox fill, strike-through, Journey write) is unchanged either
  way. The chime is not gamification — it does not fire on undo, on `StateSheet` transitions
  to Done, on habit-log paths, or on Review/Journey scans (`DM-NOT`).

### Changed
- **Uniform chrome palette across the five tabs** (`FR-NAV-5`) — Habits, Directions and
  Journey now wear the same rust `RustHeader` as Today, Planning, All Tasks and Backlog,
  so switching tabs no longer swaps between rust and cream chrome. The Habits fresh-start
  banner and Directions accent stay green — the change is only the top-of-screen header.
- **Consistent nav affordance on All Tasks and Backlog** (`FR-NAV-6`) — the bottom tab bar
  (Today · Planning · Habits · Directions · Journey) and the hamburger menu are now present
  on All Tasks and Backlog, matching the primary five tabs. The **X close** returns you to
  wherever you came from (menu → the tab you were on; Planning "undated" chip → Planning).
- **Uniform title size across headers** (`FR-NAV-7`) — the "compact" (smaller) title styling
  is removed; every `RustHeader` now uses the same title size for a calmer, non-hierarchical
  read of the tabs. On Planning, the header is restructured so the kicker line carries the
  current date ("SUNDAY · JUL 12") and the title stays "Planning".
- **About visual design refresh** (`FR-ABOUT-2`) — the About screen uses three distinct
  type roles (serif "Produced by The Fox Works" · body "Implemented by Claude" · mono
  "Version 2.4.0"), a viewport-proportional Fox Works roundel that scales with screen
  size while staying inside a legible 96–240 dp band, and vertical centering that falls back
  to scrolling on very large font scales.
- **Overdue action-set + tappable checkbox in Planning** (`FR-PLAN-4`) — the overdue-row
  action-set now offers *Do Today · Reschedule · Change Status* (instead of the previous
  *Do Today · Reschedule · Someday · Drop*), and the overdue row's checkbox is directly
  tappable to mark the task Done in place — no editor round-trip. Change Status opens the
  full state sheet.

## [2.3.0] — 2026-07-11

### Added
- **About screen** (`FR-ABOUT-1`, `FR-NAV-4`) — a new **About** entry in the "Around the den"
  menu (Today · Planning · Habits · Directions · Journey · All Tasks) opens a reference
  screen that identifies the producer ("The Fox Works"), the implementer ("Claude"), and the
  running version (`Version X.Y.Z`, read from the shipped build), with the Fox Works roundel
  and a one-tap **Contact Us** mailto link to `thefoxworksdotnet@gmail.com`. If no email app
  is available, an inline hint appears instead of a crash.

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

[2.6.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.6.0
[2.5.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.5.0
[2.4.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.4.0
[2.3.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.3.0
[2.2.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.2.0
[2.1.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.1.0
[2.0.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.0.0
