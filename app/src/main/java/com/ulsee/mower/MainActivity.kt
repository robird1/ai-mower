package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.ulsee.mower.databinding.ActivityMainBinding
import com.ulsee.mower.utils.Utils

private val TAG = MainActivity::class.java.simpleName

class MainActivity: AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.navigation_device, R.id.navigation_people, R.id.navigation_notification, R.id.navigation_settings))
//        setupActionBarWithNavController(navController, appBarConfiguration)


//        binding.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
//            binding.toolbar.collapseActionView()
            when (destination.id) {
                R.id.setupMapFragment -> {
                    if (supportActionBar != null) {
                        supportActionBar?.hide();
                    }
                }
                else -> {
                    if (supportActionBar != null) {
                        supportActionBar?.show();
                    }
                }
            }
        }
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

}