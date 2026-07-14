package com.thefoxworks.tzafon.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    onSave: (String) -> Unit,
    onExpand: (String) -> Unit,
    onClose: () -> Unit,
    datedByDefault: Boolean = false,
) {
    var title by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

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
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontFamily = DenType.serif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Tz.colors.ink,
                ).contentDir(),
                cursorBrush = SolidColor(Tz.colors.rust),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (title.isNotBlank()) { onSave(title.trim()); onClose() }
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
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Tz.colors.line, RoundedCornerShape(999.dp))
                        .pressable { onExpand(title.trim()) }
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
                    color = if (title.isBlank()) Tz.colors.faint else Tz.colors.rust,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .pressable { if (title.isNotBlank()) { onSave(title.trim()); onClose() } }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (!datedByDefault) {
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
