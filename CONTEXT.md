# Tzafon

A task and intention manager: tasks carry a plan (when you'll work on them), an optional promise (when they must be finished), and an alignment (the habit or goal they serve).

## Language

### Tasks and repetition

**Task**:
One thing to do, with exactly one of five statuses.

**Series**:
The rule-bearing template behind a repeating task; it generates occurrences.
_Avoid_: recurring task (as a noun for the template)

**Occurrence**:
One generated task of a series, identified by its series and its date.
_Avoid_: instance

**Closed series**:
A series ended as of the day it was closed: its occurrences not yet done are closed, and later ones stop appearing. Reopening it resumes occurrences from that day on and never changes past ones.

**Frozen series**:
A series on hold: it produces no occurrences until you thaw it.

**Edit scope**:
How far an edit to, or deletion of, a repeating task reaches: just this one, this and every future one, or the whole series. It is asked once, when the change is saved or the task deleted, never while editing.

**Repeat**:
A task's rule for generating occurrences; a task with one is a repeating task. It is weekly (on chosen weekdays), monthly (on a date or an nth weekday), or every N days.
_Avoid_: recurrence, recurring

**End date**:
The last day a series produces occurrences; a series with none "ends never". A fixed deadline is also the end date.

### Dates

**To-do date**:
The day you plan to work on a task; it decides what appears in Today.
_Avoid_: scheduled date, To Do Date, TO DO

**Deadline**:
The day a task must be finished by; it decides lateness. Its closeness and lateness read as "due", "due today" and "overdue", but the date itself is always the deadline.
_Avoid_: due date

**Fixed deadline**:
A repeating task's one deadline, shared by every occurrence; it also ends the series.
_Avoid_: single deadline, singular

**Per-occurrence deadline**:
A repeating task's deadline given afresh to each occurrence, a set number of days after that occurrence's to-do date.
_Avoid_: recurring deadline

**Undated**:
A task with no to-do date.

**Window**:
The days from a task's to-do date to its deadline, the time you can still move the plan around without being late. A deadline before the to-do date means you plan to work on the task after it's due.

### Status

**Status**:
Which of the five a task is in: Open, Done, Closed, Frozen or Backlog.
_Avoid_: state

**Backlog**:
The status of a task parked with no date, off the calendar until you pick it up.
_Avoid_: Someday, someday / maybe

**Frozen**:
The status of a task on hold, hidden from the calendar until you thaw it; it keeps its to-do date. A repeating task can only be frozen as a whole series: a single occurrence is never frozen on its own.

**Thaw**:
Moving a frozen task to any other state.

**Mark done / mark open**:
A tap on a task's checkbox: an Open task becomes Done, a Done task becomes Open.
_Avoid_: reopen (for a single task; reopening is what a closed series does), complete / uncomplete

**Status sheet**:
The sheet that shows all five statuses and what each one does, reached by holding a task's checkbox. A change to just this task applies the moment it's picked; a change that reaches a series asks for a scope first.

**Scope panel**:
What the status sheet turns into when a pick reaches a series: the chosen status, its "Just this one" / "The whole series" choices, and a button named for the choice ("Close this one", "Thaw series"). Back returns without changing anything. Freeze series has a single choice, because freezing always takes the whole series.

**Status strip**:
The five status chips on the editor's Task pane. A chip changes only the draft, applied at Save, unless the status reaches a series: then it opens the scope panel, and a whole-series choice applies at once and closes the editor.

**Undo**:
Taking back a status change while its toast is showing: the task returns exactly as it was, and what it ticked or moved is taken back by the same amount. A whole-series change can't be undone.

### Alignment

**Alignment**:
A task's links upward: the one habit it ticks and the goals it moves.

**Ticks**:
What a done task does to its habit: counts once toward it.

**Moves**:
What a done task does to each goal it links: advances it once.

**Serves**:
Alignment pointing further up: a goal or habit serves a theme, a habit serves a goal.

**Theme**:
A long-running direction that goals and habits serve. A task never has a theme of its own: it inherits the themes of the goals it moves and the habit it ticks.

**Cue**:
The trigger a task or habit hangs on: "when X, I will do Y", after a routine, at a time or at a place.

### Content

**Notes**:
The free-text body of a task, under its title.
_Avoid_: description

### Editor

**Pane**:
One of the task editor's three tabs: Task (title, notes, status, delete), Timing (dates and repeat) or Alignment (habit, goals, cue).

**Opening snapshot**:
The task as the editor opened it, presets included. Changes are measured against it, so undoing an edit undoes the change.

**Changed dot**:
The dot on a pane's tab when that pane differs from the opening snapshot.
