package com.ulsee.mower.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.ble.BluetoothLeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScheduleCalendarFragmentViewModel(private val bleRepository: BluetoothLeRepository) : ViewModel() {

    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
    private var mSchedules = MutableLiveData<List<String>>()
    val schedules : LiveData<List<String>>
        get() = mSchedules


    fun getSchedule() {
        mIsLoading.value = true
        viewModelScope.launch {
            // todo: fetch settings

            delay(500)
            mSchedules.value = arrayListOf("a")

            mIsLoading.value = false
        }
    }
}