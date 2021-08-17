package com.ulsee.mower.data

import com.ulsee.mower.data.model.IotCoreShadowPayload
import com.ulsee.mower.data.model.IotCoreShadowPayloadState
import com.ulsee.mower.data.model.IotCoreShadowPayloadStateReported

data class MowerStatus(
    val x: Int,
    val y: Int,
    val angle: Float,
    val power: Int,
    val workingMode: Int,
    val errorCode: Int,
    val robotStatus: String?,
    val isCharging: Boolean,
    val isMowingStatus: Boolean,
    val interruptionCode: String?,
    val testingBoundaryState: Int,
    val signalQuality: Int
) {
    fun genPayload(): IotCoreShadowPayload {
        val currentState = 1
        val clientToken = System.currentTimeMillis() % 1000000

        val payloadReported = IotCoreShadowPayloadStateReported(
            currentState,
            this
        )
        val payloadState = IotCoreShadowPayloadState(payloadReported)
        val payload = IotCoreShadowPayload(payloadState, "$clientToken")
        return payload
    }
}
