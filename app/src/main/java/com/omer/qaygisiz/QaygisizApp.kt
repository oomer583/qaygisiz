package com.omer.qaygisiz

import android.app.Application

class QaygisizApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
