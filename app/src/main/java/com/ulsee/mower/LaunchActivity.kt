package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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