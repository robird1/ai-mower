package com.ulsee.mower.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
class SettingsFragmentViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsFragmentViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}