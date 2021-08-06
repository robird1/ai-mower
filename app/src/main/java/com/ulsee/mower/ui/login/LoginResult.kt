package com.ulsee.mower.ui.login

/**
 * Authentication result : success (user details) or error message.
 */
data class LoginResult(
        val success: LoggedInUserView? = null,
        val error: String? = null
)
data class ActionResult(
        val error: String? = null
)