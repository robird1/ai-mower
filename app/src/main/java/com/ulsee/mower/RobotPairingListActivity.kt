package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class RobotPairingListActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_robot_pairing_list)
//        val icon1 = findViewById<ImageView>(R.id.imageView3)
//        val icon2 = findViewById<ImageView>(R.id.imageView4)
//        val text1 = findViewById<TextView>(R.id.textView5)
//        val text2 = findViewById<TextView>(R.id.textView6)
//        val noRobotText = findViewById<TextView>(R.id.textView7)
        val addRobotBtn = findViewById<Button>(R.id.button)
//
//        val mode = intent.getStringExtra("mode")
//        if (mode == "no_robot") {
//            icon1.isVisible = false
//            icon2.isVisible = false
//            text1.isVisible = false
//            text2.isVisible = false
//            noRobotText.isVisible = true
//        } else {
//            icon1.isVisible = true
//            icon2.isVisible = true
//            text1.isVisible = true
//            text2.isVisible = true
//            noRobotText.isVisible = false
//        }
//
        addRobotBtn.setOnClickListener {
//            if (mode == "no_robot") {
//                val intent = Intent(this, AddRobotInstructionActivity::class.java)
//                startActivity(intent)
//
//            } else {
//                // do nothing
//            }
        }
//
//        text1.setOnClickListener {
//            showPairingDialog()
//        }
//
//        text2.setOnClickListener {
//            showPairingDialog()
//        }

    }

    private fun showPairingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setMessage("The Ladybug you are pairing is beeping. Please confirm that the beeping is correct.")
            .setPositiveButton ("Add") { it, _ ->
                val intent = Intent(this, StatusActivity::class.java)
                startActivity(intent)

            }
            .setNegativeButton("Not this one") { it, _ ->
                val intent = Intent(this, StatusActivity::class.java)
                startActivity(intent)

            }
            .setCancelable(false)
            .create()
        dialog.show()
    }



}