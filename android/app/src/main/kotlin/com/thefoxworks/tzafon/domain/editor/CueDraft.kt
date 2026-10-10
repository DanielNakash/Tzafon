package com.thefoxworks.tzafon.domain.editor

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType

/** FR-CUE-1.4 — the canonical `HH:mm` (00:00..23:59, zero-padded, colon required), trimmed first. */
object CueTime {
    private val HH_MM = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    fun isValid(s: String): Boolean = HH_MM.matches(s.trim())
}

/**
 * v2.13.0 — the Alignment pane's inline cue as typed: the selected [type]
 * and the [label] and [time] fields. Both fields keep their text while the
 * type switches (FR-CUE-1.6); only the selected type's fields are saved.
 */
data class CueDraft(
    val type: CueType,
    val label: String,
    val time: String,
) {
    /** At a time and After a routine show the time field; At a place has no clock (FR-NOTIF-3). */
    val showsTime: Boolean get() = type != CueType.AT_PLACE

    /**
     * Something is typed in a shown time field but it isn't `HH:mm` yet:
     * Save stops on it ([SaveProblem.INCOMPLETE_CUE_TIME]). An empty time is
     * never incomplete; it only means there's no time.
     */
    val timeIncomplete: Boolean get() = showsTime && time.isNotBlank() && !CueTime.isValid(time)

    /**
     * The cue Save stores, or null when there's none. A cue exists only once
     * its required field is filled: the time for At a time (label optional,
     * FR-CUE-2), the label for After a routine (time optional) and At a place.
     */
    fun cue(): Cue? {
        val l = label.trim()
        val t = time.trim().takeIf { showsTime && CueTime.isValid(it) }
        return when (type) {
            CueType.AT_TIME -> t?.let { Cue(type, l, it) }
            CueType.AFTER_ROUTINE -> Cue(type, l, t).takeIf { l.isNotEmpty() }
            CueType.AT_PLACE -> Cue(type, l).takeIf { l.isNotEmpty() }
        }
    }

    fun select(type: CueType) = copy(type = type)

    /** The field's ×: no cue, the type stays selected. */
    fun clear() = copy(label = "", time = "")

    companion object {
        /** The stored cue as the pane opens it; no cue opens empty on After a routine. */
        fun of(cue: Cue?) = CueDraft(
            type = cue?.type ?: CueType.AFTER_ROUTINE,
            label = cue?.label.orEmpty(),
            time = cue?.time.orEmpty(),
        )
    }
}
