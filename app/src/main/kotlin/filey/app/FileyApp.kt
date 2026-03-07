package filey.app

import android.app.Application

class FileyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // RootManager.init() KALDIRILDI
        // Root kontrolü lazy olarak ilk ihtiyaç anında yapılır
    }
}
