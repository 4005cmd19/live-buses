package com.cmd.myapplication.data.repositories.unused

//@SuppressLint("MissingPermission")
//class BusArrivalsRepository(
//    private val remoteDataSource: MqttClientAdapter,
//): Repository<BusArrival>() {
//    override fun request(stopIds: Array<String>, callback: RequestCallback<BusArrival>) {
//        for (stopId in stopIds) {
//            remoteDataSource.listenTo("${BusStopsRepository.DATA_TOPIC}$stopId/arrivals") { _, payload ->
//                val arrival = Gson().fromJson(payload.toString(), BusArrival::class.java)
//
//                callback.onReceived(stopId, arrival)
//            }
//        }
//    }
//
//    override fun requestOnce(ids: Array<String>, callback: RequestCallback<BusArrival>) {
//        this.request(ids) { stopId, arrivals ->
//            callback.onReceived(stopId, arrivals)
//
//            ignore(arrayOf(stopId))
//        }
//    }
//
//    override fun requestAll(callback: RequestCallback<BusArrival>) = request(arrayOf("+"), callback)
//
//    override fun ignore (stopIds: Array<String>) = stopIds.forEach { remoteDataSource.stopListeningTo(it) }
//
//    override fun ignoreAll() = ignore(arrayOf("+"))
//}
//
