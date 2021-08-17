package com.ulsee.mower.data.model

import com.google.gson.JsonObject
import com.ulsee.mower.data.MowerStatus

data class IotCoreShadowPayload(
    var state: IotCoreShadowPayloadState,
    var clientToken: String
)

data class IotCoreShadowPayloadState (
    var reported: IotCoreShadowPayloadStateReported
)

data class IotCoreShadowPayloadStateReported(
//    var thingName: String,
    var State: Int,
//    var UserID: String,
//    var Mobile: JsonObject,
//    var Uploader: String
    var data: MowerStatus,
)