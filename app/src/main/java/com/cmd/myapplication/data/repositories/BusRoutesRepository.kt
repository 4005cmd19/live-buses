package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.cmd.myapplication.data.BusLineRoutes
import com.google.gson.Gson

/*
MAX_MQTT: 268,435,456 b

buses/routes/{id}
buses/nearby/

 */

@SuppressLint("MissingPermission")
class BusRoutesRepository(
    private val remoteDataSource: RemoteDataSource
): Repository<BusLineRoutes>() {
    override fun request(lineIds: Array<String>, callback: (lineId: String, routes: BusLineRoutes) -> Unit) {
        for (lineId in lineIds) {
            remoteDataSource.listenTo("${BusLinesRepository.TOPIC}$lineId/routes") { _, payload ->
                val routes = Gson().fromJson(payload.toString(), BusLineRoutes::class.java)

                callback(lineId, routes)
            }
        }
    }

    override fun requestAll(callback: (id: String, BusLineRoutes) -> Unit) = request(arrayOf("+"), callback)

    override fun ignore(lineIds: Array<String>) = lineIds.forEach { remoteDataSource.stopListeningTo(it) }

    override fun requestOnce(ids: Array<String>, callback: (id: String, BusLineRoutes) -> Unit) {
        this.request(ids) { lineId, routes ->
            callback(lineId, routes)

            ignore(arrayOf(lineId))
        }
    }

    override fun ignoreAll() = ignore(arrayOf("+"))
}