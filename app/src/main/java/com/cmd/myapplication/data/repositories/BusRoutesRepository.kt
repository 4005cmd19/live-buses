package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusLineRoutesMetadata
import com.cmd.myapplication.data.adapters.MqttClientAdapter

class BusRoutesRepository(
    remoteDataSource: MqttClientAdapter,
) : BusDataRepository<BusLineRoutes, BusLineRoutesMetadata>(
    remoteDataSource,
    BusLineRoutes::class.java,
    BusLineRoutesMetadata::class.java
) {
    override val dataTopicTemplate: String
        get() = "buses/lines/%s/routes"

    override val metaTopicTemplate: String
        get() = "meta/lines"
}