package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.media.ExportResult
import kr.donminzzi.screenloom.media.ImageLoader
import kr.donminzzi.screenloom.media.PosterWriter
import kotlinx.coroutines.CompletableDeferred
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
    fun overlappingImportRequestIsIgnoredWhileTheFirstDecodeIsActive() = runBlocking {
        val first = Uri.parse("content://screenloom/first")
        val second = Uri.parse("content://screenloom/second")
        val decodeStarted = CompletableDeferred<Unit>()
        val firstRelease = CompletableDeferred<Unit>()
        val decodedUris = mutableListOf<Uri>()
        val viewModel = editorViewModel(
            loader = ImageLoader { uri, _ ->
                decodedUris += uri
                if (uri == first) {
                    decodeStarted.complete(Unit)
                    firstRelease.await()
                }
                Result.success(testBitmap())
            },
        )

        viewModel.import(listOf(first))
        decodeStarted.await()
        viewModel.import(listOf(second))

        assertEquals(listOf(first), decodedUris)
        firstRelease.complete(Unit)
        val state = viewModel.awaitState { !it.isImporting && it.images.size == 1 }
        assertEquals(first, state.images.single().uri)
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
        assertEquals(R.string.import_failure, state.message)
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
        val state = viewModel.awaitState { !it.isExporting && it.message != null }

        assertEquals(R.string.export_success, state.message)
    }

    @Test
    fun emptyExportPublishesResourceBackedGuidance() {
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ -> Result.success(testBitmap()) },
        )

        viewModel.export(Uri.parse("content://screenloom/output"))

        assertEquals(R.string.empty_export, viewModel.state.value.message)
    }

    @Test
    fun failedExportPublishesResourceBackedError() = runBlocking {
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ -> Result.success(testBitmap()) },
            writer = PosterWriter { _, _, _ -> ExportResult.Failure(R.string.import_failure) },
        )
        viewModel.import(listOf(Uri.parse("content://screenloom/first")))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }

        viewModel.export(Uri.parse("content://screenloom/output"))
        val state = viewModel.awaitState { !it.isExporting && it.message != null }

        assertEquals(R.string.import_failure, state.message)
    }

    @Test
    fun importIsIgnoredWhileExportUsesTheActiveImages() = runBlocking {
        val first = Uri.parse("content://screenloom/first")
        val second = Uri.parse("content://screenloom/second")
        val exportRelease = CompletableDeferred<Unit>()
        var decodeCount = 0
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ ->
                decodeCount += 1
                Result.success(testBitmap())
            },
            writer = PosterWriter { _, _, _ ->
                exportRelease.await()
                ExportResult.Success
            },
        )
        viewModel.import(listOf(first))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }
        viewModel.export(Uri.parse("content://screenloom/output"))
        viewModel.awaitState { it.isExporting }

        viewModel.import(listOf(second))
        delay(100)

        assertEquals(1, decodeCount)
        assertEquals(first, viewModel.state.value.images.single().uri)
        assertTrue(viewModel.state.value.isExporting)
        exportRelease.complete(Unit)
        viewModel.awaitState { !it.isExporting }
        Unit
    }

    @Test
    fun exportIsIgnoredWhileReplacementImportIsActive() = runBlocking {
        val first = Uri.parse("content://screenloom/first")
        val second = Uri.parse("content://screenloom/second")
        val importRelease = CompletableDeferred<Unit>()
        var exportCount = 0
        val viewModel = editorViewModel(
            loader = ImageLoader { uri, _ ->
                if (uri == second) importRelease.await()
                Result.success(testBitmap())
            },
            writer = PosterWriter { _, _, _ ->
                exportCount += 1
                ExportResult.Success
            },
        )
        viewModel.import(listOf(first))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }

        viewModel.import(listOf(second))
        viewModel.awaitState { it.isImporting }
        viewModel.export(Uri.parse("content://screenloom/output"))
        delay(100)

        assertEquals(0, exportCount)
        assertTrue(viewModel.state.value.isImporting)
        assertFalse(viewModel.state.value.isExporting)
        importRelease.complete(Unit)
        viewModel.awaitState { !it.isImporting && it.images.single().uri == second }
        Unit
    }

    @Test
    fun repeatedExportRequestsStartOnlyOneWriter() = runBlocking {
        val exportRelease = CompletableDeferred<Unit>()
        var exportCount = 0
        val viewModel = editorViewModel(
            loader = ImageLoader { _, _ -> Result.success(testBitmap()) },
            writer = PosterWriter { _, _, _ ->
                exportCount += 1
                exportRelease.await()
                ExportResult.Success
            },
        )
        viewModel.import(listOf(Uri.parse("content://screenloom/first")))
        viewModel.awaitState { !it.isImporting && it.images.size == 1 }

        viewModel.export(Uri.parse("content://screenloom/first-output"))
        viewModel.export(Uri.parse("content://screenloom/second-output"))
        viewModel.awaitState { it.isExporting }
        delay(100)

        assertEquals(1, exportCount)
        exportRelease.complete(Unit)
        viewModel.awaitState { !it.isExporting }
        Unit
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
