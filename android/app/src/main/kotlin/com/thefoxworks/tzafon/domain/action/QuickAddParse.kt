package com.thefoxworks.tzafon.domain.action

import com.thefoxworks.tzafon.domain.model.Cue
import com.thefoxworks.tzafon.domain.model.CueType

/**
 * FR-CAPTURE-3 — the one structural thing a quick-add title may carry inline:
 * a trailing clock time. `Call the bank 08:30` becomes the task *Call the bank*
 * with `Cue(AT_TIME, label = "", time = "08:30")` — the label-less shape
 * FR-CUE-2 made saveable and FR-CUE-3 made visible on every row.
 *
 * This is the only place the rule lives (FR-CAPTURE-3.1): all three quick-add
 * surfaces and the expand path call [parse], so what one accepts they all
 * accept. It takes no Android dependency, so it is unit-tested directly.
 */
object QuickAddParse {

    /**
     * FR-CAPTURE-3.2 — whitespace, then a zero-padded 24-hour HH:MM at the very
     * end. The time half is exactly the `HH_MM_REGEX` the CueSheet save-gate
     * uses (FR-CUE-1.4), so quick-add can never accept a time the cue sheet
     * would reject. `\s+` before the token is what keeps `Call 08:30` apart
     * from a title that merely ends in digits.
     */
    private val TRAILING_TIME = Regex("""^(.*\S)\s+(([01]\d|2[0-3]):[0-5]\d)$""")

    /** The typed title split into what to save and the time lifted off it. */
    data class Parsed(val title: String, val time: String?)

    /**
     * FR-CAPTURE-3.10 — the cue a parsed time becomes. The label stays empty on
     * purpose: the parse invents no prose, and `Cue.display()` (FR-CUE-3) then
     * renders the row chip as the bare time. Every draft builder goes through
     * here so the three surfaces write one identical shape.
     */
    fun cueFor(time: String?): Cue? =
        time?.let { Cue(type = CueType.AT_TIME, label = "", time = it) }

    /**
     * FR-CAPTURE-3.3 — no token, no change: the title comes back trimmed with a
     * null time, and every caller behaves exactly as it did before v2.12.0.
     * FR-CAPTURE-3.4 — a time-only title (`08:30`) does not parse either;
     * stripping it would leave nothing to call the task.
     */
    fun parse(raw: String): Parsed {
        val trimmed = raw.trim()
        val m = TRAILING_TIME.find(trimmed) ?: return Parsed(trimmed, null)
        val title = m.groupValues[1].trim()
        if (title.isEmpty()) return Parsed(trimmed, null)
        return Parsed(title, m.groupValues[2])
    }
}
