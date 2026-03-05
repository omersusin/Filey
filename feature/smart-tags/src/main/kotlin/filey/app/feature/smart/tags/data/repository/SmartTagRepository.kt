package filey.app.feature.smart.tags.data.repository

import filey.app.feature.smart.tags.data.db.TagDao
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource
import kotlinx.serialization.json.Json

class SmartTagRepository(
    private val tagDao: TagDao
) {
    suspend fun saveTags(filePath: String, tags: List<SmartTag>) {
        tagDao.tagFile(filePath, tags)
    }

    suspend fun getTagsForFile(filePath: String): List<SmartTag> {
        return tagDao.getTagsForFile(filePath).map { entity ->
            SmartTag(
                value = entity.value,
                category = TagCategory.valueOf(entity.category),
                confidence = entity.confidence,
                source = TagSource.valueOf(entity.source),
                metadata = try {
                    Json.decodeFromString(entity.metadataJson)
                } catch (e: Exception) {
                    emptyMap()
                }
            )
        }
    }

    suspend fun isTagged(filePath: String): Boolean {
        return tagDao.getTagCountForFile(filePath) > 0
    }

    suspend fun needsRetagging(filePath: String, fileLastModified: Long): Boolean {
        val taggedAt = tagDao.getTagTimeForFile(filePath) ?: return true
        return taggedAt < fileLastModified
    }

    suspend fun searchFilesByTag(query: String, category: TagCategory? = null): List<String> {
        return tagDao.searchByTag(query, category?.name ?: "")
    }
}
