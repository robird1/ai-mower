package com.ulsee.mower.data

class BLECommandTable {
    companion object {
        const val VERIFICATION = 16
        const val STATUS = 80
    }
}

class BLEBroadcastAction {
    companion object {
        const val ACTION_CONNECT_FAILED = "action_connect_failed"
        const val ACTION_DEVICE_NOT_FOUND = "action_device_not_found"
        const val ACTION_GATT_CONNECTED = "action_gatt_connected"
        const val ACTION_GATT_DISCONNECTED = "action_gatt_disconnected"
        const val ACTION_GATT_NOT_SUCCESS = "action_gatt_not_success"
        const val ACTION_VERIFICATION_SUCCESS = "action_verification_success"
        const val ACTION_VERIFICATION_FAILED = "action_verification_failed"
        const val ACTION_STATUS_RESPONSE = "action_status_response"
    }
}