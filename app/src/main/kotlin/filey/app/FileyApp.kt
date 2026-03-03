package filey.app

import android.app.Application
import filey.app.core.data.RootManager

class FileyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        RootManager.init()
    }

    companion object {
        lateinit var instance: FileyApp
            private set
    }
}
