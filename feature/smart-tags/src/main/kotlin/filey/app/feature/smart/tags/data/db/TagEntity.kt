package filey.app.feature.smart.tags.data.db

import androidx.room.*

@Entity(tableName = "smart_tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: String,
    val category: String,
    val confidence: Float,
    val source: String,
    val metadataJson: String = "{}"
)

@Entity(
    tableName = "file_tag_cross_ref",
    primaryKeys = ["filePath", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FileTagCrossRef(
    val filePath: String,
    val tagId: Long,
    val taggedAt: Long = System.currentTimeMillis()
)
