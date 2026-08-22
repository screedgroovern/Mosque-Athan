package com.groovernapp.mosqueathan

import android.app.Application
import com.groovernapp.mosqueathan.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MosqueAthanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MosqueAthanApplication)
            modules(appModule)
        }
    }
}
