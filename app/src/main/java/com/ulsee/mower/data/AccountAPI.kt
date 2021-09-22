package com.ulsee.mower.data

import com.ulsee.mower.data.model.*
import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
//import okhttp3.Response

interface AccountAPI {

    @POST("account/regist")
    fun register(@Body payload: RegisterRequest): Deferred<RegisterResponse>

    @POST("account/login")
    fun login(@Body payload: LoginRequest): Deferred<Response<LoginResponse>>

    @POST("account/login")
    fun relogin(@Body payload: LoginRequest): Call<Response<LoginResponse>>

    @GET("account/whoami")
    fun getMe(@Header("Cookie") cookie: String): Deferred<LoginResponse> // response most likely login

    @GET("account/getpass")
    fun requestResetPassword(@Query("email") email: String): Deferred<APIBaseResponse>

    @GET("account/initpass")
    fun resetPassword(@Query("username") username: String, @Query("secret") secret: String, @Query("password") password: String): Deferred<APIBaseResponse>

    // TNM78-FJKXR-P26YV-GP8MB-JK8XG
    @GET("account/bind")
    fun bind(@Header("Cookie") cookie: String, @Query("sn") sn: String): Deferred<APIBaseResponse>

    @GET("account/unbind")
    fun unbind(@Header("Cookie") cookie: String, @Query("sn") sn: String): Deferred<APIBaseResponse>

}