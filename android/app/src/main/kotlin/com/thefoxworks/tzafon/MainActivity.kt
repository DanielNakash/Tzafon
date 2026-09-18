package com.thefoxworks.tzafon

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thefoxworks.tzafon.domain.model.TzafonUser
import kotlinx.coroutines.flow.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.about.AboutScreen
import com.thefoxworks.tzafon.ui.alltasks.AllTasksScreen
import com.thefoxworks.tzafon.ui.alltasks.AllTasksViewModel
import com.thefoxworks.tzafon.ui.backlog.BacklogScreen
import com.thefoxworks.tzafon.ui.backlog.BacklogViewModel
import com.thefoxworks.tzafon.ui.directions.DirectionsScreen
import com.thefoxworks.tzafon.ui.directions.DirectionsViewModel
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.habits.HabitsScreen
import com.thefoxworks.tzafon.ui.habits.HabitsViewModel
import com.thefoxworks.tzafon.ui.journey.JourneyScreen
import com.thefoxworks.tzafon.ui.journey.JourneyViewModel
import com.thefoxworks.tzafon.ui.nav.DenBottomNav
import com.thefoxworks.tzafon.ui.nav.Tab
import com.thefoxworks.tzafon.ui.planning.PlanningScreen
import com.thefoxworks.tzafon.ui.planning.PlanningViewModel
import com.thefoxworks.tzafon.ui.review.ReviewScreen
import com.thefoxworks.tzafon.ui.review.ReviewViewModel
import com.thefoxworks.tzafon.ui.settings.SettingsScreen
import com.thefoxworks.tzafon.ui.theme.LocalPalette
import com.thefoxworks.tzafon.ui.theme.paletteFor
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
import com.thefoxworks.tzafon.ui.today.TodayScreen
import com.thefoxworks.tzafon.ui.today.TodayViewModel
import com.thefoxworks.tzafon.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TzafonApp).container
        setContent {
            // FR-DESIGN-4.4 — resolve the persisted palette at the composition root
            // and provide it via LocalPalette; a change recomposes the whole tree
            // on the next frame (no restart). Defaults to Den before first emission.
            val paletteName by container.settings.palette.collectAsStateWithLifecycle(initialValue = "den")
            val palette = paletteFor(paletteName)
            CompositionLocalProvider(LocalPalette provides palette) {
                TzafonTheme(palette = palette) {
                    TzafonNavHost(container)
                }
            }
        }
    }
}

/** Simple factory for the manual container (PLAN §3 — no Hilt). */
class VmFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AllTasksViewModel::class.java ->
            AllTasksViewModel(container.taskRepository, container.sessionHorizonDays, container.habitRepository, container.goalRepository, container.settings, container.chimePlayer) as T
        TodayViewModel::class.java ->
            TodayViewModel(container.taskRepository, container.settings, container.habitRepository, container.goalRepository, container.reviewRepository, container.chimePlayer) as T
        ReviewViewModel::class.java ->
            ReviewViewModel(container.reviewRepository, container.taskRepository, container.habitRepository, container.goalRepository, container.settings) as T
        PlanningViewModel::class.java ->
            PlanningViewModel(container.taskRepository, container.settings, container.sessionHorizonDays, container.habitRepository, container.goalRepository, container.chimePlayer) as T
        BacklogViewModel::class.java ->
            BacklogViewModel(container.taskRepository) as T
        HabitsViewModel::class.java ->
            HabitsViewModel(container.habitRepository, container.settings, container.goalRepository, container.themeRepository) as T
        DirectionsViewModel::class.java ->
            DirectionsViewModel(container.themeRepository, container.goalRepository, container.habitRepository, container.taskRepository) as T
        JourneyViewModel::class.java ->
            JourneyViewModel(container.taskRepository, container.goalRepository, container.habitRepository, container.themeRepository, container.reviewRepository, container.settings) as T
        else -> throw IllegalArgumentException("Unknown VM $modelClass")
    }
}

/** FR-NAV-6.1 / FR-NAV-9 — routes that are reference views (open on top of a tab), not tabs. */
internal val REFERENCE_ROUTES = setOf("alltasks", "backlog")

/** FR-NAV-9 — is the current top a reference view (opened on top of a tab)? */
internal fun isReferenceView(currentRoute: String?): Boolean = currentRoute in REFERENCE_ROUTES

/**
 * FR-NAV-11.2 / FR-NAV-11.8 — the popUpTo target when opening a reference view.
 * Walk from the top of the current back-stack down until we find a route that is
 * NOT a reference view — that's the tab base we want the new reference view to
 * sit directly on top of. Falls back to Today's route as a last-resort anchor
 * (the always-present base tab in the graph). This keeps repeated lateral
 * `alltasks ↔ backlog` hops from accumulating: each hop pops back to the entry
 * tab first, then pushes exactly one reference view on top of it.
 */
internal fun refPopUpTarget(currentStack: List<String>, todayRoute: String): String {
    for (route in currentStack.asReversed()) {
        if (route !in REFERENCE_ROUTES) return route
    }
    return todayRoute
}

/**
 * FR-NAV-11 — pure simulation of `goTab` / `goRef` against a list-based
 * back-stack model, mirroring the invariants the real `NavController` calls
 * enforce. Kept in `main/` so `NavReferenceLeakTest` can pin the invariant
 * without a Compose / instrumented runtime. Not called from production code.
 *
 * Invariants pinned:
 *  - `goTab(tab)` pops every reference view above the current tab base and
 *    leaves the tab's route as the current top — FR-NAV-11.1.
 *  - `goRef(route)` first pops back to the nearest non-reference route, then
 *    pushes the reference view — FR-NAV-11.2 / FR-NAV-11.8, so lateral hops
 *    don't accumulate on the stack.
 *  - A `NavController.saveState/restoreState` bucket can never contain a
 *    reference view when a tab tap fires — FR-NAV-11.3 falls out because
 *    `goTab` clears reference views before the switch.
 */
internal sealed interface NavAction {
    data class GoTab(val route: String) : NavAction
    data class GoRef(val route: String) : NavAction
}

internal fun simulateNav(
    startStack: List<String>,
    actions: List<NavAction>,
    todayRoute: String = "today",
): List<String> {
    var stack = startStack.toMutableList()
    for (action in actions) {
        when (action) {
            is NavAction.GoTab -> {
                // FR-NAV-11.1 — pop every reference view above the target tab base.
                while (stack.isNotEmpty() && isReferenceView(stack.last())) {
                    stack.removeAt(stack.lastIndex)
                }
                // popUpTo(TODAY){saveState=true} + restoreState=true in real code — the
                // model normalizes to `[…, tab]` (no reference view above it).
                val idx = stack.indexOf(action.route)
                if (idx >= 0) {
                    // Restore: bring the tab to the top.
                    stack = stack.subList(0, idx + 1).toMutableList()
                } else {
                    stack.add(action.route)
                }
            }
            is NavAction.GoRef -> {
                // FR-NAV-11.2 / FR-NAV-11.8 — pop back to the nearest tab base, then push.
                val target = refPopUpTarget(stack, todayRoute)
                val idx = stack.indexOf(target)
                if (idx >= 0) stack = stack.subList(0, idx + 1).toMutableList()
                stack.add(action.route)
            }
        }
    }
    return stack.toList()
}

/**
 * FR-AUTH-1.10 — auth state has three phases: not-yet-emitted (loading),
 * signed-out, signed-in. Wrapping the Firebase flow so the first frame can
 * render a bare Den-themed splash instead of flashing Welcome to a user
 * whose persisted session is still being read.
 */
internal sealed interface AuthGate {
    data object Loading : AuthGate
    data class Ready(val user: TzafonUser?) : AuthGate
}

/**
 * FR-AUTH-1.1 — pure gate logic (extracted so the JUnit nav-layer test can
 * exercise it without the Compose runtime): a signed-in identity opens on
 * Today, a null identity opens on Welcome. `welcomeSeen` is retired — auth
 * state alone decides.
 */
internal fun authStartDestination(user: TzafonUser?): String =
    if (user != null) Tab.TODAY.route else "welcome"

@Composable
fun TzafonNavHost(container: AppContainer) {
    val gate by remember(container.authRepository) {
        container.authRepository.authState.map<TzafonUser?, AuthGate> { AuthGate.Ready(it) }
    }.collectAsStateWithLifecycle(initialValue = AuthGate.Loading)

    when (val g = gate) {
        AuthGate.Loading -> {
            // FR-AUTH-1.10 [DECISION] (a) — bare Den background until authState resolves.
            Box(Modifier.fillMaxSize().background(TzafonThemeBackground()))
        }
        is AuthGate.Ready -> TzafonMainNav(container, initialUser = g.user)
    }
}

/** Read the current theme's background so the splash matches everything else. */
@Composable
private fun TzafonThemeBackground(): androidx.compose.ui.graphics.Color =
    com.thefoxworks.tzafon.ui.theme.Tz.colors.bg

@Composable
private fun TzafonMainNav(container: AppContainer, initialUser: TzafonUser?) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val today = Dates.todayIso()

    // FR-AUTH-1.1 — auth state is the sole cold-start gate: signed-in users
    // land straight on Today, signed-out users see Welcome. Frozen for this
    // composition; live `authState` transitions are handled inline (WelcomeScreen
    // observes it to nav out on sign-in; SettingsScreen calls `onSignedOut` to
    // nav back on sign-out) so the graph itself doesn't rebuild mid-session.
    val start = remember { authStartDestination(initialUser) }

    fun goTab(tab: Tab) {
        // FR-NAV-11.1 — pop EVERY reference view above the target tab base before
        // the switch (the v2.5.0 `if` guard was only enough for the single-hop
        // case). Combined with `goRef`'s pop-to-tab (FR-NAV-11.2), the
        // `saveState`/`restoreState` bucket then can only ever contain the tab's
        // own within-tab state (scroll, expanded rows) — never a leaked
        // reference view (FR-NAV-11.3). A subsequent tab tap always lands on the
        // tab's base, regardless of how many lateral `alltasks ↔ backlog` hops
        // occurred first.
        while (isReferenceView(nav.currentBackStackEntry?.destination?.route)) {
            nav.popBackStack()
        }
        nav.navigate(tab.route) {
            popUpTo(Tab.TODAY.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // FR-NAV-11.2 / FR-NAV-11.8 — open a reference view directly on top of the
    // nearest tab base, not on top of another reference view. So `Habits →
    // goRef(alltasks) → goRef(backlog)` yields `[Habits, backlog]` instead of
    // `[Habits, alltasks, backlog]`, and repeated lateral hops cannot
    // accumulate. The popUpTo target is computed off the live back-stack so it
    // handles the "current top is itself a reference view" case correctly.
    fun goRef(route: String) {
        val currentRoutes = nav.currentBackStack.value.mapNotNull { it.destination.route }
        val target = refPopUpTarget(currentRoutes, Tab.TODAY.route)
        nav.navigate(route) {
            popUpTo(target) { inclusive = false; saveState = false }
            launchSingleTop = true
        }
    }

    // presetToday: FR-TODAY-7 — a NEW task added from the Today view defaults its
    // toDoDate to today, whether via inline quick-add or the full editor (expand).
    // presetBacklog: FR-BACKLOG-5.2 — expand-to-full-form from the Backlog view
    // opens the editor with state = BACKLOG and toDoDate = null.
    // presetCue: FR-CAPTURE-3.6 — an HH:mm parsed off a quick-add title, carried
    // into the editor's cue slot so EXPAND shows the outcome an inline save would
    // have produced, adjustable before saving.
    fun openEditor(
        taskId: String? = null,
        title: String? = null,
        presetToday: Boolean = false,
        presetBacklog: Boolean = false,
        presetCue: String? = null,
    ) {
        val params = buildList {
            if (taskId != null) add("taskId=$taskId")
            if (!title.isNullOrBlank()) add("title=${Uri.encode(title)}")
            if (presetToday) add("presetToday=true")
            if (presetBacklog) add("presetBacklog=true")
            if (presetCue != null) add("presetCue=${Uri.encode(presetCue)}")
        }
        val route = if (params.isEmpty()) "editor" else "editor?" + params.joinToString("&")
        nav.navigate(route)
    }

    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val activeTab = Tab.entries.firstOrNull { it.route == currentRoute }
    // FR-NAV-6.1 — reference views (All Tasks, Backlog) show the bar as a
    // wayfinder with no tab active, so the primary destinations stay one tap
    // away without pretending the reference view is a primary tab.
    val showBottomNav = activeTab != null || currentRoute in REFERENCE_ROUTES

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("welcome") {
                WelcomeScreen(
                    authRepository = container.authRepository,
                    onSignedIn = {
                        // FR-AUTH-1.3 — clear Welcome from the back stack so
                        // Android back from Today does not return here.
                        nav.navigate(Tab.TODAY.route) { popUpTo("welcome") { inclusive = true } }
                    },
                )
            }

            // ── the five tabs (FR-NAV-1; Today default, FR-NAV-2) ──
            composable(Tab.TODAY.route) {
                val vm: TodayViewModel = viewModel(factory = VmFactory(container))
                TodayScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    // FR-CAPTURE-3.6 — EXPAND carries the parsed cue; the date rule is
                    // Today's own (FR-TODAY-7) and is unchanged by the parse.
                    onExpandAdd = { title, cueTime ->
                        openEditor(title = title, presetToday = true, presetCue = cueTime)
                    },
                    onOpenPlanning = { goTab(Tab.PLANNING) },
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                    onOpenReview = { nav.navigate("review") },
                )
            }

            composable(Tab.PLANNING.route) {
                val vm: PlanningViewModel = viewModel(factory = VmFactory(container))
                PlanningScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    // FR-PLAN-7.4 — a parsed time opens the editor pre-set to today with
                    // the cue filled; a bare capture still opens undated (FR-CAPTURE-2).
                    onExpandAdd = { title, cueTime ->
                        openEditor(title = title, presetToday = cueTime != null, presetCue = cueTime)
                    },
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }

            composable(Tab.HABITS.route) {
                val vm: HabitsViewModel = viewModel(factory = VmFactory(container))
                HabitsScreen(
                    vm = vm,
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }
            composable(Tab.DIRECTIONS.route) {
                val vm: DirectionsViewModel = viewModel(factory = VmFactory(container))
                DirectionsScreen(
                    vm = vm,
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }
            composable(Tab.JOURNEY.route) {
                val vm: JourneyViewModel = viewModel(factory = VmFactory(container))
                JourneyScreen(
                    vm = vm,
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }

            // ── reference views via the menu (FR-NAV-1) ──
            composable("alltasks") {
                val vm: AllTasksViewModel = viewModel(factory = VmFactory(container))
                AllTasksScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    onAdd = { openEditor() },
                    onOpenBacklog = { goRef("backlog") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }

            composable("backlog") {
                val vm: BacklogViewModel = viewModel(factory = VmFactory(container))
                BacklogScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    // FR-BACKLOG-5.2 — the expand-to-full-form route from Backlog
                    // opens the editor pre-set to Backlog state / undated; the state
                    // and date preset are untouched by a parsed cue (FR-CAPTURE-3.8).
                    onExpandAdd = { title, cueTime ->
                        openEditor(title = title, presetBacklog = true, presetCue = cueTime)
                    },
                    onOpenAllTasks = { goRef("alltasks") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenAbout = { nav.navigate("about") },
                )
            }

            composable("review") {
                val vm: ReviewViewModel = viewModel(factory = VmFactory(container))
                ReviewScreen(vm = vm, onClose = { nav.popBackStack() })
            }

            composable("settings") {
                SettingsScreen(
                    settings = container.settings,
                    auth = container.authRepository,
                    exporter = container.dataExporter,
                    importer = container.dataImporter,
                    onClose = { nav.popBackStack() },
                    onSignedOut = {
                        // FR-AUTH-1.4 — sign-out returns to Welcome and clears
                        // the entire main-tab back stack so back from Welcome
                        // exits the app rather than returning to a signed-in tab.
                        nav.navigate("welcome") {
                            popUpTo(nav.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable("about") {
                AboutScreen(onClose = { nav.popBackStack() })
            }

            composable(
                route = "editor?taskId={taskId}&title={title}&presetToday={presetToday}&presetBacklog={presetBacklog}&presetCue={presetCue}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("presetToday") { type = NavType.BoolType; defaultValue = false },
                    navArgument("presetBacklog") { type = NavType.BoolType; defaultValue = false },
                    navArgument("presetCue") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStack ->
                val taskId = backStack.arguments?.getString("taskId")
                val presetTitle = backStack.arguments?.getString("title")
                val presetToday = backStack.arguments?.getBoolean("presetToday") == true
                val presetBacklog = backStack.arguments?.getBoolean("presetBacklog") == true
                val presetCue = backStack.arguments?.getString("presetCue")
                val repo = container.taskRepository
                // hydrate the draft synchronously off the DB (small row; simple M0 path)
                val initial: TaskDraft? = taskId?.let {
                    runBlocking {
                        val t = repo.getTask(it) ?: return@runBlocking null
                        val s = t.seriesId?.let { sid -> repo.getSeries(sid) }
                        TaskDraft(
                            id = t.id,
                            title = t.title,
                            description = t.description,
                            toDoDate = t.toDoDate,
                            dueDate = t.dueDate,
                            state = t.state,
                            recurrence = s?.let { series ->
                                RecurrenceDraft(rule = series.rule, endDate = series.endDate, dueMode = series.dueMode, dueRule = series.dueRule)
                            },
                            seriesId = t.seriesId,
                            occurrenceDate = t.occurrenceDate,
                            cue = t.cue,
                            themeId = t.themeId,
                            habitId = t.habitId,
                            goalIds = t.goalIds,
                            commitment = t.commitment,
                        )
                    }
                } ?: run {
                    // New task. From Today (presetToday) it defaults toDoDate = today so it
                    // lands in the Today list; from Backlog (presetBacklog, FR-BACKLOG-5.2)
                    // it lands in Backlog state / undated; elsewhere it stays OPEN/undated
                    // (FR-CAPTURE-2). presetCue (FR-CAPTURE-3.6) fills the cue slot with
                    // the time parsed off the quick-add title, whatever the surface.
                    if (presetTitle != null || presetToday || presetBacklog || presetCue != null) {
                        TaskDraft(
                            id = null,
                            title = presetTitle ?: "",
                            state = if (presetBacklog)
                                com.thefoxworks.tzafon.domain.model.TaskState.BACKLOG
                            else com.thefoxworks.tzafon.domain.model.TaskState.OPEN,
                            toDoDate = if (presetToday) today else null,
                            cue = com.thefoxworks.tzafon.domain.action.QuickAddParse.cueFor(presetCue),
                        )
                    } else null
                }
                val habits by container.habitRepository.observeHabits()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                val editorGoals by container.goalRepository.observeGoals()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                TaskEditorScreen(
                    initial = initial,
                    today = today,
                    onSave = { draft, editScope ->
                        scope.launch {
                            container.taskRepository.saveDraft(draft, editScope, today)
                        }
                        nav.popBackStack()
                    },
                    onDelete = { id, editScope ->
                        scope.launch { container.taskRepository.deleteTask(id, editScope) }
                        nav.popBackStack()
                    },
                    onSetState = { id, target ->
                        scope.launch { container.taskRepository.setState(id, target, today) }
                    },
                    onClose = { nav.popBackStack() },
                    habits = habits,
                    goals = editorGoals,
                )
            }
        }

        if (showBottomNav) {
            DenBottomNav(
                active = activeTab,
                onSelect = { goTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
