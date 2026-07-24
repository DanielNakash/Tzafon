# Changelog

All notable changes to **Tzafon** (צפון — *north*), the native Android task &
intention manager by The Fox Works. This log starts at the v2.0.0 native rebuild.

The format follows [Keep a Changelog](https://keepachangelog.com/); versions use
[Semantic Versioning](https://semver.org/). Requirement codes (`FR-*`, `DM-*`) refer
to the versioned requirements documents in the repo.

---

## [2.10.0] — 2026-07-24

### Changed
- **Goal-card step rows visibly right-align on Hebrew/Arabic labels** (`FR-DESIGN-3.7`) —
  the row now fills the card's width and the label carries its own weight, so under the
  per-label RTL wrapper introduced in v2.9.0 (`FR-DESIGN-3.6`) the checkbox actually lands
  at the **right edge** of the card and the glyphs anchor to the reading margin. Mixed
  goals continue to render per-row-independently — an English step in the same list still
  lays out LTR — and the whole row is now a togglable target. The editor is unchanged.

### Fixed
- **AT_TIME cues can be saved without a label** (`FR-CUE-2`) — the "Set the cue" button on
  the AT A TIME variant of the cue sheet now gates on a valid `HH:mm` alone; the label is
  optional, matching the fact that the time is the load-bearing field on a time-driven
  reminder. AFTER A ROUTINE and AT A PLACE continue to require a label (those triggers
  have no other identifying field). The saved label-less cue and its notification degrade
  cleanly to just the time.
- **AT_TIME reminders fire *at* the set time on Android 12+** (`FR-NOTIF-4`) — a new
  **Precise cue timing** row under Settings → Reminders surfaces the Android
  exact-alarm special-access state and hands the user into the standard OS grant page
  with one tap. On grant, today's already-scheduled alarms are immediately refreshed onto
  the exact path, so a cue set to `08:30` fires at `08:30` — not whenever the OS next
  wakes. When the grant is missing, reminders continue to fire as before (imprecise
  fallback); the app never requires the grant to deliver at all.

## [2.9.0] — 2026-07-23

### Changed
- **Today's "slipped past" count matches Planning's overdue list** (`FR-TODAY-8`) — the calm
  banner on Today ("N tasks slipped past — tidy them up in Planning") now applies the same
  same-series hide rule Planning uses (`FR-PLAN-5`), so a slipped recurring occurrence whose
  next occurrence is already today drops out of the count. The banner and Planning's overdue
  section resolve to the same integer by construction — no more "the banner promised 3 but
  Planning only shows 1".
- **Goal step rows respect content direction on the goal card and in the editor**
  (`FR-DESIGN-3.6`) — a Hebrew or Arabic step label now lays out right-to-left at the row
  level: the checkbox anchors to the trailing (right) edge and the label reads from the right
  reading margin. Mixed goals stay per-row-independent — an English step in the same list
  keeps its LTR layout. Applies to both the read-view step row and the editable-step row.

### Fixed
- **The AT_TIME cue accepts a colon and requires a valid time to save** (`FR-CUE-1`) — the
  time field in the cue sheet (used by both the Task editor and the Habit editor) now uses a
  text IME so the **colon** key is available on every keyboard, and only digits or a colon
  are accepted. The **Set the cue** button now stays disabled until a valid `HH:mm` time is
  entered (`00:00`–`23:59`, zero-padded) — so an AT_TIME cue can no longer be saved without
  the one field a time-driven reminder actually needs.
- **A date change on a recurring occurrence with "This & future" or "All" now takes effect**
  (`FR-REC-6`) — editing a recurring task's date (e.g. shifting "Cleaning" every 2 days from
  1.12 / 3.12 / 5.12 … onto 4.12 / 6.12 / 8.12 …) previously silently dropped the new date;
  the series was re-enumerated from the *original* occurrence date. The FORWARD and ALL save
  paths now honour the typed date as the series' new anchor, so the phase shift the user
  typed is the phase shift the series adopts. The per-occurrence override branch ("This one")
  is unchanged; the recurrence engine is unchanged.

## [2.8.0] — 2026-07-18

### Added
- **Export and re-import your Tzafon data** (`FR-DATA-1`, `FR-DATA-2`, `DM-EXPORT-1`) — Settings gains
  a **Your data** section with **Export data** and **Import data**. Export writes a pretty-printed,
  UTF-8 JSON file (default name `tzafon-backup-<date>.json`) via the Android file picker, containing
  every task, series, habit, habit log, goal, theme, review, and contribution — no streaks, no
  cached derived counts, only the domain the app owns. Import validates the file up-front (a foreign
  file, a truncated one, or a file from a newer Tzafon is rejected with a precise, non-technical
  message and nothing is written). When the app already has data, a "How should conflicts be
  handled?" dialog offers **Backup wins** (restore this backup exactly) or **Current wins**
  (fill in only what's missing) — one choice, applied to every matching row. The apply is atomic:
  a mid-flight failure rolls the whole restore back, never a half-imported store. A persistent
  caution on the Export row reminds you the file is plaintext — store it somewhere you trust.

## [2.7.0] — 2026-07-15

### Added
- **Point a habit at a direction, not only a goal** (`FR-HAB-10`) — the Habit editor gains a
  **Serves a direction · optional** section that mirrors the goal editor: pick a **primary
  theme** (or *No direction*), then optionally add one or more **Also serves** themes. Habits
  that fit a direction ("more push-ups a day, for better health") but don't map to a concrete
  goal now belong somewhere — Directions attributes them to the theme directly. Both the goal
  link and the theme link are independent and optional.
- **Signing in is now required** (`FR-AUTH-1`) — the app enters through the Welcome view when
  signed out; the existing Google sign-in navigates to Today on success. Signing out from
  Settings returns you to Welcome. The old cold-start `welcomeSeen` flag is retired: the
  auth state itself is the gate.
- **Rolling 7-day rhythm strip on habit cards** (`FR-HAB-11`) — every habit card, both
  collapsed and expanded, now shows a **last-7-days rhythm glance** so the recent shape of
  the habit is scannable without expanding. On the expanded card the strip is **horizontally
  scrollable** (rolling 28 days, pinned to today, week separators every 7 cells) so you can
  pan back and read earlier values. Frequency days render as filled/empty cells, quantitative
  days as proportional bars against target. The old expanded-row `WeekDots` / `QuantBars`
  are replaced (one visualisation, not two); the `THIS WEEK` text stat stays.
- **Confirm gate on the "Log a date…" picker for yes/no habits** (`FR-HAB-12`) — the picker
  now matches the quantitative flow. Tapping a date **selects** it (highlighted, not written);
  a rust **Log ${date}** button (or **Un-log ${date}** on an already-logged day) and a Cancel
  row appear below the calendar. The write only fires on confirm; tapping a different date
  swaps the selection, tapping the selected date clears it, Cancel dismisses. TalkBack
  announcements switch from *"Tap to log …"* to *"Tap to select … for logging"* accordingly.

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

[2.10.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.10.0
[2.9.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.9.0
[2.8.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.8.0
[2.7.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.7.0
[2.6.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.6.0
[2.5.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.5.0
[2.4.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.4.0
[2.3.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.3.0
[2.2.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.2.0
[2.1.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.1.0
[2.0.0]: https://github.com/DanielNakash/Tzafon/releases/tag/v2.0.0
