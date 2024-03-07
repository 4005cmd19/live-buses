package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.BusStopMetadata
import com.cmd.myapplication.data.adapters.MqttClientAdapter

class BusStopsRepository(
    remoteDataSource: MqttClientAdapter,
) : BusDataRepository<BusStop, BusStopMetadata>(
    remoteDataSource,
    BusStop::class.java,
    BusStopMetadata::class.java
) {
    override val dataTopicTemplate: String = "buses/stops/%s"
    override val metaTopicTemplate: String = "meta/stops"
}