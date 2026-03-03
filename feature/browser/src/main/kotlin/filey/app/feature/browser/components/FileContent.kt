package filey.app.feature.browser.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileModel
import filey.app.core.model.ViewMode

@Composable
fun FileContent(
    files: List<FileModel>,
    viewMode: ViewMode,
    selectedFiles: Set<String>,
    isMultiSelectActive: Boolean,
    onFileClick: (FileModel) -> Unit,
    onFileLongClick: (FileModel) -> Unit,
    modifier: Modifier = Modifier
) {
    when (viewMode) {
        ViewMode.LIST -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(files, key = { it.path }) { file ->
                    FileListItem(
                        file = file,
                        isSelected = selectedFiles.contains(file.path),
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                    HorizontalDivider()
                }
            }
        }
        ViewMode.GRID -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                modifier = modifier.fillMaxSize()
            ) {
                items(files, key = { it.path }) { file ->
                    FileGridItem(
                        file = file,
                        isSelected = selectedFiles.contains(file.path),
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                }
            }
        }
    }
}
