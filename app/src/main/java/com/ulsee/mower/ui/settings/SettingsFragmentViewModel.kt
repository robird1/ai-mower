package com.ulsee.mower.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.model.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragmentViewModel() : ViewModel() {

    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
}