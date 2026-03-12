package filey.app.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import filey.app.feature.browser.BrowserUiState
import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import filey.app.core.model.FileUtils

@Composable
fun BrowserDrawerContent(
    uiState: BrowserUiState,
    onDashboard: () -> Unit,
    onTrash: () -> Unit,
    onServer: () -> Unit,
    onSettings: () -> Unit,
    onNavigate: (String) -> Unit,
    onFileClick: (FileModel) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Filey Explorer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Dosyalarınızı akıllıca yönetin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Main Destinations
            DrawerSectionHeader("Gezgin")
            
            NavigationDrawerItem(
                label = { Text("Ana Sayfa") },
                selected = false,
                onClick = onDashboard,
                icon = { Icon(Icons.Default.Dashboard, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Çöp Kutusu") },
                selected = false,
                onClick = onTrash,
                icon = { Icon(Icons.Default.Delete, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Dosya Paylaşımı") },
                selected = false,
                onClick = onServer,
                icon = { Icon(Icons.Default.Language, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            // Storage Summary
            uiState.storageInfo?.let { info ->
                DrawerStorageInfo(
                    used = FileUtils.formatSize(info.usedBytes),
                    total = FileUtils.formatSize(info.totalBytes),
                    progress = if (info.totalBytes > 0) info.usedBytes.toFloat() / info.totalBytes else 0f
                )
                Spacer(Modifier.height(16.dp))
            }

            // Favorites Section
            DrawerSectionHeader("Favoriler")
            if (uiState.favorites.isEmpty()) {
                DrawerEmptyHint("Henüz favori klasör yok")
            } else {
                uiState.favorites.toList().sorted().forEach { path ->
                    NavigationDrawerItem(
                        label = { Text(path.substringAfterLast('/')) },
                        selected = uiState.currentPath == path,
                        onClick = { onNavigate(path) },
                        icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recents Section
            DrawerSectionHeader("Son Kullanılanlar")
            if (uiState.recents.isEmpty()) {
                DrawerEmptyHint("Son kullanılan dosya yok")
            } else {
                uiState.recents.take(5).forEach { path ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                path.substringAfterLast('/'),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = false,
                        onClick = {
                            val name = path.substringAfterLast('/')
                            onFileClick(FileModel(name, path, isDirectory = false))
                        },
                        icon = {
                            Icon(
                                when (FileUtils.getFileType(path, false)) {
                                    FileType.IMAGE -> Icons.Outlined.Image
                                    FileType.VIDEO -> Icons.Outlined.Movie
                                    FileType.AUDIO -> Icons.Outlined.MusicNote
                                    FileType.PDF -> Icons.Outlined.PictureAsPdf
                                    else -> Icons.Outlined.InsertDriveFile
                                },
                                null
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            
            NavigationDrawerItem(
                label = { Text("Ayarlar") },
                selected = false,
                onClick = onSettings,
                icon = { Icon(Icons.Default.Settings, null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun DrawerEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
    )
}

@Composable
private fun DrawerStorageInfo(used: String, total: String, progress: Float) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hafıza Kullanımı", style = MaterialTheme.typography.labelMedium)
                Text("$used / $total", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
