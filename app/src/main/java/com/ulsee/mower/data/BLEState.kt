package com.ulsee.mower.data

import android.content.Context
import android.content.Intent

abstract class BLEState {
    abstract fun getAction(): String
    abstract fun getNextState(): BLEState

    fun onCharacteristicChanged(context: Context, message: String = "") {
        val intent = Intent(getAction())
        if (message.isNotEmpty()) {
            intent.putExtra("message", message)
        }
        context.sendBroadcast(intent)
    }

}


class RobotListState: BLEState() {
    companion object {
        const val ACTION_VERIFICATION_SUCCESS = "action_verification_success"
    }

    override fun getAction() = ACTION_VERIFICATION_SUCCESS
    override fun getNextState() = RobotStatusState()

}


class RobotStatusState: BLEState() {
    companion object {
        const val ACTION_STATUS_RESPONSE = "action_status_response"
    }

    override fun getAction() = ACTION_STATUS_RESPONSE
    override fun getNextState(): BLEState {
//        TODO("Not yet implemented")
        return this
    }

}