package com.thefoxworks.tzafon.ui.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.ui.components.SectionLabel

/** A section heading inside an editor pane, shared by all three panes. */
@Composable
internal fun EdSection(text: String) {
    SectionLabel(text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp, start = 4.dp))
}
