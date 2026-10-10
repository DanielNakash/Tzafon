---
status: accepted
---

# A per-occurrence deadline is an offset from each to-do date

Since v2.13.0, a repeating task's **per-occurrence deadline** is stored as an offset: each occurrence's deadline falls `dueOffsetDays` (0–30) after its own to-do date (`DueMode.OFFSET`, added in the additive Room 5→6 migration). Before this, a per-occurrence deadline was a whole second rule (`DueMode.RECURRING` + `dueRule`), whose dates were paired with the to-do dates by position. We changed it because the Task Editor Redesign edits a task's repeat inline in a Timing pane that has to fit on one screen, and a second full rule builder doesn't fit there. An offset also matches what users mean by "a fresh deadline per occurrence": each one is due a set number of days after you plan to work on it.

`DueMode.RECURRING` and `dueRule` are **kept on purpose, not dead code**. Series saved before v2.13.0 still carry them, and the engine still computes their deadlines. The editor shows such a series as a read-only "Custom deadline rule" whose only action is "Switch to offset". Switching sets the offset to the gap between the next occurrence's to-do date and its deadline, raised to 0 if that gap is negative. No new series can be given a deadline rule. Don't remove them while exports or synced data from older versions may still hold them.

## Considered options

- **Keep the separate deadline rule.** It's the most expressive, but it needs a second rule builder in the Timing pane, and pairing by position lets the two rules drift apart ("the two need not align"), which is hard to explain.
- **Deadline on the occurrence's own day (offset 0 only).** It needs no migration, since it can be stored as `dueRule = rule`, but it leaves no slack: every occurrence is overdue the next day.

## Consequences

- An additive Room migration 5→6 (`NFR-DATA-1`). The data export (`DM-EXPORT-1`) and the Firestore codec carry `dueOffsetDays`, and a v2.12 export (without the field) still imports.
- An offset never ends a series. Only a **fixed deadline** (`DueMode.SINGULAR`) doubles as the series' end date.
- Decided in the Wayfinder ticket "Repeat settings inline in the Timing pane" (DanielNakash/Tzafon#5), built in "Domain: per-occurrence deadline offset (Room 5→6)" (DanielNakash/Tzafon#14).
