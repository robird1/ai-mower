package com.ulsee.mower.ui.settings.mower

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.model.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MowerSettingsFragmentViewModel(private val bleRepository: BluetoothLeRepository) : ViewModel() {

    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
    private var mSettings = MutableLiveData<List<Device>>()
    val settings : LiveData<List<Device>>
        get() = mSettings


    fun getSettings() {
        mIsLoading.value = true
        viewModelScope.launch {
            // todo: fetch settings
            delay(500)
            mIsLoading.value = false
        }
    }
}