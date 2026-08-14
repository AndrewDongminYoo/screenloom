package kr.donminzzi.screenloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenloomThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun themeUsesTheApprovedSunlitRoles() {
        lateinit var colors: ColorScheme
        compose.setContent {
            ScreenloomTheme {
                colors = MaterialTheme.colorScheme
            }
        }

        compose.runOnIdle {
            assertEquals(Color(0xFFFFF8E9), colors.background)
            assertEquals(Color(0xFFFFFFFF), colors.surface)
            assertEquals(Color(0xFF18213D), colors.onBackground)
            assertEquals(Color(0xFF667087), colors.onSurfaceVariant)
            assertEquals(Color(0xFF566EFF), colors.primary)
            assertEquals(Color(0xFFFF6B4A), colors.secondary)
            assertEquals(Color(0xFF18213D), colors.onSecondary)
            assertEquals(Color(0xFFF7F4ED), colors.surfaceVariant)
            assertEquals(Color(0xFFE6DCCB), colors.outline)
        }
    }

    @Test
    fun applicationTextRolesMeetNormalTextContrastGuidance() {
        lateinit var colors: ColorScheme
        compose.setContent {
            ScreenloomTheme {
                colors = MaterialTheme.colorScheme
            }
        }

        compose.runOnIdle {
            listOf(
                colors.onBackground to colors.background,
                colors.onSurface to colors.surface,
                colors.onSurfaceVariant to colors.background,
                colors.onSurfaceVariant to colors.surfaceVariant,
                colors.onSecondary to colors.secondary,
            ).forEach { (foreground, background) ->
                val ratio = contrastRatio(foreground, background)
                assertTrue("Content contrast is $ratio", ratio >= 4.5f)
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (max(firstLuminance, secondLuminance) + 0.05f) /
            (min(firstLuminance, secondLuminance) + 0.05f)
    }
}
