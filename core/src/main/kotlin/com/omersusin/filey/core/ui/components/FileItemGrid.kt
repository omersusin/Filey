package com.omersusin.filey.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.omersusin.filey.core.model.FileModel
import com.omersusin.filey.core.model.FileType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGrid(
    file: FileModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (file.type == FileType.IMAGE) {
                AsyncImage(
                    model = file.path,
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp)
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
                    contentDescription = file.name,
                    modifier = Modifier.size(48.dp),
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
