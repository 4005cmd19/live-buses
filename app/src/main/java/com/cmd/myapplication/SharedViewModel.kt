package com.cmd.myapplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SharedViewModel : ViewModel() {
    // bottom sheet properties
    val bottomSheetState: MutableLiveData<Int> by lazy { MutableLiveData(BottomSheetBehavior.STATE_COLLAPSED) }
    val bottomSheetOffset: MutableLiveData<Float> by lazy { MutableLiveData(0f) }
    val isBottomSheetDraggable: MutableLiveData<Boolean> by lazy { MutableLiveData(true) }

    // main fragment state properties
    val isMainBackPressedCallbackEnabled: MutableLiveData<Boolean> by lazy { MutableLiveData(true) }
    val searchBarHasFocus: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }
    val isInSearchMode: MutableLiveData<Boolean> by lazy { MutableLiveData(false) }

    companion object {
        const val TAG = "SharedViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SharedViewModel() as T
            }
        }
    }
}
