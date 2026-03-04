package filey.app.feature.organizer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import filey.app.feature.organizer.engine.OrganizerEngine
import filey.app.feature.organizer.model.OrganizerRule
import kotlinx.coroutines.launch
import filey.app.feature.organizer.worker.OrganizerWorkManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { OrganizerEngine() }

    var rules by remember { mutableStateOf(engine.getDefaultRules()) }
    var isOrganizing by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Simulation of persistent state (should be in DataStore normally)
    var isBackgroundWorkEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Otomatik Düzenleyici") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Kural Ekle")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background Task Switch
            ListItem(
                headlineContent = { Text("Otomatik Temizlik (Arka Plan)") },
                supportingContent = { Text("İndirilenleri her 12 saatte bir otomatik düzenle") },
                leadingContent = { Icon(Icons.Default.AutoMode, null) },
                trailingContent = {
                    Switch(
                        checked = isBackgroundWorkEnabled,
                        onCheckedChange = { enabled ->
                            isBackgroundWorkEnabled = enabled
                            if (enabled) {
                                OrganizerWorkManager.schedulePeriodicWork(context)
                            } else {
                                OrganizerWorkManager.cancelWork(context)
                            }
                        }
                    )
                }
            )

            HorizontalDivider()

            if (isOrganizing) {

                            val downloadPath = android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS
                            ).absolutePath
                            
                            val count = engine.organizeFolder(downloadPath, rules) { msg ->
                                progressMessage = msg
                            }
                            
                            isOrganizing = false
                            progressMessage = "$count dosya başarıyla taşındı."
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("İndirilenleri Düzenle") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isOrganizing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = progressMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (progressMessage.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = progressMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = "Aktif Kurallar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(rules) { rule ->
                    RuleItem(
                        rule = rule,
                        onDelete = { rules = rules.filter { it.id != rule.id } },
                        onToggle = { active ->
                            rules = rules.map { if (it.id == rule.id) it.copy(isActive = active) else it }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { ext, target ->
                rules = rules + OrganizerRule(extension = ext, targetPath = target)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleItem(
    rule: OrganizerRule,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(".${rule.extension.uppercase()}") },
        supportingContent = { Text(rule.targetPath, maxLines = 1) },
        leadingContent = {
            Icon(
                imageVector = when(rule.extension.lowercase()) {
                    "pdf", "doc", "docx" -> Icons.Default.Description
                    "jpg", "png", "jpeg" -> Icons.Default.Image
                    "mp4", "mkv" -> Icons.Default.VideoFile
                    "apk" -> Icons.Default.Android
                    else -> Icons.Default.FolderOpen
                },
                contentDescription = null
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = rule.isActive, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var ext by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Kural") },
        text = {
            Column {
                TextField(
                    value = ext,
                    onValueChange = { ext = it },
                    label = { Text("Uzantı (örn: pdf)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Hedef Klasör Yolu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (ext.isNotEmpty() && target.isNotEmpty()) onAdd(ext, target) },
                enabled = ext.isNotEmpty() && target.isNotEmpty()
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
