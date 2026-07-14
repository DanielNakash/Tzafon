package com.thefoxworks.tzafon

import com.thefoxworks.tzafon.ui.theme.allPalettes
import com.thefoxworks.tzafon.ui.theme.bluePalette
import com.thefoxworks.tzafon.ui.theme.denPalette
import com.thefoxworks.tzafon.ui.theme.magentaPalette
import com.thefoxworks.tzafon.ui.theme.paletteFor
import com.thefoxworks.tzafon.ui.theme.tealPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * DM-PREF-1 / FR-DESIGN-4 — palette resolution + the invariants the picker and
 * the theme root depend on.
 */
class PaletteTest {

    @Test
    fun `known palette names resolve to their palette`() {
        assertSame(denPalette, paletteFor("den"))
        assertSame(bluePalette, paletteFor("blue"))
        assertSame(tealPalette, paletteFor("teal"))
        assertSame(magentaPalette, paletteFor("magenta"))
    }

    @Test
    fun `unknown and null palette names fall back to Den`() {
        assertSame(denPalette, paletteFor(null))
        assertSame(denPalette, paletteFor(""))
        assertSame(denPalette, paletteFor("chartreuse"))
        assertSame(denPalette, paletteFor("DEN"))       // case-sensitive key → default
    }

    @Test
    fun `five palettes ship in picker order with Den first`() {
        assertEquals(5, allPalettes.size)
        assertEquals(listOf("den", "blue", "green", "magenta", "teal"), allPalettes.map { it.first })
    }

    @Test
    fun `each palette exposes exactly three theme accents`() {
        allPalettes.forEach { (name, p) ->
            assertEquals("$name themeAccents", 3, p.themeAccents.size)
            assertEquals(listOf(p.tHealth, p.tWrite, p.tLearn), p.themeAccents)
        }
    }

    @Test
    fun `theme accents stay distinct from the palette primary`() {
        // FR-DESIGN-4.3 / FR-DIR — no accent slot collides with the rust primary.
        allPalettes.forEach { (name, p) ->
            p.themeAccents.forEach { accent ->
                assertNotEquals("$name: accent must differ from rust", p.rust, accent)
            }
        }
    }

    @Test
    fun `magenta shifts backlog off its primary hue`() {
        // FR-DESIGN-4.10 — the someday-state mauve must not collide with magenta.
        assertNotEquals(magentaPalette.backlog, denPalette.backlog)
        assertNotEquals(magentaPalette.backlog, magentaPalette.rust)
    }
}
