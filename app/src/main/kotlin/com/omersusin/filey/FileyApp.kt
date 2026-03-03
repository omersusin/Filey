package com.omersusin.filey

import android.app.Application

class FileyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: FileyApp
            private set
    }
}
