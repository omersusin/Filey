package filey.app.feature.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import filey.app.core.data.preferences.AppPreferences
import filey.app.core.data.root.RootFileRepository
import filey.app.core.data.shizuku.ShizukuManager
import filey.app.core.model.AccessMode
import filey.app.core.model.SortOption
import filey.app.core.model.ViewMode
import filey.app.di.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppContainer.preferences }
    val scope = rememberCoroutineScope()

    val accessMode by preferences.accessModeFlow.collectAsState()
    val viewMode by preferences.viewModeFlow.collectAsState()
    val sortOption by preferences.sortOptionFlow.collectAsState()
    val showHidden by preferences.showHiddenFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Display section ──
            SectionHeader("Görünüm")

            ListItem(
                headlineContent = { Text("Görünüm modu") },
                supportingContent = {
                    Text(if (viewMode == ViewMode.LIST) "Liste" else "Grid")
                },
                leadingContent = { Icon(Icons.Default.ViewList, null) },
                modifier = Modifier.clickable {
                    scope.launch {
                        val newMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                        preferences.setViewMode(newMode)
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Gizli dosyaları göster") },
                leadingContent = {
                    Icon(
                        if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showHidden,
                        onCheckedChange = { scope.launch { preferences.setShowHidden(it) } }
                    )
                }
            )

            // Sort
            var showSortMenu by remember { mutableStateOf(false) }
            ListItem(
                headlineContent = { Text("Varsayılan sıralama") },
                supportingContent = { Text(sortOption.label) },
                leadingContent = { Icon(Icons.Default.Sort, null) },
                modifier = Modifier.clickable { showSortMenu = true }
            )
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            scope.launch { preferences.setSortOption(option) }
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (option == sortOption) Icon(Icons.Default.Check, null)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Access mode section ──
            SectionHeader("Erişim Modu")

            var rootAvailable by remember { mutableStateOf(false) }
            var shizukuReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                rootAvailable = try {
                    RootFileRepository.isRootAvailable()
                } catch (_: Exception) { false }

                shizukuReady = ShizukuManager.isInstalled(context) &&
                        ShizukuManager.isServiceRunning() &&
                        ShizukuManager.hasPermission()
            }

            AccessMode.entries.forEach { mode ->
                val enabled = when (mode) {
                    AccessMode.NORMAL -> true
                    AccessMode.ROOT -> rootAvailable
                    AccessMode.SHIZUKU -> shizukuReady
                }
                val subtitle = when (mode) {
                    AccessMode.NORMAL -> "Standart dosya erişimi"
                    AccessMode.ROOT -> if (rootAvailable) "Root erişimi mevcut" else "Root bulunamadı"
                    AccessMode.SHIZUKU -> if (shizukuReady) "Shizuku hazır" else "Shizuku kullanılamıyor"
                }

                ListItem(
                    headlineContent = { Text(mode.name) },
                    supportingContent = { Text(subtitle) },
                    leadingContent = {
                        RadioButton(
                            selected = accessMode == mode,
                            onClick = null,
                            enabled = enabled
                        )
                    },
                    modifier = Modifier.clickable(enabled = enabled) {
                        scope.launch { preferences.setAccessMode(mode) }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── About ──
            SectionHeader("Hakkında")

            ListItem(
                headlineContent = { Text("Filey") },
                supportingContent = { Text("Modern dosya yöneticisi • v1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
