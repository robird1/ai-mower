package com.ulsee.mower

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.ActivityMainBinding
import com.ulsee.mower.utils.Utils

private val TAG = MainActivity::class.java.simpleName

class MainActivity: AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
//    var bluetoothService: BluetoothLeService? = null

    // Code to manage Service lifecycle.
//    private val serviceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//                componentName: ComponentName,
//                service: IBinder
//        ) {
//            Log.d(TAG, "[Enter] onServiceConnected")
//
//            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
////            bluetoothService?.let { bluetooth ->
////                // call functions on service to check connection and connect to devices
////            }
//        }
//
//        override fun onServiceDisconnected(componentName: ComponentName) {
//            bluetoothService = null
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setSupportActionBar(binding.toolbar)

//        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.navigation_device, R.id.navigation_people, R.id.navigation_notification, R.id.navigation_settings))
//        setupActionBarWithNavController(navController, appBarConfiguration)


//        binding.navView.setupWithNavController(navController)

//        navController.addOnDestinationChangedListener { _, destination, _ ->
//            binding.toolbar.collapseActionView()
//            when (destination.id) {
//                R.id.navigation_people -> setTitle("People")
//                R.id.navigation_record -> setTitle("Record")
//                R.id.attend_record -> hideBottomNav()
//                R.id.device_settings -> hideBottomNav()
//                R.id.language_config -> hideBottomNav()
//                R.id.temperature_config -> hideBottomNav()
//                R.id.panel_ui_config -> hideBottomNav()
//                R.id.others_config -> hideBottomNav()
//                R.id.light_mode_config -> hideBottomNav()
//                R.id.volume_config -> hideBottomNav()
//                R.id.time_config -> hideBottomNav()
//                R.id.capture_config -> hideBottomNav()
//                else -> showBottomNav()
//            }
//
//        }
//        Log.d(TAG, "isLocationPermissionGranted: $isLocationPermissionGranted")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        Log.d(TAG, "[Enter] onActivityResult")
        if (requestCode == Utils.REQUEST_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) {
//                Log.d(TAG, "[Enter] requestCode == Utils.REQUEST_LOCATION_SETTINGS")


            } else {
//                Log.d(TAG, "[Enter] else section")

                Utils.checkLocationSetting(this)
//                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

//    fun setTitle (title: String) {
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        toolbar.findViewById<TextView>(R.id.textView_toolbar_title).text = title
//    }
//
//    private fun showBottomNav() {
//        binding.navView.visibility = View.VISIBLE
//    }
//
//    private fun hideBottomNav() {
//        binding.navView.visibility = View.GONE
//    }
}