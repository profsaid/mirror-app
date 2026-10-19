package com.saeed.mirror.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.saeed.mirror.model.HairstyleCatalog
import com.saeed.mirror.model.Hairstyle
import com.saeed.mirror.model.HeadAngle
import com.saeed.mirror.ui.theme.MirrorColors

private const val FRONT_TORSO = "M30 240 Q40 205 80 203 L120 203 Q160 205 170 240 Z"
private const val FRONT_NECK = "M82 176 L82 206 L118 206 L118 176 Z"
private const val SIDE_TORSO = "M30 240 Q40 205 80 203 L130 203 Q165 206 172 240 Z"
private const val SIDE_NECK = "M86 172 L84 206 L124 206 L122 180 Z"
private const val SIDE_HEAD =
    "M62 120 Q60 70 104 60 Q144 56 150 104 L152 116 L160 134 Q162 139 154 141 L156 150 Q158 156 152 158 L150 168 Q146 182 128 182 L112 184 Q86 186 74 168 Q62 150 62 120 Z"

private val Skin = Color(0xFF636366)
private val Shadow = Color(0xFF545456)
private val Torso = Color(0xFF3A3A3C)

/** The flat head drawings from the mockups, drawn from the same 200×240 path data as iOS. */
@Composable
fun HeadIllustration(
    angle: HeadAngle,
    style: Hairstyle?,
    modifier: Modifier = Modifier,
    hairColor: Color = MirrorColors.Accent
) {
    val hair = when {
        style == null && angle == HeadAngle.FRONT -> HairstyleCatalog.PLACEHOLDER_FRONT
        style == null -> HairstyleCatalog.PLACEHOLDER_SIDE
        angle == HeadAngle.FRONT -> style.frontPath
        else -> style.sidePath
    }
    val hairPath = remember(hair) { parseSvgPath(hair) }
    val parts = remember(angle) {
        if (angle == HeadAngle.FRONT) {
            listOf(parseSvgPath(FRONT_TORSO) to Torso, parseSvgPath(FRONT_NECK) to Skin)
        } else {
            listOf(parseSvgPath(SIDE_TORSO) to Torso, parseSvgPath(SIDE_NECK) to Skin, parseSvgPath(SIDE_HEAD) to Skin)
        }
    }

    Canvas(modifier.aspectRatio(200f / 240f)) {
        val s = minOf(size.width / 200f, size.height / 240f)
        val dx = (size.width - 200f * s) / 2f
        val dy = (size.height - 240f * s) / 2f
        translate(dx, dy) {
            scale(s, s, pivot = Offset.Zero) {
                // The side drawing faces right; mirror it for the left side.
                if (angle == HeadAngle.LEFT) {
                    scale(-1f, 1f, pivot = Offset(100f, 120f)) { drawHead(angle, parts, hairPath, hairColor) }
                } else {
                    drawHead(angle, parts, hairPath, hairColor)
                }
            }
        }
    }
}

private fun DrawScope.drawHead(angle: HeadAngle, parts: List<Pair<Path, Color>>, hair: Path, hairColor: Color) {
    parts.forEach { (path, color) -> drawPath(path, color) }
    if (angle == HeadAngle.FRONT) {
        oval(47f, 128f, 8f, 14f, Shadow)
        oval(153f, 128f, 8f, 14f, Shadow)
        oval(100f, 124f, 52f, 64f, Skin)
    } else {
        oval(96f, 126f, 9f, 14f, Shadow)
    }
    drawPath(hair, hairColor)
}

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
}

/** Reads the absolute M, L, Q and Z commands used by the illustrations. */
fun parseSvgPath(d: String): Path {
    val tokens = Regex("[MLQZ]|-?\\d+(?:\\.\\d+)?").findAll(d).map { it.value }.toList()
    val path = Path()
    var i = 0
    var command = 'M'
    fun num(): Float = tokens.getOrNull(i++)?.toFloatOrNull() ?: 0f
    while (i < tokens.size) {
        val t = tokens[i]
        if (t.length == 1 && t[0].isLetter()) {
            command = t[0]
            i++
            if (command == 'Z') path.close()
            continue
        }
        when (command) {
            'M' -> { path.moveTo(num(), num()); command = 'L' }
            'L' -> path.lineTo(num(), num())
            'Q' -> { val cx = num(); val cy = num(); val x = num(); val y = num(); path.quadraticTo(cx, cy, x, y) }
            else -> i++
        }
    }
    return path
}
