package filey.app.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.feature.browser.actions.ActionResult
import filey.app.feature.browser.actions.FileAction
import filey.app.feature.browser.actions.FileActionCallback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileOptionsSheet(
    file: FileModel,
    actions: List<FileAction>,
    favorites: Set<String>,
    shelf: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onToggleShelf: (String) -> Unit,
    onToggleTag: (String, String) -> Unit,
    callback: FileActionCallback,
    onResult: (ActionResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFavorite = favorites.contains(file.path)
    val isInShelf = shelf.contains(file.path)

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // File header
            ListItem(
                leadingContent = { FileIcon(file = file, size = 40) },
                headlineContent = {
                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    if (file.name == "^") {
                        Text("Üst dizin")
                    } else if (file.isDirectory) {
                        Text("${file.childCount} öğe")
                    } else {
                        Text(FileUtils.formatSize(file.size))
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Toggle Favorite (Directories)
            if (file.isDirectory && file.name != "^") {
                ListItem(
                    modifier = Modifier.clickable {
                        onToggleFavorite(file.path)
                        onDismiss()
                    },
                    leadingContent = {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    },
                    headlineContent = {
                        Text(if (isFavorite) "Favorilerden Çıkar" else "Favorilere Ekle")
                    }
                )
            }

            // Toggle Shelf (Files)
            if (!file.isDirectory && file.name != "^") {
                ListItem(
                    modifier = Modifier.clickable {
                        onToggleShelf(file.path)
                        onDismiss()
                    },
                    leadingContent = {
                        Icon(
                            if (isInShelf) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = if (isInShelf) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    },
                    headlineContent = {
                        Text(if (isInShelf) "Raftan Çıkar" else "Rafa Ekle")
                    }
                )
            }

            // Tags (Only if not "^")
            if (file.name != "^") {
                Text(
                    "Etiketler",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("İş", "Önemli", "Okul", "Kişisel", "Acil").forEach { tag ->
                        FilterChip(
                            selected = file.tags.contains(tag),
                            onClick = { onToggleTag(file.path, tag) },
                            label = { Text(tag) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Actions
            actions.forEach { action ->
                ListItem(
                    modifier = Modifier.clickable {
                        scope.launch {
                            val result = action.execute(context, file, callback)
                            onResult(result)
                        }
                    },
                    leadingContent = {
                        Icon(action.icon, contentDescription = action.title)
                    },
                    headlineContent = { Text(action.title) }
                )
            }
        }
    }
}
