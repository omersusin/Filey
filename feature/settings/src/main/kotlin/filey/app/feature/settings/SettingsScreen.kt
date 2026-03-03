package filey.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import filey.app.core.di.AppContainer
import filey.app.core.model.SortOption
import filey.app.core.model.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppContainer.Instance.preferences }
    
    val showHidden by preferences.showHiddenFlow.collectAsState()
    val viewMode by preferences.viewModeFlow.collectAsState()
    val sortOption by preferences.sortOptionFlow.collectAsState()

    val scope = rememberCoroutineScope()

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
                .padding(16.dp)
        ) {
            Text(
                "Görünüm",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            ListItem(
                headlineContent = { Text("Gizli Dosyaları Göster") },
                trailingContent = {
                    Switch(
                        checked = showHidden,
                        onCheckedChange = { 
                            scope.launch {
                                preferences.setShowHidden(it)
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Dosya İşlemleri",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            ListItem(
                headlineContent = { Text("Varsayılan Görünüm") },
                supportingContent = { Text(if (viewMode == ViewMode.LIST) "Liste" else "Izgara") },
                trailingContent = {
                    TextButton(onClick = {
                        val next = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                        scope.launch { preferences.setViewMode(next) }
                    }) {
                        Text("Değiştir")
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Varsayılan Sıralama") },
                supportingContent = { Text(sortOption.label) },
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text("Seç")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        scope.launch {
                                            preferences.setSortOption(option)
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
