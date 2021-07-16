package com.ulsee.mower.ui.settings.mower

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository

class MowerSettingsExplosiveFragmentViewModelFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MowerSettingsExplosiveFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MowerSettingsExplosiveFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}