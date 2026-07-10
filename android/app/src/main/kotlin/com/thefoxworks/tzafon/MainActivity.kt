package com.thefoxworks.tzafon

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            TzafonTheme {
                TzafonNavHost(container)
            }
        }
    }
}

/** Simple factory for the manual container (PLAN §3 — no Hilt). */
class VmFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AllTasksViewModel::class.java ->
            AllTasksViewModel(container.taskRepository, container.sessionHorizonDays, container.habitRepository, container.goalRepository) as T
        TodayViewModel::class.java ->
            TodayViewModel(container.taskRepository, container.settings, container.habitRepository, container.goalRepository, container.reviewRepository) as T
        ReviewViewModel::class.java ->
            ReviewViewModel(container.reviewRepository, container.taskRepository, container.habitRepository, container.goalRepository, container.settings) as T
        PlanningViewModel::class.java ->
            PlanningViewModel(container.taskRepository, container.settings, container.sessionHorizonDays, container.habitRepository, container.goalRepository) as T
        BacklogViewModel::class.java ->
            BacklogViewModel(container.taskRepository) as T
        HabitsViewModel::class.java ->
            HabitsViewModel(container.habitRepository, container.settings, container.goalRepository) as T
        DirectionsViewModel::class.java ->
            DirectionsViewModel(container.themeRepository, container.goalRepository, container.habitRepository, container.taskRepository) as T
        JourneyViewModel::class.java ->
            JourneyViewModel(container.taskRepository, container.goalRepository, container.habitRepository, container.themeRepository, container.reviewRepository, container.settings) as T
        else -> throw IllegalArgumentException("Unknown VM $modelClass")
    }
}

@Composable
fun TzafonNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val welcomeSeen by container.settings.welcomeSeen.collectAsStateWithLifecycle(initialValue = null as Boolean?)
    val today = Dates.todayIso()

    if (welcomeSeen == null) return // waiting on DataStore's first emission

    // frozen for the composition so the DataStore flip can't rebuild the graph
    val start = remember { if (welcomeSeen == true) Tab.TODAY.route else "welcome" }

    fun goTab(tab: Tab) {
        nav.navigate(tab.route) {
            popUpTo(Tab.TODAY.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // presetToday: FR-TODAY-7 — a NEW task added from the Today view defaults its
    // toDoDate to today, whether via inline quick-add or the full editor (expand).
    fun openEditor(taskId: String? = null, title: String? = null, presetToday: Boolean = false) {
        val params = buildList {
            if (taskId != null) add("taskId=$taskId")
            if (!title.isNullOrBlank()) add("title=${Uri.encode(title)}")
            if (presetToday) add("presetToday=true")
        }
        val route = if (params.isEmpty()) "editor" else "editor?" + params.joinToString("&")
        nav.navigate(route)
    }

    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val activeTab = Tab.entries.firstOrNull { it.route == currentRoute }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable("welcome") {
                WelcomeScreen(onStart = {
                    scope.launch { container.settings.setWelcomeSeen() }
                    nav.navigate(Tab.TODAY.route) { popUpTo("welcome") { inclusive = true } }
                })
            }

            // ── the five tabs (FR-NAV-1; Today default, FR-NAV-2) ──
            composable(Tab.TODAY.route) {
                val vm: TodayViewModel = viewModel(factory = VmFactory(container))
                TodayScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    onExpandAdd = { title -> openEditor(title = title, presetToday = true) },
                    onOpenPlanning = { goTab(Tab.PLANNING) },
                    onOpenAllTasks = { nav.navigate("alltasks") },
                    onOpenBacklog = { nav.navigate("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                    onOpenReview = { nav.navigate("review") },
                )
            }

            composable(Tab.PLANNING.route) {
                val vm: PlanningViewModel = viewModel(factory = VmFactory(container))
                PlanningScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    onExpandAdd = { title -> openEditor(title = title) },
                    onOpenAllTasks = { nav.navigate("alltasks") },
                    onOpenBacklog = { nav.navigate("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }

            composable(Tab.HABITS.route) {
                val vm: HabitsViewModel = viewModel(factory = VmFactory(container))
                HabitsScreen(
                    vm = vm,
                    onOpenAllTasks = { nav.navigate("alltasks") },
                    onOpenBacklog = { nav.navigate("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable(Tab.DIRECTIONS.route) {
                val vm: DirectionsViewModel = viewModel(factory = VmFactory(container))
                DirectionsScreen(
                    vm = vm,
                    onOpenAllTasks = { nav.navigate("alltasks") },
                    onOpenBacklog = { nav.navigate("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable(Tab.JOURNEY.route) {
                val vm: JourneyViewModel = viewModel(factory = VmFactory(container))
                JourneyScreen(
                    vm = vm,
                    onOpenAllTasks = { nav.navigate("alltasks") },
                    onOpenBacklog = { nav.navigate("backlog") },
                    onOpenSettings = { nav.navigate("settings") },
                )
            }

            // ── reference views via the menu (FR-NAV-1) ──
            composable("alltasks") {
                val vm: AllTasksViewModel = viewModel(factory = VmFactory(container))
                AllTasksScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    onAdd = { openEditor() },
                    onOpenBacklog = { nav.navigate("backlog") { launchSingleTop = true } },
                )
            }

            composable("backlog") {
                val vm: BacklogViewModel = viewModel(factory = VmFactory(container))
                BacklogScreen(
                    vm = vm,
                    onOpenTask = { id -> openEditor(taskId = id) },
                    onExpandAdd = { title -> openEditor(title = title) },
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
                    onClose = { nav.popBackStack() },
                )
            }

            composable(
                route = "editor?taskId={taskId}&title={title}&presetToday={presetToday}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("presetToday") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { backStack ->
                val taskId = backStack.arguments?.getString("taskId")
                val presetTitle = backStack.arguments?.getString("title")
                val presetToday = backStack.arguments?.getBoolean("presetToday") == true
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
                    // lands in the Today list; elsewhere it stays undated (FR-CAPTURE-2).
                    if (presetTitle != null || presetToday) {
                        TaskDraft(id = null, title = presetTitle ?: "", toDoDate = if (presetToday) today else null)
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

        if (activeTab != null) {
            DenBottomNav(
                active = activeTab,
                onSelect = { goTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
