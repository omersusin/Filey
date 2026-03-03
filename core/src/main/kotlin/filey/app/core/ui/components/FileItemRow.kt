package filey.app.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import filey.app.core.model.FileModel
import filey.app.core.model.FileType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(text = file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = "${file.sizeFormatted} • ${file.dateFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            if (file.type == FileType.IMAGE) {
                AsyncImage(
                    model = file.path,
                    contentDescription = file.name,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    imageVector = when (file.type) {
                        FileType.DIRECTORY -> Icons.Default.Folder
                        FileType.VIDEO -> Icons.Default.VideoFile
                        FileType.AUDIO -> Icons.Default.AudioFile
                        FileType.ARCHIVE -> Icons.Default.FolderZip
                        FileType.TEXT -> Icons.Default.Description
                        FileType.APK -> Icons.Default.Android
                        FileType.PDF -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    )
}
