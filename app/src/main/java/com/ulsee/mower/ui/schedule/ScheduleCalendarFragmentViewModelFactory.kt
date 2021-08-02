package com.ulsee.mower.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository


class ScheduleCalendarFragmentViewModelFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleCalendarFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleCalendarFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}