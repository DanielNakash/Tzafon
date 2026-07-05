# Task Manager — POC Requirements

**Purpose:** Define the minimum viable scope for a proof of concept. Everything here must ship before the POC is considered complete. Nothing beyond this list is in scope.

---

## In Scope

### 1. User Authentication (SSO)
Users must be able to sign in via SSO before accessing any task data.

**Acceptance criteria:**
- User can initiate sign-in via an SSO provider - For this POC Google shall be used.
- Unauthenticated users cannot view or interact with any tasks.
- Authenticated session persists across page refreshes until the user signs out.
- User can sign out explicitly.


### 2. Task List View
Users must be able to see all of their tasks in a single view. This is the entry point for every other operation.

**Acceptance criteria:**
- All tasks belonging to the authenticated user are displayed.
- Each task shows at minimum: title and current state (open / done).
- The list is scoped to the authenticated user — no cross-user data leakage.


### 3. Create a Task
Users must be able to add a new task.

**Acceptance criteria:**
- User can create a task with a title.
- Newly created task appears immediately in the task list.
- Task is saved to persistent storage — it survives a page refresh or app restart.
- Task is created in the **open** state by default.


### 4. Edit a Task
Users must be able to modify an existing task's title.

**Acceptance criteria:**
- User can edit the title of any existing task.
- Changes are saved to persistent storage and reflected immediately in the task list.


### 5. Delete a Task
Users must be able to permanently remove a task.

**Acceptance criteria:**
- User can delete any task from the list.
- Deleted task is removed from persistent storage and no longer appears in the task list after deletion.


### 6. Task State: Open / Done
Users must be able to toggle a task between open and done.

**Acceptance criteria:**
- User can mark an open task as done.
- User can reopen a done task (mark it as open).
- State change is persisted — survives a page refresh or app restart.
- The task list reflects the current state visually (e.g. distinct styling for open vs. done).


### 7. Data Protection
Task data must be protected in transit and at rest.

**Acceptance criteria:**
- All communication between client and server uses HTTPS.
- Task data in the database is accessible only to the owning authenticated user — enforced server-side, not just client-side.
- No task data is exposed to unauthenticated requests.


## Out of Scope for POC

The following items appear in the broader requirements but are explicitly deferred:

- Mobile / phone app
- Push notifications
- Streak tracking
- Encouragement / rewards
- Recurring tasks
- Habit tracking
- Triage view
- In-app character / accountability partner
- "Easily find missed tasks" filtering
- Celebration of milestones


