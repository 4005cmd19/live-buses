package com.cmd.myapplication.data.repositories.unused

//@SuppressLint("MissingPermission")
//class BusStopsRepository(
//    private val remoteDataSource: RemoteDataSource,
//): Repository<BusStop>() {
//    override fun request(stopIds: Array<String>, callback: RequestCallback<BusStop>) {
//        for (stopId in stopIds) {
//            remoteDataSource.listenTo("$TOPIC$stopId") { _, payload ->
//                val stop = Gson().fromJson(String(payload), BusStop::class.java)
//
//                callback.onReceived(stopId, stop)
//            }
//        }
//    }
//
//    override fun requestAll(callback: RequestCallback<BusStop>) = request(arrayOf("+"), callback)
//
//    override fun requestOnce(ids: Array<String>, callback: RequestCallback<BusStop>) {
//        this.request(ids) { stopId, stop ->
//            callback.onReceived(stopId, stop)
//
//            ignore(arrayOf(stopId))
//        }
//    }
//
//    override fun ignore (stopIds: Array<String>) = stopIds.forEach { remoteDataSource.stopListeningTo(it) }
//
//    override fun ignoreAll() = ignore(arrayOf("+"))
//
//    companion object {
//        const val TOPIC = "buses/stops/"
//    }
//}

