# Tzafon v2.0.0 — Build Plan

**App:** Tzafon (צפון, "north") — native Android, Kotlin + Jetpack Compose (Material 3, Den-themed).
**Contract:** `v2.0.0 Requirements.md` (final). Every item below cites its requirement codes.
**Design:** `task-manager/project/Tzafon v2.0.html` + `tz/*.jsx` (read in source; screenshots cross-referenced).
**Reference implementation (oracle):** the v1.1.0 web app at `~/LocalWorkshop/Task Manager/PROJECT/src/`
(`utils/recurrence.js` + `utils/recurrence.test.js`, `services/series.js`, `components/*`).
**Project path:** `~/LocalWorkshop/Tzafon/android/` (space-free). Git repo rooted at `~/LocalWorkshop/Tzafon/`.

This plan is for the record, not a gate — implementation proceeds M0 → M9 immediately after it is written.

---

## 1. Locked decisions honored

- Native Android; `applicationId com.thefoxworks.tzafon`; compileSdk 37 / targetSdk 36 / minSdk 26.
- Toolchain: Gradle 9.6.1, AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, JBR 21. No `kotlin.android`
  plugin (AGP 9 built-in Kotlin); `org.jetbrains.kotlin.plugin.compose` only for Compose.
- **No streaks / points / badges / adherence % anywhere** (`DM-NOT`, `DEC-2/3/4`).
- **Local-first persistence (Room)** behind repository interfaces. **Firebase (Auth + Firestore sync) is
  deferred** — see §5 "Firebase seam" for exactly when and how it lands.

## 2. Data model → Room schema

Object spine `Theme → (Goal | Habit) → Task`, Cue embedded, Review/Focus as loops (`§4` of the
requirements). All dates are ISO `yyyy-MM-dd` strings (matches the oracle engine; index-friendly).
IDs are client-generated UUIDs except series occurrences, which use the deterministic id
`{seriesId}__{occurrenceDate}` (idempotent generation, carried from v1.1.0 `series.js`).

### 2.1 Tables

**`tasks`** (`DM-TASK`) — occurrences AND standalone tasks, exactly like v1.1.0:
| column | type | notes |
|---|---|---|
| id | TEXT PK | UUID or `{seriesId}__{date}` |
| title, description | TEXT | |
| toDoDate, dueDate | TEXT? | ISO; **new tasks default undated** (`FR-CAPTURE-2`) |
| state | TEXT | `OPEN · DONE · CLOSED · FROZEN · BACKLOG` (`DM-TASK-1`); native shape from first write (`DEC-1a`) |
| seriesId | TEXT? | FK → series |
| occurrenceDate | TEXT? | slot date for occurrences |
| overridden | INT | per-occurrence edit flag (carried from v1.1.0) |
| cueType, cueLabel, cueTime | TEXT? | embedded Cue (`DM-CUE-1/2`): `AFTER_ROUTINE · AT_TIME · AT_PLACE`, label ("after morning coffee"), optional HH:mm for reminders |
| themeId | TEXT? | direct task→theme auto-bridge (**D1**, `DM-TASK-5`) |
| habitId | TEXT? | **at most one** habit (`DM-REL`) |
| commitment | TEXT? | `DM-TASK-7` |
| focusDate | TEXT? | Today's-Focus marker (`DM-FOCUS-1` — a marker, not a store) |
| focusWeekStart | TEXT? | weekly-priorities marker |
| sortOrder | INT | manual reorder in Today (`FR-TODAY-2`) |
| createdAt, stateChangedAt, completedAt | INT | epoch ms; completedAt feeds Review/Journey |

`DM-TASK-8`: nothing in the app mutates dates automatically; only generation stamps a new occurrence's
date at creation. `DM-TASK-3`: Close/Freeze/Backlog **keep** themeId/habitId/goal links (dormant).

**`task_goal_links`** — task → many goals (one typical): `(taskId, goalId)` PK.

**`series`** (`FR-REC`) — recurrence rules, one row per series:
id PK; title, description; rule fields exactly as the oracle (`pattern: INTERVAL|WEEKDAY|MONTHDAY`,
`interval`, `unit`, `weekdays` JSON, `weekInterval`, `monthMode`, `monthDate`, `monthWeekPos`,
`monthWeekday`, `monthInterval`); `ruleAnchor`, `startDate`, `endDate?`;
`dueMode: NONE|SINGULAR|RECURRING`, `dueSingular?`, due-rule fields (same shape, `due` prefix);
`exceptions` JSON (deleted slots never regenerate); `generatedThrough`;
`frozen` INT (`FR-REC-5` freeze terminates generation; thaw restores); template alignment
(themeId, habitId, cue fields, goal links JSON) so new occurrences inherit links.

**`themes`** (`DM-THEME`): id PK; name; **why (mandatory)**; windowStart, windowEnd (default one
quarter, user-editable — `DEC-5`); state `ACTIVE · UPCOMING · ARCHIVED`; archivedOutcome
`RENEWED · EVOLVED · RETIRED` + renewedToThemeId (feeds Journey's "directions over time");
accentSlot INT (cycles the three harmonized Den accents); createdAt, archivedAt.
No failed state, no % (`DM-THEME-4`). **Hard cap ≤3 ACTIVE enforced at activation** (`DM-THEME-3`).

**`goals`** (`DM-GOAL`): id PK; title, description; type `STEPPED · ACCUMULATIVE · GENERIC`;
steps JSON `[{label, done}]` (Stepped auto-creates *"Defined this goal ✓"* checked — `DM-GOAL-2`);
targetQty, unit, currentQty (Accumulative; honest non-zero start allowed); state
`ONGOING · COMPLETED · FROZEN`; deadline?, commitment?; primaryThemeId? (`DM-REL-1`);
lastActivityAt (stale-goal nudge, `DM-GOAL-5`); completedAt, createdAt.

**`goal_theme_links`** — goal serves many themes beyond the primary: `(goalId, themeId)` PK.

**`habits`** (`DM-HABIT`): id PK; name; goalType `FREQUENCY · QUANTITATIVE`; target REAL
(N/week for frequency; amount/day for quantitative); unit? ; targetDays INT? (quantitative:
aimed days/week — the design's "4 of 5 days"); cueType/cueLabel/cueTime (a habit without a cue is
just a tracker — the UI nudges but doesn't force); primaryThemeId?; goalId? (**a habit may serve
one Goal**, `DM-HABIT-6`/D2); startedAt (long arc "Running 9 weeks"); createdAt.
Delete is destructive → wipes `habit_logs`, confirmation required (`DM-HABIT-7`).

**`habit_theme_links`** — `(habitId, themeId)` PK.

**`habit_logs`** (`DM-HABIT-1/5`) — per-date history: `(habitId, date)` PK; done INT;
amount REAL (quantitative); source `TASK · DIRECT`; sourceTaskId? (exact reversal).

**`contributions`** (`DM-ATTR-1`, `NFR-DATA-2`) — the provenance ledger: id PK; taskId; goalId;
amount REAL; via `DIRECT · HABIT`; createdAt. Every automatic numeric goal advance writes a row;
un-done / Close / Freeze / Backlog reverses **exactly** these rows and deletes them.

**`reviews`** (`DM-REVIEW-5`): id PK (= periodStart); kind `WEEKLY · MONTHLY`; periodStart,
periodEnd; status `PENDING · PARTIAL · DONE · SKIPPED`; reflectNote?; snapshotJson (done count,
aligned count, per-habit rates, goal deltas — computed once, persisted for Journey); completedAt.

**Settings** — Preferences DataStore (not Room): weekStart (default **SUNDAY**, `DM-REL-2`/`FR-SET-1`),
planning range, notification prefs, dismissed-nudge state, focus/overload thresholds (tunable, §11).

### 2.2 Relationships & attribution (`DM-REL`, `DM-ATTR`)

Cardinality per the `DM-REL` table: all links optional (trellis, not cage). The UI shows one
**primary** parent as *"serves: X"* with a `+N` expander (`DM-REL-1`).

**On Done** (the up-flow, counted once — `DM-ATTR-1`):
1. Habit ledger first (`DM-ATTR-2`): if task.habitId → write `habit_logs` row (frequency: tick;
   quantitative: prompt "how much?", `DM-HABIT-5`) with source=TASK. Log date = the task's To Do
   Date if set, else today. *(The old spec's §8.2 date-prompt rules aren't in the bundle; this
   deterministic rule is the documented stand-in.)*
2. Gather goals: direct `task_goal_links` ∪ `habits[task.habitId].goalId` → **dedupe**.
3. Per goal, apply **once**: Accumulative + direct link → the user-entered amount; Accumulative
   reachable only via habit → auto-advance **only when units match** (`DM-ATTR-3`), amount = the
   habit-logged amount; both paths → prefer the explicit task→goal amount, never both. Stepped /
   Generic / unit-mismatch → directional only (no numeric change, no ledger row).
4. Write `contributions` rows for every numeric change; bump goal.lastActivityAt.

**Reversal** (Done→Open/Closed/Frozen/Backlog): delete this task's TASK-sourced habit log, subtract
its `contributions` amounts, delete the rows. Exact by construction.

### 2.3 Recurrence engine (Kotlin port of the oracle)

Pure Kotlin in `domain/recurrence/`: `enumerateDates`, `computeDue`, `planOccurrences`,
`recurSummary` — behavior-identical to `recurrence.js` (weeks phase-aligned to the anchor's
Sunday-based week, `MAX_ITERS = 4000`, month-clamping, `-1` = last weekday). JUnit tests mirror
`recurrence.test.js` case-for-case, plus:
- **`FR-REC-1` nearest-occurrence floor**: when a series' in-window set is empty and no open
  occurrence ≥ today exists, materialize the single earliest occurrence on/after today
  (deterministic id → idempotent; runs inside topUp; respects exceptions/endDate).
- **`FR-REC-2` conditional year**: date labels append the year only when ≠ current year.
- **`FR-REC-3` effective horizon**: `max(today+60d, furthest date viewed)`, driven by the Planning
  range for the session (`NFR-PERF-2`).
- `FR-REC-4` carry-forward: three patterns, per-occurrence ops (one / forward / all, delete one/all,
  end date), singular vs recurring due modes, occurrences independent of prior completion.
- `FR-REC-5`: freezing a recurring task confirms, then sets series.frozen (generation stops, frozen
  instance retained); thaw restores per `DM-TASK-2`.

## 3. Architecture

**Single `app` module**, package `com.thefoxworks.tzafon`, layered by package:

```
com.thefoxworks.tzafon
├── data/            Room: entities, DAOs, TzafonDatabase, converters
│   ├── repo/        repository IMPLEMENTATIONS (Room-backed)
│   └── settings/    Preferences DataStore
├── domain/          pure Kotlin, fully unit-testable
│   ├── recurrence/  the engine (oracle port + FR-REC-1/2/3)
│   ├── model/       domain types + repository INTERFACES (the Firebase seam)
│   ├── attribution/ DM-ATTR dedup + reversal
│   ├── habits/      in-period rate, arc, fresh-start math
│   ├── review/      review scheduling (weekly/monthly slot logic)
│   └── dates/       ISO date utils + conditional-year formatting
├── ui/
│   ├── theme/       Den tokens, typography, shapes (design-system mapping)
│   ├── components/  TaskRow, chips, headers, nav, Compass, sheets…
│   ├── today/ planning/ alltasks/ backlog/ habits/ directions/ journey/
│   │   review/ editor/ capture/ settings/ welcome/
│   └── nav/         Compose Navigation graph (5 tabs + routes)
└── notif/           AlarmManager + WorkManager (M9)
```

- **MVVM**: one ViewModel per screen; repositories expose `Flow`; ViewModels `combine` into a
  single UI-state `StateFlow`; Compose collects with lifecycle awareness.
- **DI: manual** — an `AppContainer` on the `Application`. Justification: single module, ~8
  repositories, no config variance; Hilt would add KSP passes and ceremony with no payoff at this
  scale, and a hand-built container keeps the Firebase swap-point explicit and readable.
- **Navigation**: `navigation-compose`; bottom bar `Today · Planning · Habits · Directions ·
  Journey` (`FR-NAV-1`), **Today default** (`FR-NAV-2`); All Tasks/Backlog/Settings via the
  header menu; editor/review/sheets as routes or modal sheets.
- **Occurrence top-up** runs on app start and on horizon change (idempotent, converges —
  `NFR-PERF-1/2`), mirroring v1.1.0 `topUp`.

### The Firebase seam (deferred by locked decision)

`domain/model/*Repository` interfaces are the contract; Room implementations are the only binding
in v2.0.0. The schema deliberately mirrors the v1.1.0 Firestore layout (`users/{uid}/tasks`,
`series`, + new `habits/goals/themes/reviews`), keeps client-generated ids, deterministic occurrence
ids, and additive default-tolerant fields (`NFR-DATA-1`) so a later sync layer can mirror rows to
Firestore documents 1:1. **When it lands:** as its own step **at M9-time (M9b), after the M9 polish
pass** — wiring Firebase Auth (Google SSO) + Firestore mirroring + security rules re-created per
`NFR-SEC-1`. It requires the owner to provision the new Firebase project and drop in
`google-services.json` (the one step only the owner can do); if that isn't available when M9 ships,
v2.0.0 tags local-only with the seam documented and M9b queued next. Until then the app is
single-user-on-device: `NFR-OFFLINE-1` is satisfied natively by Room, and data never leaves the
device (`NFR-SEC` trivially holds locally).

**Welcome screen (adaptation):** the Sign-in design (fox hero, compass badge, "Tzafon · צפון") is
built as-is, but since Google SSO ships with M9b, the button reads **"Get started"** and opens the
app locally. It becomes "Continue with Google" when Auth lands. (Design-vs-locked-decision
resolution — recorded here, not worth a stop.)

## 4. Design-system mapping (Den → Compose)

Source of truth: `tz/tz-core.jsx` TZ tokens + `tz-ui.jsx` idioms. Design artboards are 412×892 —
Pixel-class dp, so **design px = dp 1:1**.

**Colors** (exact): bg `#F1E5CF` · surface `#FBF4E6` · surfaceAlt `#EFE3CB` · card `#FFFDF7` ·
ink `#241A12` · muted `#7C6A52` · faint `#A8967C` · rust `#A6421E` · rustDeep `#8A3416` ·
amber `#D9913A` · due `#B23A2E` · green `#5E7A4B` · line `rgba(36,26,18,.14)` ·
line2 `rgba(36,26,18,.08)` · state tokens frozen `#5E7488` / backlog `#7E7086` / closed `#9C8C74` ·
theme accents health `#5E7A4B` / write `#8C4A63` / learn `#3F6E7D` + 12%-alpha washes.
Exposed as an immutable `Den` object via `CompositionLocal`; Material3 `colorScheme` mapped
(primary=rust, background=bg, surface=surface, error=due…) so M3 components inherit the look
(`FR-DESIGN-2` — never default Material).

**Type**: Newsreader (serif — headers, card titles), Hanken Grotesk (body), Spline Sans Mono
(kickers/labels/meta). Bundled as font resources (`FR-DESIGN-1`), downloaded from Google Fonts at
project setup. Scale from the design: kicker mono 11/ls 2.5 caps · section label mono 10.5/ls 1.4
caps · H1 serif 33–37/600 · card title serif 17–19/600 · row title body 16 · meta mono 11.

**Idioms** (from `tz-ui.jsx`, re-expressed in Compose): rust header (Action layer) with kicker +
serif title + amber progress on white-22% track + faded compass motif; cream LayerHeader
(Direction/Reflection); cards `#FFFDF7`, 1px line border, radius 13–16; 25dp rounded-square
checkboxes (state-specific: rust check / skip slash / frozen flake / backlog moon); "serves:" chip
with theme-dot; cue chip with amber bolt; GroupHeader mono caps + hairline; FAB 58dp radius-18 rust
with shadow; bottom nav 84dp with mono labels + rust pill on the active icon; banners/nudges per
`Banner`/`Nudge`. The compass rose and the bespoke line-icon set (`TI.*`) are ported as
`ImageVector`s/Canvas from their SVG path data. Fox logo: `foxworks-logo.png` center-cropped,
bundled, circle-clipped.

**Voice**: "DON'T PANIC" kicker; forgiving copy everywhere ("Missed yesterday? Normal.", "No rush ·
no guilt", "Every state is reversible. Nothing here is a failure.") — all new copy in this voice
(`FR-DESIGN-1`, `PRIN-2`).

The `NavAlt` artboard is explicitly an alternative for comparison — **not built**; `FR-NAV-1` five
tabs is authoritative. The Today "variation" artboards (focus-cards / by-cue) are design
explorations; the primary `TodayScreen` artboard is what ships.

## 5. Milestones M0 → M9 (definitions of done)

Order exactly as the roadmap (`R.2`). Each milestone: implement → JUnit (+ Compose tests where
UI-behavioral) → `assembleDebug` → install → launch → screencap vs design → logcat crash check →
progress note → **git commit** referencing milestone + codes.

- **M0 — Native foundation + v1.1.0 parity.** Gradle/AGP/Kotlin project as locked; git init +
  Android .gitignore; Den theme + fonts + core components; Room `tasks`+`series` behind
  `TaskRepository`/`SeriesRepository`; **Kotlin recurrence engine with FR-REC-1/2/3 built in**,
  JUnit parity tests mirroring `recurrence.test.js` + floor/year/horizon cases; All Tasks view
  (grouping Overdue/Today/Tomorrow/dated/No date, done toggle, empty state); full task editor
  (title/desc, To Do/Due with calendar sheets, recurrence patterns, due modes, end date, edit
  scopes one/forward/all, delete one/all); FAB → editor; **`FR-CAPTURE-2` undated default**;
  welcome screen. Done = tests green; on the emulator a task + a weekly series persist across
  restart and render grouped; screenshots match the design idiom.
- **M1 — Task state machine** (`DM-TASK-1/2/3`, `FR-REC-5`, `FR-ALL-3`). State sheet per design;
  Done/Closed/Frozen transitions + thaw target-state sheet; recurring freeze confirm terminates
  generation, thaw restores; links preserved on skip; All Tasks SHOW toggles Done/Frozen/Closed.
  Done = transition tests (incl. thaw×3 and recurring-freeze) green; toggles verified on-screen.
- **M2 — Action layer** (`FR-NAV`, `FR-TODAY`, `FR-PLAN`, `FR-ALL-4/5`, `FR-CAPTURE-1`,
  `FR-REC-3`). Bottom nav, Today default (focus card section, "Also today", done strip, slippage
  banner, static overload nudge ~8, manual reorder), Planning (overdue decide-cards with
  Today/Reschedule/Someday*/Drop, range presets driving the effective horizon, date groups, Inbox),
  quick-add composer (title+Enter, expand to editor), All Tasks horizon divider + jump-to +
  edge scrubber. (*Someday enables in M3.) Done = membership/overload/reorder tests; nav tap-through
  screenshots match design.
- **M3 — Backlog** (`FR-BACKLOG-1..4`). Backlog screen per design; full transition table with
  reversal stubs; recurring guard offers Frozen; All Tasks Backlog toggle; excluded everywhere.
  Done = transition-matrix tests green; screen verified.
- **M4 — Habits + Cues** (`DM-HABIT`, `DM-CUE`, `FR-HAB`). Habit CRUD + editor (kind, target,
  unit, cue prompt); cue section in the task editor ("When will you do this?"); Habits view per
  design (cue banner, "3 of 4 this week" endowed rate, week dots / quant bars, 5-week history grid,
  arc line, log button, lapse fresh-start card); log-two-ways with TASK/DIRECT sources;
  task↔habit link + `DM-HABIT-8` retroactive choice; destructive delete confirm. **No streaks or
  scores anywhere.** Done = rate/date-rule tests; screenshots vs design.
- **M5 — Goals + attribution** (`DM-GOAL`, `DM-ATTR`). Goal CRUD (three types, endowed first step,
  honest starting progress); goal cards per design (bar, steps with strikethrough, ENGINE habit
  chips, near-done "what's left" framing); progress two ways; **the ledger + dedup + exact
  reversal** (completing M3's stub); completion celebration + "Where would you like to go from
  here?"; ≤5 soft nudge + stale-goal nudge. Interim home: a Directions-tab goal list (the hub
  arrives in M6). Done = the attribution case table (diamond, multi-goal, unit mismatch,
  retroactive, reversal) green.
- **M6 — Themes + Directions hub** (`DM-THEME`, `FR-DIR`). Theme CRUD (mandatory why, window
  default one quarter); 3 states; **≤3-active cap at activation** with save-as-Upcoming offer;
  auto-activation / auto-swap / prompt on window arrival; Directions board per design (ThemeScope,
  BearingRose, collapsed rows with ring %, inline expanded theme with goals/habit chips/add rows,
  window-as-orientation copy, identity mirror line, "this week" nudge, orphan-goals section,
  shared "+N" affordance). Done = cap/swap unit tests; board + expanded screenshots vs design.
- **M7 — Review + Focus** (`DM-REVIEW`, `DM-FOCUS`, `FR-LOOP`, `FR-SET`). Week-start setting +
  Settings screen per design; review scheduler (fires on week start; monthly rides the first weekly
  on/after the 1st; never two in a week; openable through next day; resumable, partial preserved);
  Reflect screen (mirror numbers from real data, habit rates, goals moved, optional line);
  Plan screen (weekly priorities picker with soft cap "3 of 3 · a good number", stale-goal nudge,
  Start the week); Today's-Focus marking from Today with ~3 soft nudge; review artifacts persisted.
  Done = scheduling tests (incl. upgrade + resume) green; flow walked on emulator.
- **M8 — Journey** (`FR-JOURNEY`). Aggregate mirror card (real quarter numbers), Milestones
  (completed goals + theme + date), "Who you're becoming" arcs, "Directions over time" from
  archived themes, Review timeline. Empty-graceful (thin by design). Done = renders accrued
  history; audit confirms no forbidden surfaces (`FR-JOURNEY-3`).
- **M9 — Notifications, offline, polish → release.** Cue-driven notifications (`FR-NOTIF`): at-time
  cues via AlarmManager exact-when-permitted (inexact fallback), after-routine cues via their
  optional reminder time, WorkManager daily top-up + reschedule after reboot; POST_NOTIFICATIONS
  opt-in flow; offline behavior verified (Room-native); TalkBack/contrast/touch-target pass
  (`NFR-A11Y-1`); copy/voice pass; `DM-NOT` end-to-end audit; §9 checklist; release build;
  **tag `v2.0.0`**. Then **M9b (Firebase sync)** as its own step per §3 when the owner provisions
  the project.

## 6. Testing (`NFR-TEST-1`)

- **JUnit (domain)**: recurrence oracle-parity suite + FR-REC-1/2/3 cases; state-machine transition
  matrix; attribution case table; habit rate math (frequency + quantitative, week boundaries by
  week-start); review scheduling. Run in every milestone's verify loop.
- **Compose UI tests** for key behaviors (toggle visibility, quick-add saves undated, state sheet
  transitions) where they pay their way.
- **Emulator loop** every milestone: `assembleDebug → adb install -r → am start → screencap →
  compare vs design/screenshots → input taps → logcat crash grep`.

## 7. Risks & mitigations

- **AGP 9 built-in Kotlin × KSP (Room)** — resolved empirically at M0 first build; fallback is
  `android.builtInKotlin=false` + classic Kotlin plugin at the same versions.
- **Fonts** need a one-time download; fallback = system serif/sans stand-ins (deviation noted, swap
  later — layout tokens unaffected).
- **Recurrence fidelity** — mitigated by the oracle test suite before anything depends on it (M0).
- **Scope of M4–M6** — if a milestone runs large, split at the object/view seam per the roadmap's
  cadence note, never carrying half-built state across a build.
