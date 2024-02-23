package com.cmd.myapplication.data.repositories

import android.annotation.SuppressLint
import com.cmd.myapplication.data.BusLine
import com.google.gson.Gson

@SuppressLint("MissingPermission")
class BusLinesRepository(
    private val remoteDataSource: RemoteDataSource,
): Repository<BusLine>() {
    override fun request(lineIds: Array<String>, callback: (lineId: String, line: BusLine) -> Unit) {
        for (lineId in lineIds) {
            remoteDataSource.listenTo("$TOPIC$lineId") { _, payload ->
                val line = Gson().fromJson(payload.toString(), BusLine::class.java)

                callback(lineId, line)
            }
        }
    }

    override fun requestOnce(ids: Array<String>, callback: (id: String, BusLine) -> Unit) {
        this.request(ids) { lineId, line ->
            callback(lineId, line)

            ignore(arrayOf(lineId))
        }
    }

    override fun requestAll(callback: (id: String, BusLine) -> Unit) = request(arrayOf("+"), callback)

    override fun ignore (lineIds: Array<String>) = lineIds.forEach { remoteDataSource.stopListeningTo(it) }

    override fun ignoreAll() = ignore(arrayOf("+"))

    companion object {
        const val TOPIC = "buses/lines/"
    }
}

