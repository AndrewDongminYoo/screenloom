package kr.donminzzi.screenloom.render

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.editor.EditorDocument
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosterPreviewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun previewDisplaysCompositionCopy() {
        compose.setContent {
            PosterPreview(
                document = EditorDocument(
                    title = "Ship beautifully",
                    subtitle = "Store-ready visuals in seconds.",
                ),
                images = emptyList(),
            )
        }

        compose.onNodeWithText("Ship beautifully").assertIsDisplayed()
        compose.onNodeWithText("Store-ready visuals in seconds.").assertIsDisplayed()
    }
}
