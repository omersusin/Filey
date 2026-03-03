package filey.app

import android.app.Application
import filey.app.di.AppContainer

class FileyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
