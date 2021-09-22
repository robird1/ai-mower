package com.ulsee.mower.data

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.ulsee.mower.data.model.*
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */


class AccountDataSource(val baseURLString: String, val prefs: SharedPreferences) {

    lateinit var api: AccountAPI

    init {
        val appAuthenticator = AppAuthenticator()
        val client = OkHttpClient.Builder().authenticator(appAuthenticator).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseURLString)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
        api = retrofit.create(AccountAPI::class.java)

        appAuthenticator.setInfo(api, prefs)
    }

    suspend fun register(email: String, password: String): Result<RegisterResponse> {
        try {
            val response = api.register(RegisterRequest(email=email, password=password)).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error sign up ${e.message}"))
        }
    }

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        try {
            val response = api.login(LoginRequest(username, password)).await()
            if (response.body() == null) {
                response.errorBody()?.let{
                    val errorResponse = Gson().fromJson(it.string(),APIBaseResponse::class.java)
                    return Result.Error(Exception("Error [${errorResponse.errorcode}]: ${errorResponse.message}"))
                }
                return Result.Error(Exception("Error logging in, failed to parse response"))
            }
            val loginResponse: LoginResponse = response.body()!!
            val cookie = response.headers().get("Set-Cookie")
            if (cookie?.isEmpty() == true) {
                return Result.Error(Exception("Error logging in, no cookie"))
            }
            loginResponse.cookie = cookie!!
            if (loginResponse.isOK) {
                return Result.Success(loginResponse)
            }
            return Result.Error(Exception("(${loginResponse.errorcode}) ${loginResponse.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error logging in ${e.message}", e))
        }
    }

    suspend fun getMe(cookie: String): Result<LoginResponse> {
        try {
            val response = api.getMe(cookie).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error get me ${e.message}"))
        }
    }

    suspend fun requestResetPassword(email: String): Result<APIBaseResponse> {
        try {
            val response = api.requestResetPassword(email).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error request reset password ${e.message}"))
        }
    }

    suspend fun resetPassword(username: String, secret: String, newPassword: String): Result<APIBaseResponse> {
        try {
            val response = api.resetPassword(username, secret, newPassword).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error request reset password ${e.message}"))
        }
    }

    suspend fun bind(cookie: String, sn: String): Result<APIBaseResponse> {
        try {
            val response = api.bind(cookie, sn).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error bind ${e.message}"))
        }
    }

    suspend fun unbind(cookie: String, sn: String): Result<APIBaseResponse> {
        try {
            val response = api.unbind(cookie, sn).await()
            if (response.isOK) {
                return Result.Success(response)
            }
            return Result.Error(Exception("(${response.errorcode}) ${response.message}"))
        } catch (e: Throwable) {
            return Result.Error(Exception("Error unbind ${e.message}"))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}