package com.ulsee.mower.data.model

data class RegisterRequest(
    val username: String? = null,
    val password: String,
    val email: String,
    val country: String = "TAIWAN"
)