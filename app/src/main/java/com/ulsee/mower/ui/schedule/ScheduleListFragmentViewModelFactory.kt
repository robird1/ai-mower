package com.ulsee.mower.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository

class ScheduleListFragmentViewModelFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleListFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleListFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}