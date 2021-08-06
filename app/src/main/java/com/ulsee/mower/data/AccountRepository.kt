package com.ulsee.mower.data

import android.content.SharedPreferences
import com.ulsee.mower.data.model.APIBaseResponse
import com.ulsee.mower.data.model.LoginResponse
import com.ulsee.mower.data.model.RegisterResponse
import java.lang.Exception

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class AccountRepository(val dataSource: AccountDataSource, val prefs: SharedPreferences) {

    var cookie: String? = null

    val isLoggedIn: Boolean
        get() = cookie != null

    init {
        cookie = prefs.getString("cookie", null)
    }

    fun logout() {
        cookie = null
        dataSource.logout()
    }

//    robot=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50Ijp7InVzZXJpZCI6NSwiY3JlYXRldGltZSI6IjIwMjEtMDgtMDNUMTQ6MjM6MjkrMDg6MDAiLCJhY2Nlc3N0aW1lIjoiMjAyMS0wOC0wM1QxNDo1ODoyMiswODowMCIsInN0YXR1cyI6Im5vcm1hbCIsInJvbGUiOiJ1c2VyIiwidXNlcm5hbWUiOiIiLCJwYXNzd29yZCI6ImUxMGFkYzM5NDliYTU5YWJiZTU2ZTA1N2YyMGY4ODNlIiwiZ3JvdXBuYW1lIjoidWxzZWUiLCJkaXNwbGF5bmFtZSI6ImNvZHVzLmhzdUB1bHNlZS5jb20iLCJjb21wYW55IjoiIiwiZW1haWwiOiJjb2R1cy5oc3VAdWxzZWUuY29tIiwicGhvbmUiOiIiLCJjb3VudHJ5IjoiVEFJV0FOIn0sImV4cCI6MTYyNzk5MjE3MywiaWF0IjoxNjI3OTc0MTczfQ.q53b8evUtnezV-JpQFHEmCqyZaN5ONYW6iPHUOR4yc8; Path=/; Expires=Tue, 03 Aug 2021 20:02:53 GMT; Secure; SameSite=None
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        val result = dataSource.login(email, password)
        if(result is Result.Success) {
            val idx = result.data.cookie.indexOf(";")
            val cookieValue = result.data.cookie.substring(6, idx)
            this.cookie = cookieValue
            prefs.edit().putString("cookie", cookieValue).apply()
        }
        return result
    }

    suspend fun register(email: String, password: String): Result<RegisterResponse> {
        return dataSource.register(email, password)
    }

    suspend fun getMe(): Result<LoginResponse> {
        if (cookie == null) {
            return Result.Error(Exception("no cookie"))
        }
        return dataSource.getMe("robot="+cookie!!)
    }

    suspend fun requestResetPassword(email: String): Result<APIBaseResponse> {
        return dataSource.requestResetPassword(email)
    }

    suspend fun resetPassword(email: String, secret: String, password: String): Result<APIBaseResponse> {
        return dataSource.resetPassword(email, secret, password)
    }

    suspend fun bind(sn: String): Result<APIBaseResponse> {
        if (cookie == null) {
            return Result.Error(Exception("no cookie"))
        }
        return dataSource.bind("robot="+cookie!!, sn)
    }

    suspend fun unbind(sn: String): Result<APIBaseResponse> {
        if (cookie == null) {
            return Result.Error(Exception("no cookie"))
        }
        return dataSource.unbind("robot="+cookie!!, sn)
    }
}