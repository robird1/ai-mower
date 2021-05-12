package com.ulsee.mower

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StatusActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        val setupMapBtn = findViewById<Button>(R.id.button2)
        setupMapBtn.setOnClickListener {
//            val intent = Intent(this, SetupMapInstructionActivity::class.java)
//            startActivity(intent)

        }
    }
}