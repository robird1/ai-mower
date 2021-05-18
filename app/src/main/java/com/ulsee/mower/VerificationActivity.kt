package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class VerificationActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)
        val inputCode = findViewById<EditText>(R.id.input_code)
        val nextBtn = findViewById<Button>(R.id.nextBtn)
        nextBtn.setOnClickListener {

//            val intent = Intent(this, RobotPairingListActivity::class.java)
//            intent.putExtra("mode", "no_robot")
//            val intent = Intent(this, ConnectDeviceActivity::class.java)
//            val intent = Intent(this, AddRobotInstructionActivity::class.java)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}