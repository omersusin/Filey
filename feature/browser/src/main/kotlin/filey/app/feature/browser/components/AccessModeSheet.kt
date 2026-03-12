package filey.app.feature.browser.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.core.data.root.RootFileRepository
import filey.app.core.data.shizuku.ShizukuManager
import filey.app.core.model.AccessMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessModeSheet(
    currentMode: AccessMode,
    context: Context,
    onModeSelected: (AccessMode) -> Unit,
    onDismiss: () -> Unit
) {
    var rootAvailable by remember { mutableStateOf(false) }
    var shizukuInstalled by remember { mutableStateOf(false) }
    var shizukuRunning by remember { mutableStateOf(false) }
    var shizukuPermission by remember { mutableStateOf(false) }

    // Check availability on open
    LaunchedEffect(Unit) {
        rootAvailable = try { RootFileRepository.isRootAvailable() } catch (_: Exception) { false }
        shizukuInstalled = ShizukuManager.isInstalled(context)
        shizukuRunning = ShizukuManager.isServiceRunning()
        shizukuPermission = ShizukuManager.hasPermission()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Erişim Modu",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                "Normal: Standart dosya erişimi\nRoot: Tam sistem erişimi (root gerekli)\nShizuku: ADB seviyesi erişim",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Normal
            ListItem(
                modifier = Modifier.clickable { onModeSelected(AccessMode.NORMAL) },
                leadingContent = { Icon(Icons.Default.Folder, null) },
                headlineContent = { Text("Normal") },
                supportingContent = { Text("Standart dosya erişimi") },
                trailingContent = {
                    if (currentMode == AccessMode.NORMAL) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            // Root
            ListItem(
                modifier = Modifier.clickable(enabled = rootAvailable) {
                    if (rootAvailable) onModeSelected(AccessMode.ROOT)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Security, null,
                        tint = if (rootAvailable) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                headlineContent = { Text("Root") },
                supportingContent = {
                    Text(
                        if (rootAvailable) "Root erişimi mevcut"
                        else "Root erişimi bulunamadı"
                    )
                },
                trailingContent = {
                    if (currentMode == AccessMode.ROOT) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            // Shizuku
            val shizukuReady = shizukuInstalled && shizukuRunning && shizukuPermission
            ListItem(
                modifier = Modifier.clickable(enabled = shizukuReady) {
                    if (shizukuReady) onModeSelected(AccessMode.SHIZUKU)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Shield, null,
                        tint = if (shizukuReady) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                headlineContent = { Text("Shizuku") },
                supportingContent = {
                    Text(
                        when {
                            !shizukuInstalled -> "Shizuku yüklü değil"
                            !shizukuRunning -> "Shizuku servisi çalışmıyor"
                            !shizukuPermission -> "Shizuku izni verilmemiş"
                            else -> "Shizuku hazır"
                        }
                    )
                },
                trailingContent = {
                    when {
                        currentMode == AccessMode.SHIZUKU -> {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        !shizukuPermission && shizukuRunning && shizukuInstalled -> {
                            TextButton(onClick = { ShizukuManager.requestPermission(100) }) {
                                Text("İzin Ver")
                            }
                        }
                    }
                }
            )
        }
    }
}
