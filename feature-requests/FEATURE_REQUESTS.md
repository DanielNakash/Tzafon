# Tzafon — Feature Requests (inbox)

This is the **only file you edit by hand**. Append a request block, set `status: new`, save.
The automation does the rest (see `feature-requests/README.md`).

**Pipeline switch:** to pause all automation, change the line below to `pipeline: paused`.

pipeline: active

---

## How to add a request

Copy the template, give it a unique id (`FR-YYYY-MM-DD-x`), write a plain-language
description of what you want. Leave the `(auto)` fields alone — the groomer/builder fill them.

```
## FR-2026-07-08-a
status: new
target: (auto)
requested: 2026-07-08
implemented: (auto)
requirements: (auto)

<one or more sentences describing the feature in your own words>
```

Status lifecycle: `new` → `groomed` (requirements written) → `implemented` (shipped in a version).
A request that conflicts with a locked decision (e.g. gamification, `DM-NOT`) is set `rejected`
with a `reason:` line and never built.

---

<!-- Add requests below this line. -->
## FR-2026-07-10-a
status: implemented
target: 2.1.0
implemented: 2.1.0 / 12972bf
When creating a quick task in Today, default the date to today.

## FR-2026-07-10-b
status: implemented
target: 2.1.0
implemented: 2.1.0 / 6968574
Currently in Habits View, each Habit shows its full set of details. Already with 3 habits it gets a bit difficult to quickly see all the habits. The default state should be a collapsed view showing the habit name, the log activity button, and an option to expand it to see all the details.

## FR-2026-07-10-c
status: implemented
target: 2.1.0
implemented: 2.1.0 / b2bb8d3
Fix - When creating a new Habit, the colors of the button for confirmation make it unreadable (white text on a bright background).

## FR-2026-07-10-d
status: implemented
target: 2.1.0
implemented: 2.1.0 / 58c6233
Add RTL (Right To Left) support for text fields while editing and viewing them. For example, the task title.

## FR-2026-07-10-e
status: implemented
target: 2.2.0
implemented: 2.2.0 / d827a6e
Add an option to edit the Habit's details - name, frequency etc. as in a new habit creation.

## FR-2026-07-10-f
status: implemented
target: 2.2.0
implemented: 2.2.0 / d827a6e
There's nowhere in the app an option to add a new Goal. I think it would make most sense for it to live in the Directions View. Consider how to include an add option for a Goal without hurting the existing capability to add a Theme. The form has to include all the details required for a Goal.

## FR-2026-07-10-g
status: implemented
target: 2.2.0
implemented: 2.2.0 / d827a6e
The menu that is reachable through the logo of "The Fox Works" is very unintuitive. Please replace it with the traditional "hamburger" icon.

## FR-2026-07-10-h
status: implemented
target: 2.3.0
implemented: 2.3.0 / 6511c5a
Add an "about" section:
* Accessing the "About" section shall be from the hamburger menu.
* It shall include:
    * Title - "About"
    * The following text:
        * First row - "Produced by The Fox Works"
        * Second row - "Implemented by Claude"
        * The app's current version number in the following pattern - "Version X.Y.Z"
    * The logo of "The Fox Works"
    * Under the logo, a link:
        * Display text: "Contact Us"
        * URL for triggering an email to thefoxworksdotnet@gmail.com

## FR-2026-07-11-a
status: implemented
target: 2.4.0
implemented: 2.4.0 / fb95262
Please consider the design of the contents of the "About" screen at least in the following aspects:
* Chosen font(s) and how they're used for the text elements in this screen
* Size of the logo - On a phone oriented in potrait, I would like it to be about half as wide as the screen, but if the phone is rotated, or the screen is wide (foldable's inner screen for example) this may become an issue. So, I would like the logo to be bigger, but I don't want it to accidentally be "overwhelmingly" big. As a rule of thumb, the entire content of the About screen has to be visible without any scrolling.
* Consider whether centering the entire content of the About screen to the middle height of the screen would make sense from a visual design perspective.


## FR-2026-07-11-b
status: implemented
target: 2.4.0
implemented: 2.4.0 / fb95262
Consistency of views look and behavior - There are several differences between how some Views look and behave, which IMHO do not reflect any meaning, and therefore should be made consistent.
That said, if there is a View where is DOES make sense to have it womewhat different because it is different to others, please consider if and how it should look different.
A few things I noticed that has to be ammended:
* Header color is different between some of the views (see Today View and Habits View for example). I prefer keeping it as the darker color used in Today View for example.
* Most Views has the hamburger (Today), but Setting View has an "x", and the About View has a "back" arrow. Aside from making this confusing to use, it also in some cases locks the user out of the main Views, forcing the user to shut down the app and restart it. For example, after entering the Backlog View, it's not possible to go back to Today View. Suggestion:
    * All Tasks View and Backlog View get the same navigation bar at the bottom as in Today View.
    * Settings View and About View gets an "x" which pressing it returns the user to the last View they were at.
* The Size of the title text for some Views is different - Planning and Settings are smaller for example. Please check how this can also be made consistent. For example, for the Planning View we can:
    * Put the date where the "WHAT'S ON THE TABLE" text is now, so that the date only appears once at the top, and the original text that was on the top is removed.
    * This leaves more room for the title text "Planning" to be as big as in the other Views.

## FR-2026-07-11-c
status: implemented
target: 2.4.0
implemented: 2.4.0 / fb95262
When logging a habit directly in the Habits View I want to be able to log a date of my choosing. For example, if I forgot marking yesterday. Current "log" and "Mark today done" only allow marking for today. I think that the "log" button should allow picking a date, while keeping the "Mark today done" button as is.

## FR-2026-07-11-d
status: implemented
target: 2.4.0
implemented: 2.4.0 / fb95262
For overdue tasks in the Planning View, please add an option to mark as done. I expected that pressing the unchecked checkbox of the task will do just that, but instead it takes me into editing the task. Please make the following changes:
* Currently, there are four options for each overdue task: Today, Reschedule, Someday and Drop.
    * Keep Today and Reschedule
    * Remove Someday and Drop
    * Add "Change Status"
* Make the checkbox clickable so that clicking it marks the task as Done.

## FR-2026-07-11-e
status: implemented
target: 2.4.0
implemented: 2.4.0 / fb95262
When a task is mark done by pressing its checkbox, make a nice positive short chime sound.

## FR-2026-07-12-a
status: implemented
implemented: 2.5.0 / efedbef
target: 2.5.0
Bug fix: In All Tasks View and in Backlog View the add button is partially hidden by the navigation bar. The button should be moved app similarly to the Today View.

## FR-2026-07-12-b
status: implemented
implemented: 2.5.0 / 8058b39
target: 2.5.0
Currently there's no way to link between an existing goal and a theme. This new feature shall add that capability in two places:
* From a theme's details the user shall be able to connect to an existing goal.
* From a goal's details the user shall be able to connect to an existing theme.

## FR-2026-07-12-c
status: implemented
implemented: 2.5.0 / 7a569a6
target: 2.5.0
When creating a new Task when in the Backlog View, it shall default to the backlog/someday state, regardless of whether this is a quick add or a full form add.

## FR-2026-07-12-d
status: implemented
implemented: 2.5.0 / 174507f
target: 2.5.0
Bug Fix: When navigating from a view to the Backlog, the navigation button for the view the user came from refers to backlog instead of the view it is supposed to.
Example:
* The user is in the Today View and navigates to the Backlog View, and then continues to Habits View.
* The user taps "Today" in the navigation bar, but that takes him back to the Backlog View.
* This state remains until the user presses the Android back button while on the Today-hidden-by-Backlog View.
This may happen to multiple Views following the same procedure.

## FR-2026-07-13-a
status: implemented
implemented: 2.6.0 / 7584a43
requirements: FR-PLAN-5
target: 2.6.0
For overdue recurring tasks, if the next recurrence is today, hide the slipped task from the Planning View.
For example, a task "clean desk" is set to recur once every 2 days, today is Tuesday, and the task from Sunday was left open. The expectation of this feature request is that the Sunday instance of "clean desk" shall be hidden in the Planning View.

## FR-2026-07-13-b
status: implemented
implemented: 2.6.0 / a24082f
requirements: FR-HAB-9
target: 2.6.0
Allow to undo-logged habit. For example, I logged today by mistake while I meant to log yesterday. Currently, once a day is logged, it cannot be unlogged.

## FR-2026-07-13-c
status: implemented
implemented: 2.6.0 / 69354bc
requirements: FR-DESIGN-4, DM-PREF-1, FR-SET-4
target: 2.6.0
Add a capability to choose from a different color palette. This option shall be available from the Settings View.
The default shall be the current palette. There shall also be a few additional palettes based on blue, green, magenta and teal (each is a base for a different palette). Verify that all the relevant items are affected by the change in theme - background, text color, objects, buttons etc.

## FR-2026-07-13-d
status: implemented
implemented: 2.6.0 / 24fe8d3
requirements: FR-DESIGN-3.5
target: 2.6.0
Please add RTL support also to the tasks in the Backlog View.

## FR-2026-07-15-a
status: implemented
target: 2.7.0
requested: 2026-07-15
implemented: 2.7.0 / a72633b
requirements: FR-HAB-10
Allow linking a Habit directly to a Theme, not only to a Goal. In several cases a habit cannot be tied to a specific, well-defined goal — for example, doing a certain number of push-ups a day for better health does not lead toward a concrete goal unless I invent one, but it fits neatly under a theme like "better health". The Habit editor should let me point a habit at a theme (a "direction"), the way it already lets me point it at a goal, and both should be optional. When a habit already serves a goal, prefer inheriting that goal's theme but still allow a goalless habit to be attached to a theme on its own.

## FR-2026-07-15-b
status: implemented
target: 2.7.0
requested: 2026-07-15
implemented: 2.7.0 / da1be74
requirements: FR-AUTH-1
Using the app shall require logging in.
On first opening the app, the welcome view shall be shown with the Google SSO button.
Once login is complete, the app takes the user to the Today View.
In the Settings View the user may log out. If they log out, they're taken to the welcome view and they may log back in or log in with another account, using the same Google SSO button.

## FR-2026-07-15-c
status: implemented
target: 2.7.0
requested: 2026-07-15
implemented: 2.7.0 / 3c094c9
requirements: FR-HAB-11
I want to change how a habit's recent activity is shown in the Habits View.
Already in the collapsed card it should be possible to glance at the activity logged in the last 7 days, so I can see a habit's recent rhythm without expanding it. The same last-7-days glance should also be visible in the expanded card.
In the expanded card, it should additionally be possible to pan/scroll sideways across the shown days to reach earlier dates and see their values.
Note that quantitative habits already have a "THIS WEEK" section, so that section will have to be adapted and merged with this new last-7-days / scrollable-history capability rather than duplicating it.

## FR-2026-07-15-d
status: implemented
target: 2.7.0
requested: 2026-07-15
implemented: 2.7.0 / 2f58000
requirements: FR-HAB-12
Change the "Log a date" confirmation behavior so that yes/no habits behave more like quantity habits.
Today, when I press "Log a date" for a quantity habit, the app asks me for the quantity and shows a confirmation button before anything is recorded. But for a yes/no habit, the date I tap is marked immediately, which I don't think is the right behavior.
Instead, for a yes/no habit the chosen date should be colored/marked differently to show it has been selected (not yet logged), and there should be a confirmation button. While in this selected-but-unconfirmed state, the user should be able to confirm the log, choose a different date instead, or cancel the selection entirely.

## FR-2026-07-18-a
status: implemented
target: 2.8.0
requested: 2026-07-18
implemented: 2.8.0 / 89af11c
requirements: DM-EXPORT-1, FR-DATA-1, FR-DATA-2

Enable exporting the user's data. On one hand it has to be in a format that is readily readable — not proprietary, coded or encrypted. On the other hand it has to have some structure and preserve the existing relationships between the different objects. The goal is twofold: (1) let the user take their data and do whatever they want with it, and (2) import the data back in case of a data-loss event (i.e. a backup/restore). Preserving the privacy of the exported file is the user's responsibility. App preferences are out of scope. When importing into a non-empty app, ask the user how to treat conflicts, offering two options: "Backup wins" (the backup entry overwrites the existing entry) and "Current wins" (the backup entry is skipped). The export and import options shall live in the Settings view.

## FR-2026-07-19-a
status: groomed
target: 2.9.0
If a "slipped task" is one that is hidden in the Planning View, it should also be ignored and not mentioned in the "slipped tasks" mention shown in the Today View.
For example, if I have a daily task, and I didn't do the task yesterday, then:
* It should not appear in Planning View (already implemented)
* It should not be included in the count of "slipped tasks" in Today View (the part to be done)

## FR-2026-07-19-b
status: groomed
target: 2.9.0
Bugs fixes: When editing a cue to be set at a time:
* The text box for the time doesn't allow typing a colon, so it's not clear if and how time is accepted by this field. This needs to be fixed.
* Additionally, the condition for approval is having a text for describing the cue, rather than having an actual time. This also needs to be fixed.

## FR-2026-07-19-c
status: groomed
target: 2.9.0
The text for a goal's steps is not right aligned for RTL language.