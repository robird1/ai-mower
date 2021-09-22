package com.ulsee.mower.data

import android.content.SharedPreferences
import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.ulsee.mower.data.model.LoginRequest
import com.ulsee.mower.data.model.isOK

class AppAuthenticator: Authenticator {
    lateinit var api: AccountAPI
    lateinit var prefs: SharedPreferences

    fun setInfo(api: AccountAPI, prefs: SharedPreferences) {
        this.api = api
        this.prefs = prefs
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.i("AppAuthenticator", "${response.request.url}")
        val email = prefs.getString("email", null)
        val password = prefs.getString("password", null)
        if (email == null || password == null) return null

        val reloginResp = api.relogin(LoginRequest(email, password)).execute()
        val cookie = reloginResp.headers().get("Set-Cookie")
        if (cookie == null || cookie.isEmpty()) {
            return null
        }

        val idx = cookie.indexOf(";")
        prefs.edit().putString("cookie", cookie.substring(6, idx)).apply()

        return response.request.newBuilder()
            .header("Cookie", cookie)
            .build()
    }
}