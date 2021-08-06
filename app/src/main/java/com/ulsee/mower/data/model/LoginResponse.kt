package com.ulsee.mower.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class LoginResponse(
        val accesstime: String,
        val company: String,
        val country: String,
        val createtime: String,
        val displayname: String,
        val email: String,
        val errorcode: Int,
        val expired: String,
        val groupname: String,
        val message: String,
        val phone: String,
        val result: String,
        val role: String,
        val status: String,
        val userid: Int,
        val username: String,
        var cookie: String,// this parameter will parse from http response body
        val history: List<BindedDevice>
)
val LoginResponse.isOK : Boolean
        get() = result == "success"

data class BindedDevice(
        val sn: String,
        val accesstime: String,
        val type: String,
        val name: String,
        val owner: String,
        val status: String,
        val version: String
)