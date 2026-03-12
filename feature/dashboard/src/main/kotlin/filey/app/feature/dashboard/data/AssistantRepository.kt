package filey.app.feature.dashboard.data

import filey.app.feature.dashboard.domain.FileInsight
import kotlinx.coroutines.flow.Flow

interface AssistantRepository {
    fun getInsights(): Flow<List<FileInsight>>
}
