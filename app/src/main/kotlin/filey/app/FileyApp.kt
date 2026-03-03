package filey.app

import android.app.Application

class FileyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        // RootManager.init() KALDIRILDI
        // Root kontrolü lazy olarak ilk ihtiyaç anında yapılır
    }

    companion object {
        lateinit var instance: FileyApp
            private set
    }
}
