package com.cmd.myapplication

import android.app.Application
import com.cmd.myapplication.data.adapters.MqttClientAdapter
import com.cmd.myapplication.data.adapters.PlacesApiAdapter
import com.cmd.myapplication.data.repositories.BusLinesRepository
import com.cmd.myapplication.data.repositories.BusRoutesRepository
import com.cmd.myapplication.data.repositories.BusStopsRepository
import com.cmd.myapplication.data.repositories.DeviceLocationRepository
import com.cmd.myapplication.data.repositories.SearchRepository

class App : Application() {
    lateinit var deviceLocationRepository: DeviceLocationRepository

    lateinit var remoteDataSource: MqttClientAdapter
    lateinit var placesDataSource: PlacesApiAdapter

    lateinit var busLinesRepository: BusLinesRepository
    lateinit var busStopsRepository: BusStopsRepository
    lateinit var busRoutesRepository: BusRoutesRepository

    lateinit var searchRepository: SearchRepository

    override fun onCreate() {
        super.onCreate()

        deviceLocationRepository = DeviceLocationRepository(this)

        remoteDataSource = MqttClientAdapter()
        placesDataSource = PlacesApiAdapter(this)

        busLinesRepository = BusLinesRepository(remoteDataSource)
        busStopsRepository = BusStopsRepository(remoteDataSource)
        busRoutesRepository = BusRoutesRepository(remoteDataSource)

        searchRepository = SearchRepository(applicationContext, placesDataSource)
        searchRepository.loadCache()

//        with(TestDataProvider()) {
//            val (stops, lines, routes) = generateTestData()
//            publishToClient(TestClient(), stops, lines, routes)
//        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}