package io.github.andrewdongminyoo.screenloom.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EditorReducerTest {
    @Test
    fun titleIsClampedToSixtyCharacters() {
        val result = EditorReducer.reduce(
            EditorDocument(),
            EditorAction.SetTitle("T".repeat(61)),
        )

        assertEquals("T".repeat(60), result.title)
    }

    @Test
    fun subtitleIsClampedToOneHundredCharacters() {
        val result = EditorReducer.reduce(
            EditorDocument(),
            EditorAction.SetSubtitle("S".repeat(101)),
        )

        assertEquals("S".repeat(100), result.subtitle)
    }

    @Test
    fun removingSecondImageFallsBackFromSplitToFocus() {
        val split = EditorDocument(imageCount = 2, layout = LayoutMode.Split)

        val result = EditorReducer.reduce(split, EditorAction.SetImageCount(1))

        assertEquals(LayoutMode.Focus, result.layout)
        assertFalse(result.canUseSplit)
    }

    @Test
    fun selectingSplitWithOneImageKeepsFocus() {
        val oneImage = EditorDocument(imageCount = 1)

        val result = EditorReducer.reduce(oneImage, EditorAction.SetLayout(LayoutMode.Split))

        assertEquals(LayoutMode.Focus, result.layout)
    }

    @Test
    fun resetKeepsImportedImagesAndRestoresVisualDefaults() {
        val edited = EditorDocument(
            imageCount = 2,
            layout = LayoutMode.Stack,
            title = "Launch better",
            palette = PaletteId.Coral,
            frameEnabled = false,
        )

        val result = EditorReducer.reduce(edited, EditorAction.Reset)

        assertEquals(EditorDocument(imageCount = 2), result)
    }

    @Test
    fun imageCountIsClampedToSupportedRange() {
        val result = EditorReducer.reduce(EditorDocument(), EditorAction.SetImageCount(8))

        assertEquals(2, result.imageCount)
    }
}
