package kr.donminzzi.screenloom

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.Normalizer
import java.util.Locale
import kr.donminzzi.screenloom.editor.EditorScreen
import kr.donminzzi.screenloom.editor.EditorViewModel
import kr.donminzzi.screenloom.ui.theme.ScreenloomTheme

@Composable
fun ScreenloomApp(
    viewModel: EditorViewModel,
    onChooseImages: () -> Unit,
    onCreateDocument: (String) -> Unit,
    onSharePng: (Uri) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenloomTheme {
        EditorScreen(
            state = state,
            onChooseImages = onChooseImages,
            onRequestExport = { title -> onCreateDocument(posterFileName(title)) },
            onAction = viewModel::dispatch,
            onMessageConsumed = viewModel::consumeMessage,
            onSharePng = onSharePng,
            onCreateAnother = viewModel::createAnother,
            onUndoReset = viewModel::undoReset,
        )
    }
}

internal fun posterFileName(title: String): String {
    val normalizedSlug = Normalizer.normalize(title, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
        .trim('-')
    val endIndex = normalizedSlug.offsetByCodePoints(
        0,
        normalizedSlug.codePointCount(0, normalizedSlug.length).coerceAtMost(48),
    )
    val slug = normalizedSlug
        .substring(0, endIndex)
        .trimEnd('-')
    return if (slug.isBlank()) "screenloom-poster.png" else "$slug.png"
}
