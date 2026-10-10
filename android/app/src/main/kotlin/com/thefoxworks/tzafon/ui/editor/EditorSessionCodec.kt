package com.thefoxworks.tzafon.ui.editor

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.google.gson.Gson
import com.thefoxworks.tzafon.domain.editor.CueDraft
import com.thefoxworks.tzafon.domain.editor.EditorSession
import com.thefoxworks.tzafon.domain.model.CueType

/**
 * v2.13.0 — the open editor's saved form, so the snapshot, the draft and the
 * active tab survive rotation and the app being killed in the background.
 * One JSON string in the saved-state bundle; the domain classes keep their
 * field names under R8 (proguard-rules.pro), so a release build round-trips too.
 */
internal object EditorSessionCodec {
    private val gson = Gson()

    fun encode(session: EditorSession): String = gson.toJson(session)

    fun decode(saved: String): EditorSession = gson.fromJson(saved, EditorSession::class.java)

    val saver: Saver<EditorSession, String> = Saver(save = { encode(it) }, restore = { decode(it) })
}

/** The Alignment pane's cue as typed, saved beside the session (it isn't part of the draft). */
internal val CueDraftSaver: Saver<CueDraft, Any> = listSaver(
    save = { listOf(it.type.name, it.label, it.time) },
    restore = { CueDraft(CueType.valueOf(it[0]), it[1], it[2]) },
)
