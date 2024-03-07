package com.cmd.myapplication.data.repositories.unused

//import android.annotation.SuppressLint
//import com.cmd.myapplication.data.BusLine
//import com.google.gson.Gson
//
//@SuppressLint("MissingPermission")
//class BusLinesRepository(
//    private val remoteDataSource: RemoteDataSource,
//): Repository<BusLine>() {
//    override fun request(lineIds: Array<String>, callback: RequestCallback<BusLine>) {
//        for (lineId in lineIds) {
//            remoteDataSource.listenTo("$TOPIC$lineId") { _, payload ->
//                val line = Gson().fromJson(String(payload), BusLine::class.java)
//
//                callback.onReceived(lineId, line)
//            }
//        }
//    }
//
//    override fun requestOnce(ids: Array<String>, callback: RequestCallback<BusLine>) {
//        this.request(ids) { lineId, line ->
//            callback.onReceived(lineId, line)
//
//            ignore(arrayOf(lineId))
//        }
//    }
//
//    override fun requestAll(callback: RequestCallback<BusLine>) = request(arrayOf("+"), callback)
//
//    override fun ignore (lineIds: Array<String>) = lineIds.forEach { remoteDataSource.stopListeningTo(it) }
//
//    override fun ignoreAll() = ignore(arrayOf("+"))
//
//    companion object {
//        const val TOPIC = "buses/lines/"
//    }
//}
//
