package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.cmd.myapplication.data.BusStop
import com.google.gson.Gson

@SuppressLint("MissingPermission")
class BusStopsRepository(
    private val remoteDataSource: RemoteDataSource,
): Repository<BusStop>() {
    override fun request(stopIds: Array<String>, callback: (stopId: String, stop: BusStop) -> Unit) {
        for (stopId in stopIds) {
            remoteDataSource.listenTo("$TOPIC$stopId") { _, payload ->
                val stop = Gson().fromJson(payload.toString(), BusStop::class.java)

                callback(stopId, stop)
            }
        }
    }

    override fun requestAll(callback: (id: String, BusStop) -> Unit) = request(arrayOf("+"), callback)

    override fun requestOnce(ids: Array<String>, callback: (id: String, BusStop) -> Unit) {
        this.request(ids) { stopId, stop ->
            callback(stopId, stop)

            ignore(arrayOf(stopId))
        }
    }

    override fun ignore (stopIds: Array<String>) = stopIds.forEach { remoteDataSource.stopListeningTo(it) }

    override fun ignoreAll() = ignore(arrayOf("+"))

    companion object {
        const val TOPIC = "buses/stops/"
    }
}

