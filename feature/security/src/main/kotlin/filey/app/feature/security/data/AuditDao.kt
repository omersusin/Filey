package filey.app.feature.security.data

import androidx.room.*

@Entity(tableName = "audit_log")
data class AuditEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val commandType: String,
    val commandDetails: String,
    val riskLevel: String,
    val action: String,
    val reason: String = "",
    val executionTimeMs: Long = 0,
    val errorMessage: String? = null
)

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entry: AuditEntry)
}
