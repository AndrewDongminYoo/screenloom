package kr.donminzzi.screenloom.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.donminzzi.screenloom.R
import kr.donminzzi.screenloom.render.PosterPreview
import kr.donminzzi.screenloom.ui.theme.Cobalt
import kr.donminzzi.screenloom.ui.theme.Coral

@Composable
fun EditorScreen(
    state: EditorUiState,
    onChooseImages: () -> Unit,
    onRequestExport: (String) -> Unit,
    onAction: (EditorAction) -> Unit,
    onMessageConsumed: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageConsumed()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StudioBackdrop()
            if (state.images.isEmpty()) {
                EmptyState(
                    isImporting = state.isImporting,
                    onChooseImages = onChooseImages,
                )
            } else {
                EditorWorkspace(
                    state = state,
                    onChooseImages = onChooseImages,
                    onRequestExport = onRequestExport,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    isImporting: Boolean,
    onChooseImages: () -> Unit,
) {
    val sampleImages = remember { createSampleImages() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StudioHeader(sequence = stringResource(R.string.sample_sequence))
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier
                .width(166.dp)
                .shadow(32.dp, RoundedCornerShape(28.dp)),
            color = Color.Transparent,
            shape = RoundedCornerShape(28.dp),
        ) {
            PosterPreview(
                document = EditorDocument(
                    imageCount = 2,
                    layout = LayoutMode.Stack,
                    title = stringResource(R.string.sample_poster_title),
                    subtitle = stringResource(R.string.sample_poster_subtitle),
                    palette = PaletteId.Coral,
                ),
                images = sampleImages,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp)),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.empty_headline),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.empty_body),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onChooseImages,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = !isImporting,
            shape = RoundedCornerShape(18.dp),
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.importing))
            } else {
                Text(stringResource(R.string.choose_screenshots))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.privacy_note),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EditorWorkspace(
    state: EditorUiState,
    onChooseImages: () -> Unit,
    onRequestExport: (String) -> Unit,
    onAction: (EditorAction) -> Unit,
) {
    val editorEnabled = !state.isImporting && !state.isExporting
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StudioHeader(
            sequence = stringResource(
                if (state.images.size == 1) R.string.one_frame_loaded else R.string.two_frames_loaded,
            ),
        )
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier
                .widthIn(max = 246.dp)
                .fillMaxWidth(0.68f)
                .shadow(26.dp, RoundedCornerShape(24.dp)),
            color = Color.Transparent,
            shape = RoundedCornerShape(24.dp),
        ) {
            PosterPreview(
                document = state.document,
                images = state.images.map { it.bitmap.asImageBitmap() },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
            )
        }
        Spacer(Modifier.height(22.dp))
        EditorControls(
            document = state.document,
            enabled = editorEnabled,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onChooseImages,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                enabled = editorEnabled,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.replace))
            }
            OutlinedButton(
                onClick = { onAction(EditorAction.Reset) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                enabled = editorEnabled,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.reset))
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onRequestExport(state.document.title) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp),
            enabled = editorEnabled,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
            if (state.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.exporting))
            } else {
                Text(stringResource(R.string.export_png))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StudioHeader(sequence: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.wordmark),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = sequence,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.8.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun StudioBackdrop() {
    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.025f)
        val step = size.width / 6f
        repeat(12) { index ->
            val x = (index - 3) * step
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x + size.height * 0.28f, size.height),
                strokeWidth = 1f,
            )
        }
        drawCircle(
            color = Cobalt.copy(alpha = 0.07f),
            radius = size.width * 0.48f,
            center = Offset(size.width * 1.02f, size.height * 0.08f),
        )
        drawCircle(
            color = Coral.copy(alpha = 0.045f),
            radius = size.width * 0.4f,
            center = Offset(-size.width * 0.08f, size.height * 0.72f),
        )
    }
}

private fun createSampleImages(): List<ImageBitmap> = listOf(
    createSampleImage(AndroidColor.rgb(91, 124, 250), AndroidColor.rgb(20, 24, 42)),
    createSampleImage(AndroidColor.rgb(255, 122, 110), AndroidColor.rgb(44, 22, 34)),
)

private fun createSampleImage(startColor: Int, endColor: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, 320f, 640f, startColor, endColor, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f, 0f, 320f, 640f, paint)
    paint.shader = null
    paint.color = AndroidColor.argb(225, 245, 241, 232)
    canvas.drawRoundRect(28f, 34f, 212f, 58f, 12f, 12f, paint)
    paint.color = AndroidColor.argb(90, 245, 241, 232)
    canvas.drawRoundRect(28f, 76f, 274f, 92f, 8f, 8f, paint)
    repeat(3) { index ->
        val top = 136f + index * 126f
        paint.color = AndroidColor.argb(44 + index * 12, 255, 255, 255)
        canvas.drawRoundRect(24f, top, 296f, top + 96f, 22f, 22f, paint)
    }
    return bitmap.asImageBitmap()
}
