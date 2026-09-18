package com.thefoxworks.tzafon.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thefoxworks.tzafon.domain.action.QuickAddParse
import com.thefoxworks.tzafon.ui.components.TzIcons
import com.thefoxworks.tzafon.ui.components.pressable
import com.thefoxworks.tzafon.ui.theme.Tz
import com.thefoxworks.tzafon.ui.theme.DenType
import com.thefoxworks.tzafon.ui.theme.contentDir

/**
 * FR-CAPTURE-1 — the one-line quick-add (design: QuickAdd). Title + Enter
 * saves; EXPAND opens the full editor with the typed title carried over.
 * FR-CAPTURE-2 — the default capture path lands in the Inbox (undated). Today's
 * own quick-add overrides this via FR-TODAY-7 and passes [datedByDefault] = true;
 * the sheet then suppresses the "inbox" helper line so the copy isn't misleading.
 *
 * FR-CAPTURE-3 — a trailing `HH:MM` is lifted off the title into an AT_TIME cue.
 * [onSave] and [onExpand] receive the stripped title and that time; when nothing
 * parsed — the overwhelmingly common case — they receive the typed title and
 * null, exactly as before. The parse is never silent: a caption previews it and
 * one tap declines it (FR-CAPTURE-3.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onSave: (title: String, time: String?) -> Unit,
    onExpand: (title: String, time: String?) -> Unit,
    onClose: () -> Unit,
    datedByDefault: Boolean = false,
    datesOnParsedTime: Boolean = false,
) {
    var title by remember { mutableStateOf("") }
    // FR-CAPTURE-3.5 — declining the parse is per-capture, and typing re-arms it
    var optedOut by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    val parsed = QuickAddParse.parse(title)
    val parsedTime = parsed.time?.takeIf { !optedOut }
    // what actually gets saved: the stripped title only when the parse stands
    val saveTitle = if (parsedTime != null) parsed.title else title.trim()
    val canSave = saveTitle.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Tz.colors.surface,
        contentColor = Tz.colors.ink,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                "QUICK ADD",
                style = TextStyle(fontFamily = DenType.mono, fontSize = 10.sp, letterSpacing = 1.sp),
                color = Tz.colors.faint,
            )
            BasicTextField(
                value = title,
                onValueChange = { title = it; optedOut = false },
                textStyle = TextStyle(
                    fontFamily = DenType.serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Tz.colors.ink,
                ).contentDir(),
                cursorBrush = SolidColor(Tz.colors.rust),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (canSave) { onSave(saveTitle, parsedTime); onClose() }
                }),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "What needs doing?",
                                style = TextStyle(fontFamily = DenType.serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                color = Tz.colors.faint,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .focusRequester(focus),
            )
            // FR-CAPTURE-3.5 — the parse is legible before commit, and one tap
            // declines it. A caption and a tap target, not a confirmation dialog:
            // whoever ignores it keeps FR-CAPTURE-1's friction floor intact.
            parsed.time?.let { t ->
                val on = !optedOut
                Text(
                    if (on) "CUE · $t — TAP TO KEEP AS TEXT" else "$t KEPT IN TITLE — TAP TO SET AS CUE",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp, lineHeight = 14.sp),
                    color = if (on) Tz.colors.muted else Tz.colors.faint,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pressable { optedOut = !optedOut }
                        .heightIn(min = 44.dp) // NFR-A11Y-1
                        .wrapContentHeight(Alignment.CenterVertically)
                        .semantics {
                            contentDescription = if (on) {
                                "Cue $t will be set. Tap to keep $t in the title instead."
                            } else {
                                "$t kept in the title. Tap to set it as a cue instead."
                            }
                        },
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
                        .pressable { onExpand(saveTitle, parsedTime) }
                        .padding(horizontal = 13.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    TzIcons.Pencil(13.dp, Tz.colors.muted)
                    Text(
                        "EXPAND — DATES, CUE, SERVES",
                        style = TextStyle(fontFamily = DenType.mono, fontSize = 11.sp),
                        color = Tz.colors.muted,
                    )
                }
                Box(Modifier.weight(1f))
                Text(
                    "↵ SAVE",
                    style = TextStyle(fontFamily = DenType.mono, fontSize = 10.5.sp),
                    color = if (!canSave) Tz.colors.faint else Tz.colors.rust,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressable { if (canSave) { onSave(saveTitle, parsedTime); onClose() } }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            // FR-PLAN-7 — on Planning a parsed time dates the task to today, so the
            // inbox promise below would be a lie for exactly that capture; same
            // reason `datedByDefault` suppresses it on Today (FR-DESIGN-1).
            if (!datedByDefault && !(datesOnParsedTime && parsedTime != null)) {
                Text(
                    "Just a title is enough. No date needed — it lands in your inbox to schedule later.",
                    style = TextStyle(fontFamily = DenType.body, fontSize = 12.sp, lineHeight = 16.5.sp),
                    color = Tz.colors.faint,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }
}
