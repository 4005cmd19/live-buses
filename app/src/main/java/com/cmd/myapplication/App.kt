package com.cmd.myapplication

import android.app.Application
import com.cmd.myapplication.data.repositories.DeviceLocationRepository

class App: Application() {
    lateinit var deviceLocationRepository: DeviceLocationRepository

    override fun onCreate() {
        super.onCreate()

        deviceLocationRepository = DeviceLocationRepository(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}