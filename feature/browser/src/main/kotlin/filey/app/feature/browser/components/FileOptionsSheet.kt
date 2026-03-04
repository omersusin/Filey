package filey.app.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils
import filey.app.feature.browser.actions.ActionResult
import filey.app.feature.browser.actions.FileAction
import filey.app.feature.browser.actions.FileActionCallback
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileOptionsSheet(
    file: FileModel,
    actions: List<FileAction>,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit,
    callback: FileActionCallback,
    onResult: (ActionResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFavorite = favorites.contains(file.path)

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

            // Toggle Favorite (Only for directories and not "^")
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
