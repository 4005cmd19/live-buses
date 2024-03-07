package com.cmd.myapplication.data.repositories.unused

//import android.annotation.SuppressLint
//import com.cmd.myapplication.data.BusLineRoutes
//import com.google.gson.Gson
//
///*
//MAX_MQTT: 268,435,456 b
//
//buses/routes/{id}
//buses/nearby/
//
// */
//
//@SuppressLint("MissingPermission")
//class BusRoutesRepository(
//    private val remoteDataSource: RemoteDataSource
//): Repository<BusLineRoutes>() {
//    override fun request(lineIds: Array<String>, callback: RequestCallback<BusLineRoutes>) {
//        for (lineId in lineIds) {
//            remoteDataSource.listenTo("${BusLinesRepository.TOPIC}$lineId/routes") { _, payload ->
//                val routes = Gson().fromJson(String(payload), BusLineRoutes::class.java)
//
//                callback.onReceived(lineId, routes)
//            }
//        }
//    }
//
//    override fun requestAll(callback: RequestCallback<BusLineRoutes>) = request(arrayOf("+"), callback)
//
//    override fun ignore(lineIds: Array<String>) = lineIds.forEach { remoteDataSource.stopListeningTo(it) }
//
//    override fun requestOnce(ids: Array<String>, callback: RequestCallback<BusLineRoutes>) {
//        this.request(ids) { lineId, routes ->
//            callback.onReceived(lineId, routes)
//
//            ignore(arrayOf(lineId))
//        }
//    }
//
//    override fun ignoreAll() = ignore(arrayOf("+"))
//}