package com.cmd.myapplication.data.viewModels

//class BusRoutesViewModel(
//    private val repository: BusRoutesRepository,
//) : ViewModel() {
//
//    val busRoutes: LiveData<Set<BusLineRoutes>> by lazy { MutableLiveData(emptySet()) }
//
//    fun requestBusRoutes(lineIds: Array<String> = arrayOf("+")) {
//        val ids = lineIds.copyOf().toMutableSet()
//
//        repository.requestOnce(ids.toTypedArray()) { id, busRoutes ->
//            update(busRoutes)
//        }
//    }
//
//    private fun update(routes: BusLineRoutes) = viewModelScope.launch(Dispatchers.Main){
//        (busRoutes as MutableLiveData).apply {
//            value = value?.toMutableSet()?.apply {
//                add(routes)
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
//                return BusRoutesViewModel(application.busRoutesRepository) as T
//            }
//        }
//    }
//}