package filey.app.feature.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    path: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    var pageCount by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    // PdfRenderer setup
    val pdfRenderer = remember(file) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd)
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(pdfRenderer) {
        pageCount = pdfRenderer?.pageCount ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(file.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                        if (pageCount > 0) {
                            Text(
                                "Toplam $pageCount sayfa", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share PDF */ }) {
                        Icon(Icons.Default.Share, null)
                    }
                }
            )
        }
    ) { padding ->
        if (pdfRenderer == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("PDF dosyası açılamadı.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Gray.copy(alpha = 0.1f)),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageCount) { index ->
                    PdfPageItem(pdfRenderer, index)
                }
            }
        }
    }
}

@Composable
fun PdfPageItem(renderer: PdfRenderer, index: Int) {
    val bitmap = remember(index) {
        val page = renderer.openPage(index)
        // Adjust scale for better quality
        val width = page.width * 2
        val height = page.height * 2
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        bmp.asImageBitmap()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Sayfa ${index + 1}",
            modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
            contentScale = ContentScale.Fit
        )
    }
}
