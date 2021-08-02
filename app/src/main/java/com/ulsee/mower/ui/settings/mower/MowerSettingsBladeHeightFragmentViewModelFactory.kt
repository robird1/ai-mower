package com.ulsee.mower.ui.settings.mower

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.ble.BluetoothLeRepository

class MowerSettingsBladeHeightFragmentViewModelFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MowerSettingsBladeHeightFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MowerSettingsBladeHeightFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}