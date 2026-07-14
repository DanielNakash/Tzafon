package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.dates.Dates
import com.thefoxworks.tzafon.domain.recurrence.IntervalUnit
import com.thefoxworks.tzafon.domain.recurrence.MonthMode
import com.thefoxworks.tzafon.domain.recurrence.Pattern
import com.thefoxworks.tzafon.domain.recurrence.Rule
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType

/** Field wrapper with a mono caps label (RecurrenceFields.jsx Field). */
@Composable
fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(bottom = 14.dp)) {
        SectionLabel(label, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

/** Rust-filled segmented control (RecurrenceFields.jsx Segmented). */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    value: T,
    onSelect: (T) -> Unit,
    small: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Tz.colors.surfaceAlt)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (v, label) ->
            val on = v == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Tz.colors.rust else Color.Transparent)
                    .pressable { onSelect(v) }
                    .padding(vertical = if (small) 8.dp else 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = TextStyle(
                        fontFamily = DenType.body,
                        fontSize = if (small) 13.sp else 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (on) Color.White else Tz.colors.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** – n + stepper (RecurrenceFields.jsx Stepper). */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 1, max: Int = 99, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Tz.colors.card)
            .border(1.dp, Tz.colors.line, RoundedCornerShape(11.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).pressable { if (value > min) onChange(value - 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("–", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold), color = if (value <= min) Tz.colors.faint else Tz.colors.rust)
        }
        Text(
            "$value",
            style = TextStyle(fontFamily = DenType.mono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = Tz.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(30.dp),
        )
        Box(
            Modifier.size(38.dp).pressable { if (value < max) onChange(value + 1) },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold), color = if (value >= max) Tz.colors.faint else Tz.colors.rust)
        }
    }
}

/**
 * The recurrence pattern builder (RecurrenceFields.jsx): pattern segmented +
 * per-pattern fields. Pure controlled component over a Rule.
 */
@Composable
fun RecurrenceFields(rule: Rule, onChange: (Rule) -> Unit) {
    Column {
        Field("Pattern") {
            Segmented(
                options = listOf(Pattern.INTERVAL to "Interval", Pattern.WEEKDAY to "Weekdays", Pattern.MONTHDAY to "Monthly"),
                value = rule.pattern,
                onSelect = { onChange(rule.copy(pattern = it)) },
            )
        }

        when (rule.pattern) {
            Pattern.INTERVAL -> {
                Field("Repeat every") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Stepper(rule.interval, { onChange(rule.copy(interval = it)) })
                        Box(Modifier.weight(1f)) {
                            Segmented(
                                options = listOf(IntervalUnit.DAY to "Days", IntervalUnit.WEEK to "Weeks", IntervalUnit.MONTH to "Months"),
                                value = rule.unit,
                                onSelect = { onChange(rule.copy(unit = it)) },
                            )
                        }
                    }
                }
            }

            Pattern.WEEKDAY -> {
                Field("On these days") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Dates.WD.forEachIndexed { i, d ->
                            val on = i in rule.weekdays
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(if (on) Tz.colors.rust else Color.Transparent)
                                    .border(1.5.dp, if (on) Tz.colors.rust else Tz.colors.line, CircleShape)
                                    .pressable {
                                        onChange(rule.copy(weekdays = if (on) rule.weekdays - i else rule.weekdays + i))
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    d.take(1),
                                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium),
                                    color = if (on) Color.White else Tz.colors.muted,
                                )
                            }
                        }
                    }
                }
                Field("Frequency") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Every", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                        Stepper(rule.weekInterval, { onChange(rule.copy(weekInterval = it)) })
                        Text(if (rule.weekInterval == 1) "week" else "weeks", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                    }
                }
            }

            Pattern.MONTHDAY -> {
                Field("On") {
                    Segmented(
                        options = listOf(MonthMode.DATE to "A date", MonthMode.WEEKDAY to "A weekday"),
                        value = rule.monthMode,
                        onSelect = { onChange(rule.copy(monthMode = it)) },
                    )
                }
                if (rule.monthMode == MonthMode.DATE) {
                    Field("Day of month") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("The", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                            Stepper(rule.monthDate, { onChange(rule.copy(monthDate = it)) }, min = 1, max = 31)
                            Text(Dates.ordinal(rule.monthDate).drop("${rule.monthDate}".length), style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                        }
                    }
                } else {
                    Field("Which weekday") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Segmented(
                                options = listOf(1 to "First", 2 to "Second", 3 to "Third", 4 to "Fourth", -1 to "Last"),
                                value = rule.monthWeekPos,
                                onSelect = { onChange(rule.copy(monthWeekPos = it)) },
                                small = true,
                            )
                            Segmented(
                                options = Dates.WD.mapIndexed { i, d -> i to d },
                                value = rule.monthWeekday,
                                onSelect = { onChange(rule.copy(monthWeekday = it)) },
                                small = true,
                            )
                        }
                    }
                }
                Field("Frequency") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Every", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                        Stepper(rule.monthInterval, { onChange(rule.copy(monthInterval = it)) })
                        Text(if (rule.monthInterval == 1) "month" else "months", style = TextStyle(fontFamily = DenType.body, fontSize = 15.sp), color = Tz.colors.muted)
                    }
                }
            }
        }
    }
}
