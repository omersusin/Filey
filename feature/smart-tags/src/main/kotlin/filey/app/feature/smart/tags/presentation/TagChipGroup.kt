package filey.app.feature.smart.tags.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.feature.smart.tags.domain.model.SmartTag

@Composable
fun TagChipGroup(
    tags: List<SmartTag>,
    modifier: Modifier = Modifier,
    onTagClick: (SmartTag) -> Unit = {}
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            AssistChip(
                onClick = { onTagClick(tag) },
                label = { Text(tag.value) }
            )
        }
    }
}
