package filey.app.feature.security.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AuditEntry::class], version = 1, exportSchema = false)
abstract class AuditDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    companion object {
        @Volatile private var INSTANCE: AuditDatabase? = null
        fun getInstance(context: Context): AuditDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AuditDatabase::class.java, "audit.db"
                ).build().also { INSTANCE = it }
            }
    }
}
