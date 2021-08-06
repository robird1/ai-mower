package com.ulsee.mower.data

import com.ulsee.mower.data.model.*
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
//import okhttp3.Response

interface AccountAPI {

    @POST("/api/account/regist")
    fun register(@Body payload: RegisterRequest): Deferred<RegisterResponse>

    @POST("/api/account/login")
    fun login(@Body payload: LoginRequest): Deferred<Response<LoginResponse>>

    @GET("/api/account/whoami")
    fun getMe(@Header("Cookie") cookie: String): Deferred<LoginResponse> // response most likely login

    @GET("/api/account/getpass")
    fun requestResetPassword(@Query("email") email: String): Deferred<APIBaseResponse>

    @GET("/api/account/initpass")
    fun resetPassword(@Query("username") username: String, @Query("secret") secret: String, @Query("password") password: String): Deferred<APIBaseResponse>

    // TNM78-FJKXR-P26YV-GP8MB-JK8XG
    @GET("/api/account/bind")
    fun bind(@Header("Cookie") cookie: String, @Query("sn") sn: String): Deferred<APIBaseResponse>

    @GET("/api/account/unbind")
    fun unbind(@Header("Cookie") cookie: String, @Query("sn") sn: String): Deferred<APIBaseResponse>

}