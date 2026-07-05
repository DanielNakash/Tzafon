package com.thefoxworks.tzafon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.RecurrenceDraft
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.ui.alltasks.AllTasksScreen
import com.thefoxworks.tzafon.ui.alltasks.AllTasksViewModel
import com.thefoxworks.tzafon.ui.editor.TaskEditorScreen
import com.thefoxworks.tzafon.ui.theme.TzafonTheme
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
        AllTasksViewModel::class.java -> AllTasksViewModel(container.taskRepository) as T
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

    NavHost(
        navController = nav,
        startDestination = if (welcomeSeen == true) "alltasks" else "welcome",
    ) {
        composable("welcome") {
            WelcomeScreen(onStart = {
                scope.launch { container.settings.setWelcomeSeen() }
                nav.navigate("alltasks") { popUpTo("welcome") { inclusive = true } }
            })
        }

        composable("alltasks") {
            val vm: AllTasksViewModel = viewModel(factory = VmFactory(container))
            AllTasksScreen(
                vm = vm,
                onOpenTask = { id -> nav.navigate("editor?taskId=$id") },
                onAdd = { nav.navigate("editor") },
            )
        }

        composable(
            route = "editor?taskId={taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStack ->
            val taskId = backStack.arguments?.getString("taskId")
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
            }
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
            )
        }
    }
}
