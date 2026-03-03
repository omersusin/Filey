package com.omersusin.filey.feature.browser.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omersusin.filey.core.model.FileModel
import com.omersusin.filey.core.ui.components.FileItemGrid

@Composable
fun FileGrid(
    files: List<FileModel>,
    onClick: (FileModel) -> Unit,
    onLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        itemsIndexed(files, key = { _, f -> f.path }) { index, file ->
            FileItemGrid(
                file = file,
                onClick = { onClick(file) },
                onLongClick = { onLongClick(index) }
            )
        }
    }
}
