package com.cmd.myapplication.data.viewModels

//class BusLinesViewModel(
//    private val repository: BusLinesRepository,
//) : ViewModel() {
//
//    val busLines: LiveData<Set<BusLine>> by lazy { MutableLiveData(emptySet()) }
//
//    fun requestBusLines(lineIds: Array<String> = arrayOf("+")) {
//        val ids = lineIds.copyOf().toMutableSet()
//
//        repository.requestOnce(ids.toTypedArray()) { id, busLine ->
//            update(busLine)
//        }
//    }
//
//    private fun update(line: BusLine) = viewModelScope.launch (Dispatchers.Main) {
//        (busLines as MutableLiveData).apply {
//            value = value?.toMutableSet()?.apply {
//                add(line)
//            }
//        }
//    }
//
//    companion object {
//        const val TAG = "BusLinesViewModel"
//
//        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
//            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
//                Log.e(TAG, "create")
//
//                val application =
//                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
//                return BusLinesViewModel(application.busLinesRepository) as T
//            }
//        }
//    }
//}