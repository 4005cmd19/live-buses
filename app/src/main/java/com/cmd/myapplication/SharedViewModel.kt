package com.cmd.myapplication

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class SharedViewModel : ViewModel() {
    private val componentStates = mutableMapOf<LifecycleOwner, Bundle>()

    fun saveState(owner: LifecycleOwner, state: Bundle) {
        componentStates.set(owner, state)
    }

    fun restoreState(owner: LifecycleOwner): Bundle? {
        return componentStates[owner]
    }

    companion object {
        const val TAG = "SharedViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SharedViewModel() as T
            }
        }
    }
}