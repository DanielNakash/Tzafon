package com.thefoxworks.tzafon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * FR-DESIGN-6 — one vocabulary app-wide. Scans the string literals under
 * `ui/` and `notify/` for the words v2.13.0 retired (comments and code
 * identifiers are free to keep them, FR-DESIGN-6.9).
 */
class WordingTest {

    /** Each retired phrase, and the FR-DESIGN-6 rule that retired it. */
    private val retired = mapOf(
        Regex("Someday", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.2 Backlog, never Someday",
        Regex("description…", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.10 Search title or notes…",
        Regex("Description · optional", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.3 Notes",
        Regex("Change Status") to "FR-DESIGN-6.1 Set status",
        Regex("task state", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.1 Status, not state",
        Regex("RECURRING TASKS|Recurrence|recurring", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.6 Repeat",
        Regex("Due Date|To Do Date", RegexOption.IGNORE_CASE) to "FR-DESIGN-6.3 To-do date / Deadline",
        Regex("DATES, CUE, SERVES") to "FR-DESIGN-6.10 EXPAND — TIMING, ALIGNMENT",
        Regex("Where does this stand") to "FR-STATUS-1.3",
        Regex("This & future") to "FR-DESIGN-6.7 scope labels",
    )

    private val root = File("src/main/kotlin/com/thefoxworks/tzafon")

    /** A string literal on one line, skipping escaped quotes. */
    private val literal = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")

    private fun literals(dir: String): List<Pair<String, String>> =
        File(root, dir).walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            f.readLines().withIndex()
                .filterNot { (_, line) -> line.trimStart().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") } }
                .flatMap { (i, line) -> literal.findAll(line).map { "${f.name}:${i + 1}" to it.groupValues[1] } }
        }.toList()

    @Test
    fun `the scan reads the real sources`() {
        assertTrue("run from the app module", File(root, "ui").isDirectory)
        assertTrue(literals("ui").size > 500)
    }

    @Test
    fun `no retired word is left in a ui or notify string`() {
        val found = (literals("ui") + literals("notify")).flatMap { (at, text) ->
            retired.filterKeys { it.containsMatchIn(text) }.values.map { rule -> "$at \"$text\" — $rule" }
        }
        assertEquals(emptyList<String>(), found)
    }

    @Test
    fun `quick add's expand names the editor's panes`() {
        assertTrue(literals("ui/capture").any { (_, text) -> text == "EXPAND — TIMING, ALIGNMENT" })
    }
}
