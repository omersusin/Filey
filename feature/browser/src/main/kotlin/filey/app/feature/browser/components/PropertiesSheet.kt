package filey.app.feature.browser.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.core.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesSheet(
    file: FileModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var md5 by remember { mutableStateOf<String?>(null) }
    var sha256 by remember { mutableStateOf<String?>(null) }
    var ownerApp by remember { mutableStateOf<String?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    val repository = remember { AppContainer.Instance.fileRepository }

    LaunchedEffect(file.path) {
        ownerApp = withContext(Dispatchers.IO) { repository.getOwnerApp(file.path) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("Dosya Özellikleri", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            PropertyRow("Ad", file.name)
            PropertyRow("Yol", file.path)
            PropertyRow("Tür", if (file.isDirectory) "Klasör" else file.mimeType)
            
            ownerApp?.let {
                PropertyRow("Sahiplik", it)
            }

            if (!file.isDirectory) {
                PropertyRow("Boyut", FileUtils.formatSize(file.size))
                PropertyRow("Uzantı", file.extension.ifEmpty { "-" })

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text("Özet Bilgileri (Checksum)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                if (md5 == null && !isCalculating) {
                    Button(
                        onClick = {
                            isCalculating = true
                            scope.launch {
                                val m = withContext(Dispatchers.IO) { FileUtils.calculateHash(file.path, "MD5") }
                                val s = withContext(Dispatchers.IO) { FileUtils.calculateHash(file.path, "SHA-256") }
                                md5 = m
                                sha256 = s
                                isCalculating = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hesapla (MD5 & SHA-256)")
                    }
                } else if (isCalculating) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    HashRow("MD5", md5 ?: "Hata", context)
                    HashRow("SHA-256", sha256 ?: "Hata", context)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            PropertyRow("Son değişiklik", FileUtils.formatDate(file.lastModified))
            if (file.permissions.isNotBlank()) {
                PropertyRow("İzinler", file.permissions)
            }
            if (file.owner.isNotBlank()) {
                PropertyRow("Sahip", file.owner)
            }
            PropertyRow("Gizli", if (file.isHidden) "Evet" else "Hayır")
        }
    }
}

@Composable
private fun HashRow(label: String, value: String, context: Context) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(label, value))
            }
        ) {
            Text(text = value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
