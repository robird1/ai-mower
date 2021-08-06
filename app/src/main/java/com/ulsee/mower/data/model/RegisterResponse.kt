package com.ulsee.mower.data.model

data class RegisterResponse(
    val accountid: Int,
    val errorcode: Int,
    val message: String,
    val result: String
)
val RegisterResponse.isOK : Boolean
    get() = result == "success"