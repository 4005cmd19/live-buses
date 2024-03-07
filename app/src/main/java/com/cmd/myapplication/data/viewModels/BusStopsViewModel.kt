package com.cmd.myapplication.data.viewModels

//class BusStopsViewModel(
//    private val repository: BusStopsRepository,
//) : ViewModel() {
//
//    val busStops: LiveData<Set<BusStop>> by lazy { MutableLiveData(emptySet()) }
//
//    fun requestBusStops(stopIds: Array<String> = arrayOf("+")) {
//        val ids = stopIds.copyOf()
//            .toMutableSet()
//            .toTypedArray()
//
//        repository.requestOnce(ids) { id, busStop ->
//            update(busStop)
//        }
//    }
//
//    private fun update(stop: BusStop) = viewModelScope.launch (Dispatchers.Main) {
//        (busStops as MutableLiveData).apply {
//            value = value?.toMutableSet()?.apply {
//                add(stop)
//            }
//        }
//    }
//
//    companion object {
//        const val TAG = "BusLinesViewModel"
//        const val PERIOD = 5000L
//
//        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
//            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
//                Log.e(TAG, "create")
//
//                val application =
//                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
//                return BusStopsViewModel(application.busStopsRepository) as T
//            }
//        }
//    }
//}