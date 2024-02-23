package com.cmd.myapplication.data.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.cmd.myapplication.App
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.repositories.BusLinesRepository

class BusLinesViewModel(
    private val repository: BusLinesRepository,
) : ViewModel() {

    val busLines: LiveData<Set<BusLine>> by lazy { MutableLiveData(emptySet()) }

    fun requestBusLines(lineIds: Array<String> = emptyArray()) {
        val ids = lineIds.copyOf().toMutableSet()

        if (lineIds.isEmpty()) {
            ids.add("+")
        }

        val lines = mutableSetOf<BusLine>()

        repository.requestOnce(ids.toTypedArray()) { id, busLine ->
            lines.add(busLine)

            ids.remove(id)
            if (ids.isEmpty()) {
                update(lines)
            }
        }
    }

    fun debugSet(lines: Set<BusLine>) {
        update(lines)
    }

    private fun update(lines: Set<BusLine>) {
        (busLines as MutableLiveData).value = lines
    }

    companion object {
        const val TAG = "BusLinesViewModel"
        const val PERIOD = 5000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                Log.e(TAG, "create")

                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as App
                return BusLinesViewModel(application.busLinesRepository) as T
            }
        }
    }
}