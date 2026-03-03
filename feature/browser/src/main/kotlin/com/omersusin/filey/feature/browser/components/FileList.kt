package com.omersusin.filey.feature.browser.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.omersusin.filey.core.model.FileModel
import com.omersusin.filey.core.ui.components.FileItemRow

@Composable
fun FileList(
    files: List<FileModel>,
    onClick: (FileModel) -> Unit,
    onLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(files, key = { _, f -> f.path }) { index, file ->
            FileItemRow(
                file = file,
                onClick = { onClick(file) },
                onLongClick = { onLongClick(index) }
            )
            if (index < files.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
