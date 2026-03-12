package filey.app.feature.smart.tags.di

import android.content.Context
import androidx.room.Room
import filey.app.feature.smart.tags.data.db.SmartTagDatabase
import filey.app.feature.smart.tags.data.repository.SmartTagRepository

class SmartTagContainer(context: Context) {
    private val db = Room.databaseBuilder(
        context,
        SmartTagDatabase::class.java,
        "smart_tags.db"
    ).build()

    val repository = SmartTagRepository(db.tagDao())

    companion object {
        private var instance: SmartTagContainer? = null

        fun getInstance(context: Context): SmartTagContainer {
            return instance ?: synchronized(this) {
                instance ?: SmartTagContainer(context).also { instance = it }
            }
        }
    }
}
