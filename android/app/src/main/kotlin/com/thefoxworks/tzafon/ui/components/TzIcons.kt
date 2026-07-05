package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The bespoke Tzafon line-icon set — ports of the design's 24-grid SVGs
 * (tz-core.jsx TI.*). Drawn parametrically so size / color / weight match
 * the design exactly where it varies them.
 */
object TzIcons {

    private class IconScope(val ds: DrawScope, val c: Color, val w: Float) {
        val u = ds.size.minDimension / 24f
        fun stroke(width: Float = w) = Stroke(width * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = w) =
            ds.drawLine(c, p(x1, y1), p(x2, y2), width * u, cap = StrokeCap.Round)
        fun path(build: Path.(Float) -> Unit): Path = Path().apply { build(u) }
        fun draw(path: Path, width: Float = w) = ds.drawPath(path, c, style = stroke(width))
        fun fill(path: Path) = ds.drawPath(path, c)
        fun circle(cx: Float, cy: Float, r: Float, filled: Boolean = false, width: Float = w) {
            if (filled) ds.drawCircle(c, r * u, p(cx, cy))
            else ds.drawCircle(c, r * u, p(cx, cy), style = stroke(width))
        }
    }

    @Composable
    private fun Icon(
        size: Dp,
        color: Color,
        weight: Float,
        modifier: Modifier = Modifier,
        draw: IconScope.() -> Unit,
    ) {
        Canvas(modifier.size(size)) { IconScope(this, color, weight).draw() }
    }

    @Composable
    fun Check(size: Dp = 16.dp, color: Color = Color.White, weight: Float = 3f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(5 * u, 12.5f * u); lineTo(9.5f * u, 17 * u); lineTo(19 * u, 7 * u) })
        }

    @Composable
    fun Plus(size: Dp = 22.dp, color: Color = Color.White, weight: Float = 2.6f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }

    @Composable
    fun X(size: Dp = 16.dp, color: Color, weight: Float = 2.4f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }

    @Composable
    fun Trash(size: Dp = 18.dp, color: Color, weight: Float = 1.7f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            line(4f, 7f, 20f, 7f)
            draw(path { u -> moveTo(9 * u, 7 * u); lineTo(9 * u, 5 * u); quadraticTo(9 * u, 4 * u, 10 * u, 4 * u); lineTo(14 * u, 4 * u); quadraticTo(15 * u, 4 * u, 15 * u, 5 * u); lineTo(15 * u, 7 * u) })
            draw(path { u -> moveTo(6 * u, 7 * u); lineTo(7 * u, 20 * u); quadraticTo(7 * u, 21 * u, 8 * u, 21 * u); lineTo(16 * u, 21 * u); quadraticTo(17 * u, 21 * u, 17 * u, 20 * u); lineTo(18 * u, 7 * u) })
        }

    @Composable
    fun Back(size: Dp = 24.dp, color: Color, weight: Float = 2f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(15 * u, 5 * u); lineTo(8 * u, 12 * u); lineTo(15 * u, 19 * u) })
        }

    enum class Dir { RIGHT, LEFT, DOWN, UP }

    @Composable
    fun Chevron(size: Dp = 20.dp, color: Color, weight: Float = 2f, dir: Dir = Dir.RIGHT, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            ds.run {
                val deg = when (dir) { Dir.RIGHT -> 0f; Dir.DOWN -> 90f; Dir.LEFT -> 180f; Dir.UP -> -90f }
                rotate(deg, center) {
                    drawPath(
                        path { u -> moveTo(9 * u, 5 * u); lineTo(16 * u, 12 * u); lineTo(9 * u, 19 * u) },
                        c, style = stroke(),
                    )
                }
            }
        }

    @Composable
    fun Calendar(size: Dp = 19.dp, color: Color, weight: Float = 1.7f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            ds.drawRoundRect(c, p(3.5f, 5f), Size(17 * u, 15.5f * u), CornerRadius(2.5f * u), style = stroke())
            line(3.5f, 9.5f, 20.5f, 9.5f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
        }

    @Composable
    fun Repeat(size: Dp = 16.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(4 * u, 9 * u); cubicTo(4 * u, 6.2f * u, 6.2f * u, 4 * u, 9 * u, 4 * u); lineTo(16 * u, 4 * u); lineTo(13.8f * u, 1.8f * u) })
            draw(path { u -> moveTo(20 * u, 15 * u); cubicTo(20 * u, 17.8f * u, 17.8f * u, 20 * u, 15 * u, 20 * u); lineTo(8 * u, 20 * u); lineTo(10.2f * u, 22.2f * u) })
            draw(path { u -> moveTo(16 * u, 1.8f * u); lineTo(18.4f * u, 4 * u); lineTo(16 * u, 6.2f * u) })
            draw(path { u -> moveTo(8 * u, 22.2f * u); lineTo(5.6f * u, 20 * u); lineTo(8 * u, 17.8f * u) })
        }

    @Composable
    fun Flag(size: Dp = 14.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            line(6f, 21f, 6f, 4f)
            draw(path { u -> moveTo(6 * u, 5 * u); lineTo(17 * u, 5 * u); lineTo(14.5f * u, 9 * u); lineTo(17 * u, 13 * u); lineTo(6 * u, 13 * u) })
        }

    @Composable
    fun Alert(size: Dp = 14.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            circle(12f, 12f, 9f)
            line(12f, 7.5f, 12f, 12.5f)
            circle(12f, 16f, weight / 2f, filled = true)
        }

    @Composable
    fun Search(size: Dp = 18.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { circle(11f, 11f, 7f); line(20f, 20f, 16f, 16f) }

    @Composable
    fun Eye(size: Dp = 18.dp, color: Color, weight: Float = 1.8f, off: Boolean = false, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u ->
                moveTo(2 * u, 12 * u)
                cubicTo(5.6f * u, 5 * u, 18.4f * u, 5 * u, 22 * u, 12 * u)
                cubicTo(18.4f * u, 19 * u, 5.6f * u, 19 * u, 2 * u, 12 * u)
                close()
            })
            circle(12f, 12f, 2.6f)
            if (off) line(3f, 3f, 21f, 21f)
        }

    @Composable
    fun Menu(size: Dp = 20.dp, color: Color, weight: Float = 2f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { line(4f, 7f, 20f, 7f); line(4f, 12f, 20f, 12f); line(4f, 17f, 20f, 17f) }

    // ── nav + object icons ──────────────────────────────────

    /** sun — Today */
    @Composable
    fun Today(size: Dp = 24.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            circle(12f, 12f, 4.7f)
            line(12f, 2.2f, 12f, 4.9f); line(12f, 19.1f, 12f, 21.8f)
            line(2.2f, 12f, 4.9f, 12f); line(19.1f, 12f, 21.8f, 12f)
            line(5.05f, 5.05f, 6.95f, 6.95f); line(17.05f, 17.05f, 18.95f, 18.95f)
            line(18.95f, 5.05f, 17.05f, 6.95f); line(6.95f, 17.05f, 5.05f, 18.95f)
        }

    /** calendar with lines — Planning */
    @Composable
    fun Plan(size: Dp = 24.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            ds.drawRoundRect(c, p(3.5f, 5f), Size(17 * u, 15.5f * u), CornerRadius(2.5f * u), style = stroke())
            line(3.5f, 9.5f, 20.5f, 9.5f); line(8f, 3f, 8f, 7f); line(16f, 3f, 16f, 7f)
            line(7.5f, 13.5f, 12.5f, 13.5f); line(7.5f, 16.5f, 15.5f, 16.5f)
        }

    /** cycle arrows — Habits */
    @Composable
    fun Habit(size: Dp = 24.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(4 * u, 12 * u); cubicTo(4 * u, 7.6f * u, 7.6f * u, 4 * u, 12 * u, 4 * u); cubicTo(15.2f * u, 4 * u, 17.9f * u, 5.9f * u, 19.2f * u, 8.6f * u) })
            draw(path { u -> moveTo(20 * u, 12 * u); cubicTo(20 * u, 16.4f * u, 16.4f * u, 20 * u, 12 * u, 20 * u); cubicTo(8.8f * u, 20 * u, 6.1f * u, 18.1f * u, 4.8f * u, 15.4f * u) })
            draw(path { u -> moveTo(18.2f * u, 3.5f * u); lineTo(19.5f * u, 8.5f * u); lineTo(14.5f * u, 7.5f * u) })
            draw(path { u -> moveTo(5.8f * u, 20.5f * u); lineTo(4.5f * u, 15.5f * u); lineTo(9.5f * u, 16.5f * u) })
        }

    /** compass — Directions */
    @Composable
    fun CompassIcon(size: Dp = 24.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            circle(12f, 12f, 8.5f)
            fill(path { u -> moveTo(15.5f * u, 8.5f * u); lineTo(13 * u, 13 * u); lineTo(8.5f * u, 15.5f * u); lineTo(11 * u, 11 * u); close() })
        }

    /** mountain trail — Journey */
    @Composable
    fun Journey(size: Dp = 24.dp, color: Color, weight: Float = 1.9f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(2.5f * u, 20 * u); lineTo(8.5f * u, 8 * u); lineTo(12 * u, 13.5f * u); lineTo(16 * u, 5 * u); lineTo(21.5f * u, 20 * u); close() })
            ds.run {
                drawPath(
                    path { u -> moveTo(6.4f * u, 15 * u); lineTo(8.5f * u, 13.5f * u); lineTo(10.2f * u, 14.7f * u) },
                    c.copy(alpha = c.alpha * 0.55f), style = stroke(),
                )
            }
        }

    /** concentric target — Goal */
    @Composable
    fun Target(size: Dp = 18.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { circle(12f, 12f, 8.5f); circle(12f, 12f, 4.5f); circle(12f, 12f, 1f, filled = true) }

    /** lightning bolt — Cue */
    @Composable
    fun Cue(size: Dp = 16.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(13 * u, 2 * u); lineTo(4.5f * u, 13 * u); lineTo(11 * u, 13 * u); lineTo(10 * u, 22 * u); lineTo(18.5f * u, 11 * u); lineTo(12 * u, 11 * u); close() })
        }

    /** snowflake — Frozen */
    @Composable
    fun Frozen(size: Dp = 16.dp, color: Color, weight: Float = 1.7f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            line(12f, 2f, 12f, 22f); line(4f, 7f, 20f, 17f); line(20f, 7f, 4f, 17f)
            line(12f, 2f, 9.8f, 4.2f); line(12f, 2f, 14.2f, 4.2f)
            line(12f, 22f, 9.8f, 19.8f); line(12f, 22f, 14.2f, 19.8f)
        }

    /** crescent — Backlog / someday */
    @Composable
    fun Moon(size: Dp = 16.dp, color: Color, weight: Float = 1.7f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u ->
                moveTo(20 * u, 14.5f * u)
                cubicTo(14.7f * u, 15.7f * u, 8.3f * u, 9.3f * u, 9.5f * u, 4 * u)
                cubicTo(3.5f * u, 5.5f * u, 2.5f * u, 13.5f * u, 7 * u, 18 * u)
                cubicTo(11.5f * u, 22.5f * u, 18.5f * u, 20.5f * u, 20 * u, 14.5f * u)
                close()
            })
        }

    /** slashed circle — Closed / skip */
    @Composable
    fun Skip(size: Dp = 16.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { circle(12f, 12f, 9f); line(8.5f, 8.5f, 15.5f, 15.5f) }

    /** two-leaf sprout — arcs / fresh starts */
    @Composable
    fun Sprout(size: Dp = 18.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            line(12f, 21f, 12f, 13f)
            draw(path { u -> moveTo(12 * u, 13 * u); cubicTo(12 * u, 10 * u, 9.5f * u, 8 * u, 6 * u, 8 * u); cubicTo(6 * u, 11 * u, 8.5f * u, 13 * u, 12 * u, 13 * u); close() })
            draw(path { u -> moveTo(12 * u, 11 * u); cubicTo(12 * u, 8 * u, 14.5f * u, 5.5f * u, 18 * u, 5.5f * u); cubicTo(18 * u, 8.5f * u, 15.5f * u, 11 * u, 12 * u, 11 * u); close() })
        }

    /** sparkle — milestones */
    @Composable
    fun Sparkle(size: Dp = 16.dp, color: Color, modifier: Modifier = Modifier) =
        Icon(size, color, 1.6f, modifier) {
            ds.run {
                drawPath(
                    path { u -> moveTo(12 * u, 3 * u); lineTo(13.8f * u, 8.4f * u); lineTo(19 * u, 10 * u); lineTo(13.8f * u, 11.6f * u); lineTo(12 * u, 17 * u); lineTo(10.2f * u, 11.6f * u); lineTo(5 * u, 10 * u); lineTo(10.2f * u, 8.4f * u); close() },
                    c.copy(alpha = c.alpha * 0.9f),
                )
                drawPath(
                    path { u -> moveTo(19 * u, 15 * u); lineTo(19.7f * u, 17 * u); lineTo(21.7f * u, 17.7f * u); lineTo(19.7f * u, 18.4f * u); lineTo(19 * u, 20.4f * u); lineTo(18.3f * u, 18.4f * u); lineTo(16.3f * u, 17.7f * u); lineTo(18.3f * u, 17 * u); close() },
                    c.copy(alpha = c.alpha * 0.7f),
                )
            }
        }

    @Composable
    fun Pencil(size: Dp = 16.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(4 * u, 20 * u); lineTo(8 * u, 19 * u); lineTo(19 * u, 8 * u); lineTo(16 * u, 5 * u); lineTo(5 * u, 16 * u); close() })
            line(14.5f, 6.5f, 17.5f, 9.5f)
        }

    @Composable
    fun Bell(size: Dp = 18.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u ->
                moveTo(6 * u, 9 * u)
                cubicTo(6 * u, 5.7f * u, 8.7f * u, 3 * u, 12 * u, 3 * u)
                cubicTo(15.3f * u, 3 * u, 18 * u, 5.7f * u, 18 * u, 9 * u)
                cubicTo(18 * u, 14 * u, 20 * u, 15 * u, 20 * u, 15 * u)
                lineTo(4 * u, 15 * u)
                cubicTo(4 * u, 15 * u, 6 * u, 14 * u, 6 * u, 9 * u)
                close()
            })
            draw(path { u -> moveTo(10 * u, 19 * u); cubicTo(10 * u, 20.1f * u, 10.9f * u, 21 * u, 12 * u, 21 * u); cubicTo(13.1f * u, 21 * u, 14 * u, 20.1f * u, 14 * u, 19 * u) })
        }

    @Composable
    fun Link(size: Dp = 15.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(10 * u, 14 * u); cubicTo(11.5f * u, 15.5f * u, 14 * u, 15.5f * u, 15.6f * u, 14 * u); lineTo(18.6f * u, 11 * u); cubicTo(20.2f * u, 9.4f * u, 20.2f * u, 7 * u, 18.6f * u, 5.4f * u); cubicTo(17 * u, 3.8f * u, 14.6f * u, 3.8f * u, 13 * u, 5.4f * u); lineTo(11.5f * u, 7 * u) })
            draw(path { u -> moveTo(14 * u, 10 * u); cubicTo(12.5f * u, 8.5f * u, 10 * u, 8.5f * u, 8.4f * u, 10 * u); lineTo(5.4f * u, 13 * u); cubicTo(3.8f * u, 14.6f * u, 3.8f * u, 17 * u, 5.4f * u, 18.6f * u); cubicTo(7 * u, 20.2f * u, 9.4f * u, 20.2f * u, 11 * u, 18.6f * u); lineTo(12.5f * u, 17 * u) })
        }

    @Composable
    fun Clock(size: Dp = 15.dp, color: Color, weight: Float = 1.8f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) { circle(12f, 12f, 8.5f); draw(path { u -> moveTo(12 * u, 7.5f * u); lineTo(12 * u, 12 * u); lineTo(15 * u, 14 * u) }) }

    @Composable
    fun Coffee(size: Dp = 15.dp, color: Color, weight: Float = 1.7f, modifier: Modifier = Modifier) =
        Icon(size, color, weight, modifier) {
            draw(path { u -> moveTo(4 * u, 8 * u); lineTo(17 * u, 8 * u); lineTo(17 * u, 13 * u); cubicTo(17 * u, 15.8f * u, 14.8f * u, 18 * u, 12 * u, 18 * u); lineTo(9 * u, 18 * u); cubicTo(6.2f * u, 18 * u, 4 * u, 15.8f * u, 4 * u, 13 * u); close() })
            draw(path { u -> moveTo(17 * u, 9 * u); lineTo(19.5f * u, 9 * u); cubicTo(20.9f * u, 9 * u, 22 * u, 10.1f * u, 22 * u, 11.5f * u); cubicTo(22 * u, 12.9f * u, 20.9f * u, 14 * u, 19.5f * u, 14 * u); lineTo(17 * u, 14 * u) })
            ds.run {
                drawPath(path { u -> moveTo(8 * u, 3.5f * u); cubicTo(7.5f * u, 4.3f * u, 7.5f * u, 5.2f * u, 8 * u, 6 * u) }, c.copy(alpha = c.alpha * 0.7f), style = stroke())
                drawPath(path { u -> moveTo(12 * u, 3.5f * u); cubicTo(11.5f * u, 4.3f * u, 11.5f * u, 5.2f * u, 12 * u, 6 * u) }, c.copy(alpha = c.alpha * 0.7f), style = stroke())
            }
        }
}
