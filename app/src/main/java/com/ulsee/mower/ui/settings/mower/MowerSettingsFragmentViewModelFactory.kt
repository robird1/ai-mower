package com.ulsee.mower.ui.settings.mower

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.data.DatabaseRepository

class MowerSettingsFragmentViewModelFactory(private val dbRepository: DatabaseRepository, private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MowerSettingsFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MowerSettingsFragmentViewModel(dbRepository, bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}