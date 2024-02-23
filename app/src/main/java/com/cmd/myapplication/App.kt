package com.cmd.myapplication

import android.app.Application
import com.cmd.myapplication.data.repositories.BusLinesRepository
import com.cmd.myapplication.data.repositories.BusRoutesRepository
import com.cmd.myapplication.data.repositories.BusStopsRepository
import com.cmd.myapplication.data.repositories.DeviceLocationRepository
import com.cmd.myapplication.data.repositories.RemoteDataSource
import com.cmd.myapplication.data.repositories.test.Api
import com.cmd.myapplication.data.repositories.test.TestDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class App : Application() {
    lateinit var deviceLocationRepository: DeviceLocationRepository

    lateinit var remoteDataSource: RemoteDataSource

    lateinit var busLinesRepository: BusLinesRepository
    lateinit var busStopsRepository: BusStopsRepository
    lateinit var busRoutesRepository: BusRoutesRepository

    override fun onCreate() {
        super.onCreate()

        deviceLocationRepository = DeviceLocationRepository(this)

        remoteDataSource = RemoteDataSource()

        busLinesRepository = BusLinesRepository(remoteDataSource)
        busStopsRepository = BusStopsRepository(remoteDataSource)
        busRoutesRepository = BusRoutesRepository(remoteDataSource)

        //supplyTestData()
    }

    private fun supplyTestData() {
        val provider = TestDataProvider()

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val (lines, stops, routes) = Api.getServiceData()
                provider.supplyData(lines, stops, routes)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}