package filey.app.feature.duplicates.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileUtils
import filey.app.feature.duplicates.engine.DuplicateEngine
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val engine = remember { DuplicateEngine() }
    
    var isScanning by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var duplicateGroups by remember { mutableStateOf<Map<String, List<File>>>(emptyMap()) }
    var selectedFiles by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kopya Dosya Bulucu") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (duplicateGroups.isNotEmpty() && !isScanning) {
                        TextButton(onClick = {
                            // "Her gruptan en az birini tut" mantığıyla otomatik seçim
                            val toSelect = mutableSetOf<String>()
                            duplicateGroups.values.forEach { group ->
                                // En eski dosyayı tutup diğerlerini seçelim (silinecekler)
                                val sorted = group.sortedBy { it.lastModified() }
                                sorted.drop(1).forEach { toSelect.add(it.absolutePath) }
                            }
                            selectedFiles = toSelect
                        }) {
                            Text("Otomatik Seç")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            selectedFiles.forEach { path -> File(path).delete() }
                            selectedFiles = emptySet()
                            // Yeniden tara
                            isScanning = true
                            duplicateGroups = engine.findDuplicates(
                                android.os.Environment.getExternalStorageDirectory().absolutePath
                            ) { progressMessage = it }
                            isScanning = false
                        }
                    },
                    icon = { Icon(Icons.Default.DeleteSweep, null) },
                    text = { Text("${selectedFiles.size} Dosyayı Temizle") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else if (!isScanning && duplicateGroups.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        isScanning = true
                        scope.launch {
                            duplicateGroups = engine.findDuplicates(
                                android.os.Environment.getExternalStorageDirectory().absolutePath
                            ) { progressMessage = it }
                            isScanning = false
                        }
                    },
                    icon = { Icon(Icons.Default.Search, null) },
                    text = { Text("Taramayı Başlat") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(progressMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (duplicateGroups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Taramayı başlatarak kopya dosyaları bulabilirsiniz.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(duplicateGroups.entries.toList()) { (hash, files) ->
                        DuplicateGroupCard(
                            files = files,
                            selectedFiles = selectedFiles,
                            onToggleSelection = { path ->
                                selectedFiles = if (selectedFiles.contains(path)) {
                                    selectedFiles - path
                                } else {
                                    selectedFiles + path
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateGroupCard(
    files: List<File>,
    selectedFiles: Set<String>,
    onToggleSelection: (String) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Grup: ${files[0].name}", 
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Boyut: ${FileUtils.formatSize(files[0].length())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(12.dp))
            
            files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedFiles.contains(file.absolutePath),
                        onCheckedChange = { onToggleSelection(file.absolutePath) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            file.absolutePath, 
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            FileUtils.formatDate(file.lastModified()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
