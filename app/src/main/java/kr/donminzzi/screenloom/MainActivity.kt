package kr.donminzzi.screenloom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kr.donminzzi.screenloom.editor.EditorViewModel
import kr.donminzzi.screenloom.media.ImageDecoder
import kr.donminzzi.screenloom.media.OutputStreamProvider
import kr.donminzzi.screenloom.media.PosterExporter
import kr.donminzzi.screenloom.render.PosterRenderer

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
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(modelClass.isAssignableFrom(EditorViewModel::class.java))
                return EditorViewModel(imageLoader, posterWriter) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
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
            )
        }
    }
}
