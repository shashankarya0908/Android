package com.fable.app

import android.app.Application
import com.fable.app.di.FableDependencies

class FableApplication : Application() {
    lateinit var dependencies: FableDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        dependencies = FableDependencies(this)
    }
}
