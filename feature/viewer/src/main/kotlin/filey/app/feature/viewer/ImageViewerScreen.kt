package filey.app.feature.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import filey.app.core.model.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    path: String,
    onBack: () -> Unit
) {
    val file = remember { File(path) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(Icons.Default.Info, "Bilgi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset = Offset(
                            x = offset.x + pan.x,
                            y = offset.y + pan.y
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Info overlay
            if (showInfo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ad: ${file.name}", color = Color.White)
                        Text("Boyut: ${FileUtils.formatSize(file.length())}", color = Color.White)
                        Text("Yol: ${file.absolutePath}", color = Color.White)
                        Text(
                            "Tarih: ${FileUtils.formatDate(file.lastModified())}",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
