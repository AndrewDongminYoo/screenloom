package kr.donminzzi.screenloom.render

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kr.donminzzi.screenloom.editor.PaletteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterPaletteTest {
    @Test
    fun paletteIdentifiersRemainStable() {
        assertEquals(
            listOf(
                PaletteId.Ink,
                PaletteId.Cobalt,
                PaletteId.Coral,
                PaletteId.Moss,
                PaletteId.Violet,
                PaletteId.Sunrise,
            ),
            PaletteId.entries,
        )
    }

    @Test
    fun palettesMatchTheApprovedSunlitRoleMap() {
        val ink = 0xFF18213D.toInt()
        val paper = 0xFFFFF8E9.toInt()
        val coral = 0xFFFF6B4A.toInt()
        val cobalt = 0xFF566EFF.toInt()
        val sun = 0xFFFFD466.toInt()
        val expected = mapOf(
            PaletteId.Ink to PosterPalette(
                startColor = paper,
                endColor = 0xFFFFD9A2.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = cobalt,
                ribbonTwoColor = coral,
                sunColor = sun,
                copyZoneColor = null,
            ),
            PaletteId.Cobalt to PosterPalette(
                startColor = 0xFF3557F0.toInt(),
                endColor = 0xFF78DBEF.toInt(),
                headlineColor = paper,
                supportingCopyColor = paper,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = sun,
                ribbonTwoColor = coral,
                sunColor = paper,
                copyZoneColor = 0xFF3557F0.toInt(),
            ),
            PaletteId.Coral to PosterPalette(
                startColor = 0xFFFF765C.toInt(),
                endColor = 0xFFFFC46D.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = paper,
                ribbonTwoColor = cobalt,
                sunColor = 0xFFFFF0BD.toInt(),
                copyZoneColor = null,
            ),
            PaletteId.Moss to PosterPalette(
                startColor = 0xFF6BD7B3.toInt(),
                endColor = 0xFFD8EF6A.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = ink,
                ribbonTwoColor = coral,
                sunColor = paper,
                copyZoneColor = null,
            ),
            PaletteId.Violet to PosterPalette(
                startColor = 0xFF5D50D8.toInt(),
                endColor = 0xFFF3A1C7.toInt(),
                headlineColor = paper,
                supportingCopyColor = paper,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = sun,
                ribbonTwoColor = paper,
                sunColor = sun,
                copyZoneColor = 0xFF5D50D8.toInt(),
            ),
            PaletteId.Sunrise to PosterPalette(
                startColor = 0xFFFFE26C.toInt(),
                endColor = 0xFFFF7C56.toInt(),
                headlineColor = ink,
                supportingCopyColor = ink,
                frameColor = ink,
                shadowColor = ink,
                ribbonOneColor = cobalt,
                ribbonTwoColor = paper,
                sunColor = paper,
                copyZoneColor = null,
            ),
        )

        assertEquals(expected, PaletteId.entries.associateWith { paletteId -> paletteId.colors() })
    }

    @Test
    fun cobaltAndVioletCopyZonesCoverTheActualCopyAreaWithLegibleWarmWhiteContrast() {
        assertTrue(
            "Copy zone must include the full permitted copy area",
            PosterCopyZone.left <= 90f && PosterCopyZone.right >= 990f &&
                PosterCopyZone.top <= 150f && PosterCopyZone.bottom >= 450f,
        )

        listOf(PaletteId.Cobalt, PaletteId.Violet).forEach { paletteId ->
            val copyZoneColor = paletteId.colors().copyZoneColor
            assertTrue("$paletteId must define a copy-zone color", copyZoneColor != null)
            (90..990 step 30).forEach { x ->
                (150..450 step 30).forEach { y ->
                    val background = expectedUndarkenedBackground(paletteId, x, y)
                    val copyZone = composite(copyZoneColor!!, background, 235)
                    val ratio = contrastRatio(0xFFFFF8E9.toInt(), copyZone)
                    assertTrue("$paletteId copy-zone contrast at ($x, $y) is $ratio", ratio >= 4.5)
                }
            }
        }
    }

    private fun contrastRatio(first: Int, second: Int): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun component(shift: Int): Double {
            val encoded = ((color ushr shift) and 0xFF) / 255.0
            return if (encoded <= 0.04045) {
                encoded / 12.92
            } else {
                ((encoded + 0.055) / 1.055).pow(2.4)
            }
        }
        return 0.2126 * component(16) + 0.7152 * component(8) + 0.0722 * component(0)
    }

    private fun expectedUndarkenedBackground(paletteId: PaletteId, x: Int, y: Int): Int {
        val (start, end, sun) = when (paletteId) {
            PaletteId.Cobalt -> Triple(0xFF3557F0.toInt(), 0xFF78DBEF.toInt(), 0xFFFFF8E9.toInt())
            PaletteId.Violet -> Triple(0xFF5D50D8.toInt(), 0xFFF3A1C7.toInt(), 0xFFFFD466.toInt())
            else -> error("Only copy-zone palettes are covered")
        }
        val progress = ((x * 1080f + y * 1920f) / (1080f * 1080f + 1920f * 1920f)).coerceIn(0f, 1f)
        fun channel(component: (Int) -> Int): Int =
            (component(start) + progress * (component(end) - component(start))).toInt()
        val background = rgb(
            channel(::red),
            channel(::green),
            channel(::blue),
        )
        val sunCenterX = 0.82f * 1080f
        val sunCenterY = 0.18f * 1920f
        return if ((x - sunCenterX) * (x - sunCenterX) + (y - sunCenterY) * (y - sunCenterY) <= 240f * 240f) {
            composite(sun, background, 176)
        } else {
            background
        }
    }

    private fun composite(foreground: Int, background: Int, alpha: Int): Int {
        fun channel(component: (Int) -> Int): Int =
            ((component(foreground) * alpha + component(background) * (255 - alpha)) / 255f).toInt()
        return rgb(
            channel(::red),
            channel(::green),
            channel(::blue),
        )
    }

    private fun red(color: Int): Int = color ushr 16 and 0xFF

    private fun green(color: Int): Int = color ushr 8 and 0xFF

    private fun blue(color: Int): Int = color and 0xFF

    private fun rgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
