package com.androidvip.ddgui

import android.app.Application
import com.androidvip.ddgui.data.di.dataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DDGuiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DDGuiApp)
            modules(dataModule)
        }
    }
}
