package kr.donminzzi.screenloom

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kr.donminzzi.screenloom.editor.EditorStylePreferences
import kr.donminzzi.screenloom.editor.EditorViewModel
import kr.donminzzi.screenloom.media.ImageDecoder
import kr.donminzzi.screenloom.media.OutputStreamProvider
import kr.donminzzi.screenloom.media.PosterExporter
import kr.donminzzi.screenloom.render.PosterRenderer
import kr.donminzzi.screenloom.ui.theme.Ink
import kr.donminzzi.screenloom.ui.theme.Paper

class MainActivity : ComponentActivity() {
    private val editorViewModel: EditorViewModel by viewModels {
        // The ViewModel outlives configuration changes, so it must not capture the Activity's
        // ContentResolver: that would keep the first MainActivity reachable forever.
        val resolver = applicationContext.contentResolver
        val imageLoader = ImageDecoder(resolver)
        val posterWriter = PosterExporter(
            renderer = PosterRenderer(),
            outputStreamProvider = OutputStreamProvider(resolver::openOutputStream),
        )
        val stylePreferences = EditorStylePreferences(
            applicationContext.getSharedPreferences(EditorStylePreferences.Name, Context.MODE_PRIVATE),
        )
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(modelClass.isAssignableFrom(EditorViewModel::class.java))
                return EditorViewModel(
                    imageLoader = imageLoader,
                    posterWriter = posterWriter,
                    initialStyle = stylePreferences.load(),
                    onStyleChanged = stylePreferences::save,
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Paper.toArgb(), Ink.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Paper.toArgb(), Ink.toArgb()),
        )
        val imagePicker = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(2),
        ) { uris ->
            editorViewModel.import(uris)
        }
        val exportPicker = registerForActivityResult(
            ActivityResultContracts.CreateDocument("image/png"),
        ) { uri ->
            uri?.let(editorViewModel::export)
        }
        setContent {
            ScreenloomApp(
                viewModel = editorViewModel,
                onChooseImages = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onCreateDocument = exportPicker::launch,
                onSharePng = { uri ->
                    startActivity(
                        Intent.createChooser(createSharePngIntent(uri), getString(R.string.share_png)),
                    )
                },
            )
        }
    }
}

internal fun createSharePngIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newRawUri("Screenloom PNG", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
