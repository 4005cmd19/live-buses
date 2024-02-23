package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.cmd.myapplication.data.BusArrival
import com.google.gson.Gson

@SuppressLint("MissingPermission")
class BusArrivalsRepository(
    private val remoteDataSource: RemoteDataSource,
): Repository<BusArrival>() {
    override fun request(stopIds: Array<String>, callback: (stopId: String, arrivals: BusArrival) -> Unit) {
        for (stopId in stopIds) {
            remoteDataSource.listenTo("${BusStopsRepository.TOPIC}$stopId/arrivals") { _, payload ->
                val arrival = Gson().fromJson(payload.toString(), BusArrival::class.java)

                callback(stopId, arrival)
            }
        }
    }

    override fun requestOnce(ids: Array<String>, callback: (id: String, BusArrival) -> Unit) {
        this.request(ids) { stopId, arrivals ->
            callback(stopId, arrivals)

            ignore(arrayOf(stopId))
        }
    }

    override fun requestAll(callback: (id: String, BusArrival) -> Unit) = request(arrayOf("+"), callback)

    override fun ignore (stopIds: Array<String>) = stopIds.forEach { remoteDataSource.stopListeningTo(it) }

    override fun ignoreAll() = ignore(arrayOf("+"))
}

