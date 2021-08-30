package com.ulsee.mower.data.model

import com.google.gson.annotations.SerializedName
import com.ulsee.mower.data.MowerStatus
import com.ulsee.mower.data.Status
import com.ulsee.mower.ui.settings.mower.MowerSettings
import com.ulsee.mower.ui.settings.mower.MowerWorkingMode

data class IotCoreShadowPayload(
    var state: IotCoreShadowPayloadState,
    var clientToken: String
) {
    companion object {
        fun createStatus(status: MowerStatus): IotCoreShadowPayload {
            val clientToken = System.currentTimeMillis() % 1000000

            // state
            var state: ReportedState? = ReportedState.setting
            if (status.errorCode>0) { // error
                state = ReportedState.error
            } else if ((status.robotStatus?.indexOf('1') ?: 0) >= 0) { // robotStatus emergency stop
                state = ReportedState.error
            } else if ((status.interruptionCode?.indexOf('1') ?: 0) >= 0) { // robotStatus emergency stop
                state = ReportedState.error
            } else if (status.isCharging) {
                state = ReportedState.charging
            } else if (status.workingMode == Status.WorkingMode.SUSPEND_WORKING_MODE) {
                state = ReportedState.working
            } else if (status.workingMode == Status.WorkingMode.WORKING_MODE) {
                state = ReportedState.working
            } else if (status.workingMode == Status.WorkingMode.LEARNING_MODE) {
                state = ReportedState.working
            }
            // action
            var action: ReportedAction? = ReportedAction.other
            if (state == ReportedState.working) {
                action = if(status.workingMode == Status.WorkingMode.SUSPEND_WORKING_MODE) ReportedAction.pause else ReportedAction.go
            }

            val payloadReported = IotCoreShadowPayloadStateReported(
                state = state,
                action = action,
                battery = status.power,
                workingpercentage = if(state == ReportedState.working) status.workingPercentage else 0,
            )
            val payloadState = IotCoreShadowPayloadState(payloadReported, null)
            val payload = IotCoreShadowPayload(payloadState, "$clientToken")
            return payload
        }

        fun createSettings(settings: MowerSettings): IotCoreShadowPayload {
            val clientToken = System.currentTimeMillis() % 1000000

            val payloadReported = IotCoreShadowPayloadStateReported(
                mode = when(settings.workingMode) {
                    MowerWorkingMode.learning -> DesiredMode.learning
                    MowerWorkingMode.working -> DesiredMode.working
                    MowerWorkingMode.learnAndWork -> DesiredMode.learnthenwork
                    MowerWorkingMode.gradual -> DesiredMode.gradual
                    MowerWorkingMode.explosive -> DesiredMode.explosive
                    else -> DesiredMode.unknown
                },
                blade = settings.knifeHeight,
                rainmode = if(settings.rainMode == 1) DesiredBool.yes else DesiredBool.no,
            )
            if (settings.mowerCount >= 0)payloadReported.mapnumber = settings.mowerCount
            val payloadState = IotCoreShadowPayloadState(payloadReported, null)
            val payload = IotCoreShadowPayload(payloadState, "$clientToken")
            return payload
        }
    }
}

data class IotCoreShadowPayloadState (
    val reported: IotCoreShadowPayloadStateReported?,
    val desired: IotCoreShadowPayloadStateDesired?
)

data class IotCoreShadowPayloadStateReported(
    var state: ReportedState? = null,
    val mode: DesiredMode? = null,
    val blade: Int? = null,
    val action: ReportedAction? = null,
    val rainmode: DesiredBool? = null,
    val battery: Int? = null,
    var mapnumber: Int? = null,
    var workingpercentage: Int? = null,
    /*
    "lastremove": 1~64,
    "state": "normal/error/charging/disconnect",
    "requiredtime": 0-9999,
    "worktime": 0-9999
     */
)
data class IotCoreShadowPayloadStateDesired(
    val mode: DesiredMode?,
    val blade: Int?,
    val action: DesiredAction?,
    val rainmode: DesiredBool?,
//    val learningmode: DesiredBool?,
//    val lastremove: Int?
)

enum class DesiredMode(val value: String) {
    @SerializedName("unknown")
    unknown("unknown"),
    @SerializedName("learning")
    learning("learning"),
    @SerializedName("working")
    working("working"),
    @SerializedName("learnthenwork")
    learnthenwork("learnthenwork"),
    @SerializedName("explosive")
    explosive("explosive"),
    @SerializedName("gradual")
    gradual("gradual");
    companion object {
        operator fun invoke(rawValue: String) = values().find { it.value == rawValue } ?: unknown
    }
}

enum class DesiredAction(val value: String) {
    @SerializedName("unknown")
    unknown("unknown"),
    @SerializedName("go")
    go("go"),
    @SerializedName("pause")
    pause("pause"),
    @SerializedName("charge")
    charge("charge");
    companion object {
        operator fun invoke(rawValue: String) = values().find { it.value == rawValue } ?: unknown
    }
}

enum class DesiredBool(val value: String) {
    @SerializedName("unknown")
    unknown("unknown"),
    @SerializedName("ON")
    yes("ON"),
    @SerializedName("OFF")
    no("OFF");
    companion object {
        operator fun invoke(rawValue: String) = values().find { it.value == rawValue } ?: unknown
    }
    val isTrue : Boolean get() = this == yes
}

enum class ReportedState(val value: String) {
    @SerializedName("unknown")
    unknown("unknown"),
    @SerializedName("setting")
    setting("setting"),
    @SerializedName("error")
    error("error"),
    @SerializedName("charging")
    charging("charging"),
    @SerializedName("working")
    working("working");
    companion object {
        operator fun invoke(rawValue: String) = values().find { it.value == rawValue } ?: unknown
    }
}

enum class ReportedAction(val value: String) {
    @SerializedName("unknown")
    unknown("unknown"),
    @SerializedName("go")
    go("go"),
    @SerializedName("pause")
    pause("pause"),
    @SerializedName("other")
    other("other");
    companion object {
        operator fun invoke(rawValue: String) = values().find { it.value == rawValue } ?: unknown
    }
}