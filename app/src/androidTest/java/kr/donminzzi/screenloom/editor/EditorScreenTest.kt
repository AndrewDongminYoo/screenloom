package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.ui.theme.ScreenloomTheme
import org.junit.Assert.assertEquals
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

        compose.onNodeWithText("Choose screenshots")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()
        compose.onNodeWithContentDescription("Promotional poster preview").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, chooseRequests) }
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

        compose.onNodeWithText("Split")
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Add a second screenshot to use Split",
                ),
            )
        compose.onNodeWithText("Export PNG").assertIsEnabled()
    }

    @Test
    fun copyFieldDispatchesTitleChanges() {
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

        compose.onNodeWithText("Copy").performClick()
        compose.onNodeWithTag("title-field").performTextInput("Ship something beautiful")

        compose.runOnIdle {
            assertEquals(EditorAction.SetTitle("Ship something beautiful"), actions.last())
        }
    }

    private fun oneImageState(): EditorUiState {
        val bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
        return EditorUiState(
            document = EditorDocument(imageCount = 1),
            images = listOf(ImportedImage(Uri.EMPTY, bitmap)),
        )
    }
}
