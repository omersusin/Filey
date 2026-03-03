package filey.app.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import filey.app.core.model.PathSegment

@Composable
fun PathBar(
    segments: List<PathSegment>,
    onSegmentClick: (PathSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to end when segments change
    LaunchedEffect(segments) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, segment ->
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                val isLast = index == segments.lastIndex
                Text(
                    text = segment.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLast) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onSegmentClick(segment) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
