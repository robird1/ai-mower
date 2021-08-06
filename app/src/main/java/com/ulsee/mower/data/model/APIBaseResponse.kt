package com.ulsee.mower.data.model

data class APIBaseResponse(
    val errorcode: Int,
    val message: String,
    val result: String
)
val APIBaseResponse.isOK : Boolean
    get() = result == "success"