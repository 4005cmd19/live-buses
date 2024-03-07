package com.cmd.myapplication.data.repositories

import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineMetadata
import com.cmd.myapplication.data.adapters.MqttClientAdapter

class BusLinesRepository(
    remoteDataSource: MqttClientAdapter,
) : BusDataRepository<BusLine, BusLineMetadata>(
    remoteDataSource,
    BusLine::class.java,
    BusLineMetadata::class.java
) {
    override val dataTopicTemplate = "buses/lines/%s"
    override val metaTopicTemplate = "meta/lines"
}