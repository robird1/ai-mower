package com.ulsee.mower.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository

class ScheduleEditorFragmentViewModelFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleEditorFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleEditorFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}