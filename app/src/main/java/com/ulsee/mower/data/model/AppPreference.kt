package com.ulsee.mower.data.model

import android.content.SharedPreferences

private const val FIRST_ADD_DEVICE = "first_add_device"

class AppPreference (private val prefs: SharedPreferences) {

    fun getFirstAddDevice(): Boolean {
        return prefs.getBoolean(FIRST_ADD_DEVICE, true)
    }

    fun setFirstAddDevice() {
        prefs.edit().putBoolean(FIRST_ADD_DEVICE, false).apply()
    }

}