package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ulsee.mower.ui.login.LoginActivity
import java.util.*

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(this.javaClass.simpleName, "[Enter] onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                startLoginActivity()
            }
        }, 2000)
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
//        val intent = Intent(this, ScheduleActivity::class.java)

        startActivity(intent)
        finish()
    }
}