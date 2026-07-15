package com.thefoxworks.tzafon.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.model.HabitKind
import com.thefoxworks.tzafon.domain.model.HabitLog
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.a
import java.time.Instant
import java.time.ZoneId

/**
 * FR-HAB-11 — the rolling last-7-days rhythm surface that replaces the
 * calendar-week `WeekDots` / `QuantBars` layer on habit cards. Reads the
 * same `HabitCardState.logs` the card already has; no repository change.
 *
 * The collapsed row uses `interactive = false` (a factual read-out; taps
 * do not log per `FR-HAB-11.6`); the expanded row uses `interactive =
 * true` inside a horizontal scroller (`FR-HAB-11.5`) with initial
 * position pinned to the rightmost / today end.
 *
 * RTL (`FR-HAB-11.10`, `FR-DESIGN-3.5`) is honoured for free by `Row`:
 * cells are laid out in `[today − 6, …, today]` order, so LTR puts today
 * on the right, RTL puts today on the left. The horizontalScroll flips
 * with LayoutDirection for the same reason.
 */
@Composable
internal fun RhythmStrip(
    today: String,
    startedAt: Long,
    logs: List<HabitLog>,
    kind: HabitKind,
    target: Double,
    accent: Color,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onTapDay: (String) -> Unit = {},
) {
    val startIso = remember(startedAt) { epochToIsoOrNull(startedAt) }
    val cells = remember(today, logs, kind, target, startedAt, interactive) {
        val daysBack = if (interactive) EXPANDED_DAYS - 1 else COLLAPSED_DAYS - 1
        (daysBack downTo 0).map { offset ->
            val date = Dates.addDays(today, -offset.toLong())
            date to rhythmCellState(
                date = date,
                today = today,
                startedAtIso = startIso,
                logs = logs,
                kind = kind,
                target = target,
            )
        }
    }

    if (interactive) {
        val scroll = rememberScrollState()
        // FR-HAB-11.5 — pin the initial scroll to the rightmost (today) end.
        LaunchedEffect(cells.size) { scroll.scrollTo(scroll.maxValue) }
        Row(
            modifier = modifier.horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) {
            cells.forEachIndexed { i, (date, state) ->
                RhythmCell(
                    date = date,
                    state = state,
                    isToday = date == today,
                    accent = accent,
                    interactive = true,
                    onTap = { onTapDay(date) },
                )
                // Insert a subtle week separator between cells so panning shows
                // where each 7-day bucket ends (FR-HAB-11.5).
                val posFromToday = cells.size - 1 - i
                val nextPosFromToday = posFromToday - 1
                val nextIsNewWeek = nextPosFromToday >= 0 && (nextPosFromToday + 1) % 7 == 0
                if (i < cells.size - 1 && nextIsNewWeek) {
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(CELL_SIZE + 4.dp)
                            .background(Tz.colors.line2),
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
        ) {
            cells.forEach { (date, state) ->
                RhythmCell(
                    date = date,
                    state = state,
                    isToday = date == today,
                    accent = accent,
                    interactive = false,
                )
            }
        }
    }
}

@Composable
private fun RhythmCell(
    date: String,
    state: RhythmCellState,
    isToday: Boolean,
    accent: Color,
    interactive: Boolean,
    onTap: () -> Unit = {},
) {
    val cellShape = RoundedCornerShape(6.dp)
    val todayRing = if (isToday) Modifier.border(1.5.dp, accent, cellShape) else Modifier

    when (state) {
        RhythmCellState.PreStart -> {
            // FR-HAB-11.5 — pre-startedAt days: thin muted line, no interaction.
            Column(
                Modifier.size(CELL_SIZE).then(todayRing),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(CELL_SIZE - 12.dp)
                        .height(1.dp)
                        .background(Tz.colors.faint.a(0.35f)),
                )
            }
        }
        is RhythmCellState.Freq -> {
            val bg = if (state.done) accent else Tz.colors.ink.a(0.08f)
            val cellMod = Modifier
                .size(CELL_SIZE)
                .clip(cellShape)
                .background(bg)
                .then(todayRing)
                .let {
                    if (interactive) {
                        val label = if (state.done) "Un-log ${date}" else "Log ${date}"
                        it
                            .semantics { contentDescription = label }
                            .pressable(label = label, role = Role.Button, onClick = onTap)
                    } else it
                }
            Box(cellMod)
        }
        is RhythmCellState.Quant -> {
            val fillH = (CELL_SIZE.value * state.fillFraction).coerceAtLeast(0f)
            val cellMod = Modifier
                .size(CELL_SIZE)
                .then(todayRing)
                .let {
                    if (interactive) {
                        val label = "Log amount for ${date}"
                        it
                            .semantics { contentDescription = label }
                            .pressable(label = label, role = Role.Button, onClick = onTap)
                    } else it
                }
            Box(cellMod, contentAlignment = Alignment.BottomCenter) {
                if (state.fillFraction <= 0f) {
                    // Zero-amount day: mirror QuantBars's low-line placeholder.
                    Box(
                        Modifier
                            .width(CELL_SIZE - 8.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Tz.colors.ink.a(0.1f)),
                    )
                } else {
                    Box(
                        Modifier
                            .width(CELL_SIZE - 8.dp)
                            .height(fillH.dp.coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(2.dp))
                            .background(accent),
                    )
                }
            }
        }
    }
}

/** Pure render-rule state for a single strip cell — unit-testable. */
internal sealed interface RhythmCellState {
    /** Day is before `startedAt` — thin muted line, no interaction. */
    data object PreStart : RhythmCellState
    /** Frequency habit — done ⇒ filled accent, else empty. */
    data class Freq(val done: Boolean) : RhythmCellState
    /** Quantitative habit — fill fraction 0..1 over `target * 1.15`. */
    data class Quant(val fillFraction: Float) : RhythmCellState
}

/**
 * FR-HAB-11 — the per-day render rule, extracted for unit tests. Mirrors
 * `WeekDots` (filled iff `logs.any { it.date == d && it.done }`) and
 * `QuantBars` (bar proportional to `amount / (target * 1.15)`).
 */
internal fun rhythmCellState(
    date: String,
    today: String,
    startedAtIso: String?,
    logs: List<HabitLog>,
    kind: HabitKind,
    target: Double,
): RhythmCellState {
    if (startedAtIso != null && date < startedAtIso) return RhythmCellState.PreStart
    return when (kind) {
        HabitKind.FREQUENCY -> RhythmCellState.Freq(
            done = logs.any { it.date == date && it.done },
        )
        HabitKind.QUANTITATIVE -> {
            val amount = logs.firstOrNull { it.date == date }?.amount ?: 0.0
            val denom = (target * 1.15).coerceAtLeast(0.0001)
            val frac = (amount / denom).coerceIn(0.0, 1.0).toFloat()
            RhythmCellState.Quant(fillFraction = frac)
        }
    }
}

private fun epochToIsoOrNull(epochMs: Long): String? {
    if (epochMs == 0L) return null
    val d = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return Dates.iso(d)
}

private val CELL_SIZE = 22.dp
private val CELL_GAP = 6.dp
private const val COLLAPSED_DAYS = 7
private const val EXPANDED_DAYS = 28
