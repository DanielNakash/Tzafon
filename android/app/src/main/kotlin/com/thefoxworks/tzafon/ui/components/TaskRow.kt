package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.domain.model.Task
import com.thefoxworks.tzafon.domain.model.TaskState
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.a

/** Which due indicator (if any) a task shows today — v1.1.0 dueIndicator. */
enum class DueIndicator { NONE, DEADLINE, TODAY }

fun dueIndicator(task: Task, today: String): DueIndicator = when {
    task.dueDate == null -> DueIndicator.NONE
    task.toDoDate != null && task.dueDate == task.toDoDate -> DueIndicator.DEADLINE
    task.dueDate == today && task.toDoDate != task.dueDate -> DueIndicator.TODAY
    else -> DueIndicator.NONE
}

/**
 * The reusable task row (tz-ui.jsx TaskRow): state checkbox, title
 * (struck for done/closed), meta chips, optional chevron, hairline below.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    today: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    recurSummary: String? = null,
    servesLabel: String? = null,
    servesAccent: androidx.compose.ui.graphics.Color = Den.muted,
    habitLabel: String? = null,
    last: Boolean = false,
    chevron: Boolean = true,
) {
    val struck = task.state == TaskState.DONE || task.state == TaskState.CLOSED
    val ind = dueIndicator(task, today)

    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = onOpen,
                onLongClick = onLongPress,
            )
            .alpha(if (task.state == TaskState.DONE) 0.62f else 1f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 13.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(Modifier.padding(top = 1.dp)) { TaskCheckbox(task.state, onClick = onToggle) }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        task.title,
                        style = DenType.rowTitle,
                        color = if (struck) Den.muted else Den.ink,
                        textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    when (ind) {
                        DueIndicator.DEADLINE -> DeadlinePill()
                        DueIndicator.TODAY -> DuePill(today = true)
                        DueIndicator.NONE -> {}
                    }
                    if (task.state != TaskState.OPEN) StateTag(task.state)
                }
                val hasMeta = servesLabel != null || habitLabel != null || task.cue != null || recurSummary != null
                if (hasMeta) {
                    Row(
                        Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (servesLabel != null) Serves(servesLabel, servesAccent)
                        if (habitLabel != null) Chip(habitLabel, icon = { TzIcons.Repeat(12.dp, Den.faint) })
                        task.cue?.let { CueChip(it.label) }
                        if (recurSummary != null) Chip(recurSummary, icon = { TzIcons.Repeat(12.dp, Den.faint) })
                    }
                }
            }
            if (chevron) {
                Box(Modifier.padding(top = 3.dp)) { TzIcons.Chevron(17.dp, Den.ink.a(0.26f)) }
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Den.line))
    }
}
