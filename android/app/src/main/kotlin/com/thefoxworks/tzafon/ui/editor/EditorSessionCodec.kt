package com.thefoxworks.tzafon.ui.editor

import androidx.compose.runtime.saveable.Saver
import com.google.gson.Gson
import com.thefoxworks.tzafon.domain.editor.EditorSession

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
