package filey.app.core.di

import android.content.Context
import filey.app.core.data.DelegatingFileRepository
import filey.app.core.data.FileRepository
import filey.app.core.data.NormalFileRepository
import filey.app.core.data.preferences.AppPreferences
import filey.app.core.data.root.RootFileRepository
import filey.app.core.data.shizuku.ShizukuFileRepository

/**
 * Dependency injection container for the app.
 */
class AppContainer(private val context: Context) {

    val preferences: AppPreferences by lazy {
        AppPreferences(context)
    }

    private val normalRepository: FileRepository by lazy {
        NormalFileRepository(context)
    }

    private val rootRepository: FileRepository by lazy {
        RootFileRepository()
    }

    private val shizukuRepository: FileRepository by lazy {
        ShizukuFileRepository()
    }

    val fileRepository: FileRepository by lazy {
        DelegatingFileRepository(
            modeFlow = preferences.accessModeFlow,
            normal = normalRepository,
            root = rootRepository,
            shizuku = shizukuRepository
        )
    }

    // ObjectBox Store for Vector Search and Metadata
    // Note: MyObjectBox is generated after the first build
    // val boxStore: io.objectbox.BoxStore by lazy {
    //    MyObjectBox.builder()
    //        .androidContext(context)
    //        .build()
    // }

    companion object {
        lateinit var Instance: AppContainer
            private set

        fun init(context: Context) {
            if (!::Instance.isInitialized) {
                Instance = AppContainer(context)
            }
        }
    }
}
