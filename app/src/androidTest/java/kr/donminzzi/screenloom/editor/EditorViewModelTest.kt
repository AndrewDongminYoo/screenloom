package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.media.ExportResult
import kr.donminzzi.screenloom.media.ImageLoader
import kr.donminzzi.screenloom.media.PosterWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorViewModelTest {
    @Test
    fun importUsesOnlyTheFirstTwoSelections() = runBlocking {
        val first = Uri.parse("content://screenloom/first")
        val second = Uri.parse("content://screenloom/second")
        val third = Uri.parse("content://screenloom/third")
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ -> Result.success(testBitmap()) },
        )

        viewModel.import(listOf(first, second, third))
        val state = viewModel.awaitState { !it.isImporting && it.images.size == 2 }

        assertEquals(listOf(first, second), state.images.map(ImportedImage::uri))
        assertEquals(2, state.document.imageCount)
    }

    @Test
    fun failedReplacementPreservesTheActiveComposition() = runBlocking {
        val first = Uri.parse("content://screenloom/first")
        val broken = Uri.parse("content://screenloom/broken")
        val bitmap = testBitmap()
        val loader = ImageLoader { uri, _ ->
            if (uri == broken) Result.failure(IllegalArgumentException("broken")) else Result.success(bitmap)
        }
        val viewModel = editorViewModel(loader)
        viewModel.import(listOf(first))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }
        viewModel.dispatch(EditorAction.SetTitle("Keep this composition"))

        viewModel.import(listOf(broken))
        val state = viewModel.awaitState { !it.isImporting && it.message != null }

        assertEquals("Keep this composition", state.document.title)
        assertSame(bitmap, state.images.single().bitmap)
        assertFalse(bitmap.isRecycled)
        assertEquals("Unable to read that image", state.message)
    }

    @Test
    fun successfulExportPublishesConfirmationAndClearsBusyState() = runBlocking {
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ -> Result.success(testBitmap()) },
            writer = PosterWriter { _, _, _ -> ExportResult.Success },
        )
        viewModel.import(listOf(Uri.parse("content://screenloom/first")))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }

        viewModel.export(Uri.parse("content://screenloom/output"))
        val state = viewModel.awaitState { !it.isExporting && it.message == "PNG saved" }

        assertEquals("PNG saved", state.message)
    }

    @Test
    fun clearingViewModelRecyclesImportedBitmaps() = runBlocking {
        val bitmap = testBitmap()
        val viewModel = editorViewModel(ImageLoader { _, _ -> Result.success(bitmap) })
        viewModel.import(listOf(Uri.parse("content://screenloom/first")))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }
        val store = ViewModelStore()
        store.put("editor", viewModel)

        store.clear()

        assertTrue(bitmap.isRecycled)
    }

    private fun editorViewModel(
        loader: ImageLoader,
        writer: PosterWriter = PosterWriter { _, _, _ -> ExportResult.Success },
    ): EditorViewModel = EditorViewModel(loader, writer)

    private fun testBitmap(): Bitmap = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888)

    private suspend fun EditorViewModel.awaitState(
        predicate: (EditorUiState) -> Boolean,
    ): EditorUiState = withTimeout(5_000) {
        while (!predicate(state.value)) {
            delay(10)
        }
        state.value
    }
}
