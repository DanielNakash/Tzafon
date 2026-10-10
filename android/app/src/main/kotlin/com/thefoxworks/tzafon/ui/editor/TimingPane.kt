package com.thefoxworks.tzafon.ui.editor

import androidx.compose.runtime.Composable
import com.thefoxworks.tzafon.domain.editor.Armed
import com.thefoxworks.tzafon.domain.editor.CardBody
import com.thefoxworks.tzafon.domain.model.TaskDraft
import com.thefoxworks.tzafon.domain.recurrence.SeriesSpec
import java.time.YearMonth

/**
 * v2.13.0 — the Timing pane: the shared calendar for the to-do date and the
 * deadline, then the repeat card. Everything edits the draft. Emits into the
 * editor's scrolling column.
 */
@Composable
internal fun TimingPane(
    draft: TaskDraft,
    today: String,
    armed: Armed,
    month: YearMonth,
    cardBody: CardBody,
    weekStart: String,
    series: SeriesSpec?,
    onArm: (Armed) -> Unit,
    onMonth: (YearMonth) -> Unit,
    onCardBody: (CardBody) -> Unit,
    onEdit: ((TaskDraft) -> TaskDraft) -> Unit,
) {
    EdSection("Dates · one calendar, two fields")
    TimingCalendar(draft, today, armed, month, weekStart, onArm, onMonth, onEdit)

    EdSection("Repeat")
    TimingRepeatCard(draft, today, armed, cardBody, weekStart, series, onArm, onCardBody, onEdit)
}
