package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.BusArrival
import com.cmd.myapplication.data.BusArrivalMetadata
import com.cmd.myapplication.data.adapters.MqttClientAdapter

class BusArrivalsRepository(
    remoteDataSource: MqttClientAdapter,
) : BusDataRepository<BusArrival, BusArrivalMetadata>(
    remoteDataSource,
    BusArrival::class.java,
    BusArrivalMetadata::class.java
) {
    override val dataTopicTemplate: String
        get() = "buses/stops/%s/arrivals"

    override val metaTopicTemplate: String
        get() = "meta/stops"
}