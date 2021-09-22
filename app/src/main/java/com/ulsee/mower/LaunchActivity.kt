package com.ulsee.mower

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ulsee.mower.data.AccountDataSource
import com.ulsee.mower.data.AccountRepository
import com.ulsee.mower.ui.login.LoginActivity
import kotlinx.coroutines.runBlocking
import java.util.*

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(this.javaClass.simpleName, "[Enter] onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                val prefs = getSharedPreferences("account", Context.MODE_PRIVATE)
                val loginRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/", prefs), prefs)
                if (loginRepository.isLoggedIn) {
                    startMainActivity()
                } else {
                    startLoginActivity()
                }
            }
        }, 2000)
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}