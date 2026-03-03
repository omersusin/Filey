package filey.app.di

import android.content.Context
import filey.app.core.data.DelegatingFileRepository
import filey.app.core.data.FileRepository
import filey.app.core.data.NormalFileRepository
import filey.app.core.data.preferences.AppPreferences
import filey.app.core.data.root.RootFileRepository
import filey.app.core.data.shizuku.ShizukuFileRepository

/**
 * Poor-man's DI — good enough for now.
 * Replace with Hilt later: @Module + @Provides + @Singleton.
 */
object AppContainer {

    lateinit var fileRepository: FileRepository
        private set

    lateinit var preferences: AppPreferences
        private set

    fun init(context: Context) {
        preferences = AppPreferences(context.applicationContext)

        val normalRepo = NormalFileRepository()
        val rootRepo = RootFileRepository()
        val shizukuRepo = ShizukuFileRepository()

        fileRepository = DelegatingFileRepository(
            modeFlow = preferences.accessModeFlow,
            normal = normalRepo,
            root = rootRepo,
            shizuku = shizukuRepo
        )
    }
}
