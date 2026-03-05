package filey.app.feature.smart.tags.data.db

import androidx.room.*
import filey.app.feature.smart.tags.domain.model.SmartTag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Dao
interface TagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: FileTagCrossRef)
    
    @Transaction
    suspend fun tagFile(filePath: String, tags: List<SmartTag>) {
        // Clear old tags
        deleteTagsForFile(filePath)
        
        tags.forEach { tag ->
            val tagId = insertTag(
                TagEntity(
                    value = tag.value,
                    category = tag.category.name,
                    confidence = tag.confidence,
                    source = tag.source.name,
                    metadataJson = Json.encodeToString(tag.metadata)
                )
            )
            insertCrossRef(FileTagCrossRef(filePath, tagId))
        }
    }
    
    @Query("""
        SELECT ftc.filePath 
        FROM file_tag_cross_ref ftc
        INNER JOIN smart_tags st ON ftc.tagId = st.id
        WHERE st.value LIKE '%' || :query || '%'
        OR st.category = :category
        ORDER BY st.confidence DESC
    """)
    suspend fun searchByTag(
        query: String, 
        category: String = ""
    ): List<String>
    
    @Query("""
        SELECT st.* FROM smart_tags st
        INNER JOIN file_tag_cross_ref ftc ON st.id = ftc.tagId
        WHERE ftc.filePath = :filePath
        ORDER BY st.confidence DESC
    """)
    suspend fun getTagsForFile(filePath: String): List<TagEntity>
    
    @Query("""
        SELECT st.value, st.category, COUNT(*) as count
        FROM smart_tags st
        INNER JOIN file_tag_cross_ref ftc ON st.id = ftc.tagId
        GROUP BY st.value
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getPopularTags(limit: Int = 20): List<TagSummary>
    
    @Query("DELETE FROM file_tag_cross_ref WHERE filePath = :filePath")
    suspend fun deleteTagsForFile(filePath: String)

    @Query("SELECT COUNT(*) FROM file_tag_cross_ref WHERE filePath = :filePath")
    suspend fun getTagCountForFile(filePath: String): Int

    @Query("SELECT taggedAt FROM file_tag_cross_ref WHERE filePath = :filePath LIMIT 1")
    suspend fun getTagTimeForFile(filePath: String): Long?
}

data class TagSummary(
    val value: String,
    val category: String,
    val count: Int
)
