package filey.app.feature.browser.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileModel
import filey.app.core.model.FileType
import filey.app.core.model.FileUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    ListItem(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(bgColor),
        leadingContent = {
            FileIcon(file = file, size = 40)
        },
        headlineContent = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (file.isDirectory) {
                    "${file.childCount} öğe"
                } else {
                    "${FileUtils.formatSize(file.size)} • ${FileUtils.formatDate(file.lastModified)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FileModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(bgColor)
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                FileIcon(file = file, size = 48)
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Seçili",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FileIcon(file: FileModel, size: Int) {
    val (icon, tint) = getFileIconAndColor(file)
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size.dp)
    )
}

@Composable
private fun getFileIconAndColor(file: FileModel): Pair<ImageVector, androidx.compose.ui.graphics.Color> {
    val type = FileUtils.getFileType(file.path, file.isDirectory)
    val cs = MaterialTheme.colorScheme
    return when (type) {
        FileType.DIRECTORY -> Icons.Default.Folder to cs.primary
        FileType.IMAGE -> Icons.Outlined.Image to cs.tertiary
        FileType.VIDEO -> Icons.Outlined.Movie to cs.error
        FileType.AUDIO -> Icons.Outlined.MusicNote to cs.secondary
        FileType.TEXT -> Icons.Outlined.Description to cs.onSurfaceVariant
        FileType.ARCHIVE -> Icons.Outlined.FolderZip to cs.tertiary
        FileType.APK -> Icons.Outlined.Android to cs.primary
        FileType.OTHER -> Icons.Outlined.InsertDriveFile to cs.onSurfaceVariant
    }
}
