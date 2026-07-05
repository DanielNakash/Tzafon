package com.thefoxworks.tzafon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thefoxworks.tzafon.ui.theme.Den
import com.thefoxworks.tzafon.ui.theme.a

/**
 * The compass rose — the "north" motif, a faithful port of the design's
 * 8-point SVG geometry (tz-core.jsx Compass, 48-unit space).
 */
@Composable
fun Compass(
    size: Dp = 44.dp,
    ring: Color = Den.rust,
    needleN: Color = Den.rust,
    needleS: Color = Den.faint,
    bg: Color = Color.Transparent,
    stroke: Float = 1.6f,
    ticks: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / 48f // 48-unit design space
        fun p(x: Float, y: Float) = Offset(x * u, y * u)

        // outer + inner rings
        drawCircle(bg, radius = 22 * u, center = p(24f, 24f))
        drawCircle(ring, radius = 22 * u, center = p(24f, 24f), style = Stroke(stroke * u))
        drawCircle(ring.a(ring.alpha * 0.4f), radius = 17.5f * u, center = p(24f, 24f), style = Stroke(stroke * 0.6f * u))

        // 4-point star path (main rose)
        fun star(): Path = Path().apply {
            moveTo(24 * u, 3.5f * u); lineTo(26.8f * u, 21.2f * u); lineTo(44.5f * u, 24 * u)
            lineTo(26.8f * u, 26.8f * u); lineTo(24 * u, 44.5f * u); lineTo(21.2f * u, 26.8f * u)
            lineTo(3.5f * u, 24 * u); lineTo(21.2f * u, 21.2f * u); close()
        }
        // smaller diagonal star (behind, rotated 45°)
        fun diag(): Path = Path().apply {
            moveTo(24 * u, 11 * u); lineTo(25.7f * u, 22.3f * u); lineTo(37 * u, 24 * u)
            lineTo(25.7f * u, 25.7f * u); lineTo(24 * u, 37 * u); lineTo(22.3f * u, 25.7f * u)
            lineTo(11 * u, 24 * u); lineTo(22.3f * u, 22.3f * u); close()
        }

        rotate(45f, pivot = p(24f, 24f)) { drawPath(diag(), needleS.a(needleS.alpha * 0.3f)) }
        drawPath(star(), needleS.a(needleS.alpha * 0.5f))

        // north point, accented
        drawPath(Path().apply { moveTo(24 * u, 3.5f * u); lineTo(26.8f * u, 21.2f * u); lineTo(24 * u, 24 * u); close() }, needleN)
        drawPath(
            Path().apply { moveTo(24 * u, 3.5f * u); lineTo(21.2f * u, 21.2f * u); lineTo(24 * u, 24 * u); close() },
            needleN.a(needleN.alpha * 0.72f),
        )

        if (ticks) {
            for (angle in listOf(45f, 135f, 225f, 315f)) {
                rotate(angle, pivot = p(24f, 24f)) {
                    drawCircle(ring.a(ring.alpha * 0.5f), radius = stroke * 0.9f * u, center = p(24f, 8.5f))
                }
            }
        }
        drawCircle(ring, radius = 2.3f * u, center = p(24f, 24f))
    }
}
