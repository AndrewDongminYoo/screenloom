package kr.donminzzi.screenloom.editor

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.ui.theme.ScreenloomTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyStateOffersScreenshotImportAndPosterPreview() {
        var chooseRequests = 0
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = EditorUiState(),
                    onChooseImages = { chooseRequests += 1 },
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.choose_screenshots))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        val previewDescription = previewDescription(compose.activity)
        compose.onNodeWithContentDescription(previewDescription).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, chooseRequests) }
    }

    @Test
    fun koreanLocaleRendersTranslatedEmptyStateAndDynamicPreviewDescription() {
        val configuration = Configuration(compose.activity.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("ko-KR"))
        }
        val koreanContext = compose.activity.createConfigurationContext(configuration)

        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides koreanContext,
                LocalConfiguration provides configuration,
            ) {
                ScreenloomTheme {
                    EditorScreen(
                        state = EditorUiState(),
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                    )
                }
            }
        }

        compose.onNodeWithText(koreanContext.getString(R.string.choose_screenshots)).assertIsDisplayed()
        compose.onNodeWithContentDescription(previewDescription(koreanContext)).assertIsDisplayed()
    }

    @Test
    fun splitIsDisabledWithOneImageAndExplainsWhy() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.layout_split))
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    string(R.string.split_unavailable),
                ),
            )
        compose.onNodeWithText(string(R.string.export_png)).assertIsEnabled()
    }

    @Test
    fun copyFieldDispatchesTitleChanges() {
        var state by mutableStateOf(oneImageState())
        val actions = mutableListOf<EditorAction>()
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = state,
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = { action ->
                        actions += action
                        state = state.copy(document = EditorReducer.reduce(state.document, action))
                    },
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.tab_copy)).performClick()
        compose.onNodeWithTag("title-field").performTextInput("Ship something beautiful")

        compose.runOnIdle {
            assertEquals(EditorAction.SetTitle("Ship something beautiful"), actions.last())
        }
    }

    @Test
    fun deviceFrameControlLivesUnderLayout() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.device_frame)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.tab_style)).performClick()
        compose.onNodeWithText(string(R.string.device_frame)).assertDoesNotExist()
    }

    @Test
    fun copyCounterCountsEmojiAsOneCharacter() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(
                        document = EditorDocument(
                            imageCount = 1,
                            title = "T".repeat(59) + "🚀",
                        ),
                    ),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.tab_copy)).performClick()
        val characterCounter = compose.activity.getString(R.string.character_counter, 60, 60)
        compose.onNodeWithText(characterCounter).assertIsDisplayed()
    }

    @Test
    fun deviceFrameSwitchHasAnAccessibleLabel() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        val deviceFrame = compose.activity.getString(R.string.device_frame)
        compose.onNodeWithContentDescription(deviceFrame).assertIsDisplayed()
    }

    @Test
    fun exportInProgressLocksCompositionChanges() {
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState().copy(isExporting = true),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = {},
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.tab_layout)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.replace)).assertIsNotEnabled()
        compose.onNodeWithText(string(R.string.reset)).assertIsNotEnabled()
    }

    @Test
    fun successfulExportOffersShareAndCreateAnotherActions() {
        val output = Uri.parse("content://screenloom/output")
        val state = oneImageState().copy(lastExportUri = output)
        var sharedUri: Uri? = null
        var createAnotherRequests = 0
        try {
            compose.setContent {
                ScreenloomTheme {
                    EditorScreen(
                        state = state,
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                        onSharePng = { sharedUri = it },
                        onCreateAnother = { createAnotherRequests += 1 },
                    )
                }
            }

            compose.onNodeWithText(string(R.string.share_png)).performScrollTo().performClick()
            compose.onNodeWithText(string(R.string.create_another)).performScrollTo().performClick()

            compose.runOnIdle {
                assertEquals(output, sharedUri)
                assertEquals(1, createAnotherRequests)
            }
        } finally {
            state.images.single().bitmap.recycle()
        }
    }

    @Test
    fun resetSnackbarUndoCallsItsHandler() {
        val state = oneImageState().copy(
            message = R.string.reset_complete,
            canUndoReset = true,
        )
        var undoRequests = 0
        try {
            compose.setContent {
                ScreenloomTheme {
                    EditorScreen(
                        state = state,
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                        onUndoReset = { undoRequests += 1 },
                    )
                }
            }

            compose.onNodeWithText(string(R.string.undo)).assertIsDisplayed().performClick()

            compose.runOnIdle { assertEquals(1, undoRequests) }
        } finally {
            state.images.single().bitmap.recycle()
        }
    }

    @Test
    fun tabsAndCurrentChoicesExposeSelectedSemantics() {
        var state by mutableStateOf(oneImageState())
        val source = state.images.single().bitmap
        try {
            compose.setContent {
                ScreenloomTheme {
                    EditorScreen(
                        state = state,
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                    )
                }
            }

            compose.onNodeWithText(string(R.string.tab_layout)).assertSelected()
            compose.onNodeWithText(string(R.string.layout_focus)).assertSelected()
            compose.onNodeWithText(string(R.string.tab_style)).performClick().assertSelected()
            compose.onNodeWithText(string(R.string.palette_ink)).assertSelected()
            compose.onNodeWithText(string(R.string.shadow_medium)).assertSelected()
        } finally {
            compose.runOnIdle { state = EditorUiState() }
            source.recycle()
        }
    }

    @Test
    fun selectedStyleTabAndItsControlsSurviveStateRestoration() {
        val restorationTester = StateRestorationTester(compose)
        var state by mutableStateOf(oneImageState())
        val source = state.images.single().bitmap
        try {
            restorationTester.setContent {
                ScreenloomTheme {
                    EditorScreen(
                        state = state,
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                    )
                }
            }
            compose.onNodeWithText(string(R.string.tab_style)).performClick().assertSelected()

            restorationTester.emulateSavedInstanceStateRestore()

            compose.onNodeWithText(string(R.string.tab_style)).assertSelected()
            compose.onNodeWithText(string(R.string.palette_ink)).performScrollTo().assertIsDisplayed()
        } finally {
            compose.runOnIdle { state = EditorUiState() }
            source.recycle()
        }
    }

    private fun oneImageState(
        document: EditorDocument = EditorDocument(imageCount = 1),
    ): EditorUiState {
        val bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        return EditorUiState(
            document = document,
            images = listOf(ImportedImage(Uri.EMPTY, bitmap)),
        )
    }

    @Test
    fun renamedPaletteLabelsDispatchTheStablePaletteIdentifiers() {
        val actions = mutableListOf<EditorAction>()
        compose.setContent {
            ScreenloomTheme {
                EditorScreen(
                    state = oneImageState(),
                    onChooseImages = {},
                    onRequestExport = {},
                    onAction = actions::add,
                    onMessageConsumed = {},
                )
            }
        }

        compose.onNodeWithText(string(R.string.tab_style)).performClick()
        compose.onNodeWithText(string(R.string.palette_ink)).assertIsDisplayed().assertSelected()
        compose.onNodeWithText(string(R.string.palette_moss)).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText(string(R.string.palette_violet)).performScrollTo().assertIsDisplayed().performClick()

        compose.runOnIdle {
            assertEquals(EditorAction.SetPalette(PaletteId.Moss), actions[actions.lastIndex - 1])
            assertEquals(EditorAction.SetPalette(PaletteId.Violet), actions.last())
        }
    }

    @Test
    fun metadataAndSectionLabelsUseLegibleForegroundsOnTheirRenderedSurfaces() {
        val state = oneImageState()
        try {
            compose.setContent {
                ScreenloomTheme {
                    EditorScreen(
                        state = state,
                        onChooseImages = {},
                        onRequestExport = {},
                        onAction = {},
                        onMessageConsumed = {},
                    )
                }
            }

            compose.onNodeWithText(string(R.string.one_frame_loaded))
                .assertRenderedForegroundHasPerimeterContrast(0xFF18213D.toInt())
            compose.onNodeWithText(string(R.string.layout_section_label))
                .assertRenderedForegroundHasPerimeterContrast(0xFF18213D.toInt())
        } finally {
            state.images.single().bitmap.recycle()
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertSelected() = assert(
        SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
    )

    private fun string(@StringRes resourceId: Int): String = compose.activity.getString(resourceId)

    private fun previewDescription(context: Context): String = context.getString(
        R.string.poster_preview_description,
        context.getString(R.string.layout_stack),
        context.resources.getQuantityString(R.plurals.poster_preview_screenshot_count, 2, 2),
        context.getString(R.string.palette_ink),
    )

    private fun SemanticsNodeInteraction.assertRenderedForegroundHasPerimeterContrast(foreground: Int) {
        val pixels = captureToImage().toPixelMap()
        val foregroundExists = (0 until pixels.height).any { y ->
            (0 until pixels.width).any { x ->
                val pixel = pixels[x, y]
                pixel.alpha >= 0.99f && colorMatches(pixel.red, pixel.green, pixel.blue, foreground)
            }
        }
        assertTrue("Expected rendered foreground $foreground", foregroundExists)

        val perimeterColors = listOf(
            pixels[0, 0],
            pixels[pixels.width - 1, 0],
            pixels[0, pixels.height - 1],
            pixels[pixels.width - 1, pixels.height - 1],
        ).filter { it.alpha >= 0.99f }
        assertTrue("Expected opaque line-box perimeter pixels", perimeterColors.isNotEmpty())
        perimeterColors.forEach { background ->
            val ratio = contrastRatio(foreground, rgb(background.red, background.green, background.blue))
            assertTrue("Foreground $foreground contrast is $ratio", ratio >= 4.5)
        }
    }

    private fun colorMatches(red: Float, green: Float, blue: Float, target: Int): Boolean =
        abs(red * 255 - (target ushr 16 and 0xFF)) <= 4f &&
            abs(green * 255 - (target ushr 8 and 0xFF)) <= 4f &&
            abs(blue * 255 - (target and 0xFF)) <= 4f

    private fun rgb(red: Float, green: Float, blue: Float): Int =
        (0xFF shl 24) or
            ((red * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8) or
            (blue * 255).toInt()

    private fun contrastRatio(first: Int, second: Int): Double {
        val firstLuminance = relativeLuminance(first)
        val secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05) /
            (min(firstLuminance, secondLuminance) + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        fun component(shift: Int): Double {
            val encoded = ((color ushr shift) and 0xFF) / 255.0
            return if (encoded <= 0.04045) encoded / 12.92 else ((encoded + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * component(16) + 0.7152 * component(8) + 0.0722 * component(0)
    }
}
