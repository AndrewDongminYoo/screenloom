package kr.donminzzi.screenloom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenloomThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun primaryContentMeetsNormalTextContrastGuidance() {
        var primary = Color.Unspecified
        var onPrimary = Color.Unspecified
        compose.setContent {
            ScreenloomTheme {
                primary = MaterialTheme.colorScheme.primary
                onPrimary = MaterialTheme.colorScheme.onPrimary
            }
        }

        compose.runOnIdle {
            val ratio = contrastRatio(primary, onPrimary)
            assertTrue("Primary content contrast is $ratio", ratio >= 4.5f)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (max(firstLuminance, secondLuminance) + 0.05f) /
            (min(firstLuminance, secondLuminance) + 0.05f)
    }
}
