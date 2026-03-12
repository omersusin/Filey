package filey.app.feature.smart.tags.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TagEntity::class, FileTagCrossRef::class], version = 1)
abstract class SmartTagDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
}
