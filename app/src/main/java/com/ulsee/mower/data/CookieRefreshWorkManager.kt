package com.ulsee.mower.data

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CookieRefreshWorkManager(val context: Context, params: WorkerParameters) : Worker(context, params) {

    private val TAG = "CookieRefreshWorkManager"

    override fun doWork(): Result {

        val prefs = context.getSharedPreferences("account", Context.MODE_PRIVATE)
        val accountRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/"), prefs)
        Log.i(TAG, "accountRepository.onceLoggedIn = ${accountRepository.onceLoggedIn}")
        if (accountRepository.onceLoggedIn) {
            GlobalScope.launch {
                val result = accountRepository.login(accountRepository.email!!, accountRepository.password!!)
                if (result is com.ulsee.mower.data.Result.Success) {
                    Log.i(TAG, "Login result success")
                } else if (result is com.ulsee.mower.data.Result.Error) {
                    Log.i(TAG, "Login result error: ${result.exception.message}")
                } else {
                    Log.i(TAG, "Login result unknown")
                }
            }
        }

        return Result.success()
    }
}