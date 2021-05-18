package com.ulsee.mower

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes

private val TAG = Utils::class.java.simpleName

class Utils {
    companion object {
        const val REQUEST_LOCATION_SETTINGS = 5678

        fun checkLocationSetting(activity: Activity) {
            val mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000)

            val settingsBuilder = LocationSettingsRequest.Builder().addLocationRequest(
                mLocationRequest
            )
            settingsBuilder.setAlwaysShow(true)

            val result = LocationServices.getSettingsClient(activity).checkLocationSettings(
                settingsBuilder.build()
            )

            result.addOnCompleteListener {
                try {
                    val response = it.getResult(ApiException::class.java)
                    Log.d(TAG, "response: " + response.toString())

                } catch (ex: ApiException) {
                    when (ex.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.d(
                                TAG,
                                "Location settings are not satisfied. Show the user a dialog to upgrade location settings."
                            )
                            try {
                                val resolvableApiException = ex as ResolvableApiException
                                resolvableApiException.startResolutionForResult(
                                    activity,
                                    REQUEST_LOCATION_SETTINGS
                                )

                            } catch (e: IntentSender.SendIntentException) {
                                Log.d(TAG, "PendingIntent unable to execute request.")
//                                Toast.makeText(this, "Failed to switch Wi-Fi...", Toast.LENGTH_LONG).show()
//                                finish()
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            Log.d(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.")
//                            Toast.makeText(this, "Failed to switch Wi-Fi...", Toast.LENGTH_LONG).show()
//                            finish()
                        }
                    }
                }
            }

        }
    }
}