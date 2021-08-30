package com.ulsee.mower

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.databinding.ActivityMainBinding
import com.ulsee.mower.ui.login.LoginActivity
import com.ulsee.mower.utils.Utils

private val TAG = MainActivity::class.java.simpleName
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

class MainActivity: AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    lateinit var viewModel: MainActivityViewModel
    private lateinit var bluetoothService: BluetoothLeService

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)

        checkService()

        if (!bluetoothService.bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        } else {
            initView()
        }
    }

    private fun initView() {
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding!!.root)

        val navController = findNavController(R.id.nav_host_fragment)

        navController.addOnDestinationChangedListener { _, destination, _ ->
//            binding.toolbar.collapseActionView()
            when (destination.id) {
                R.id.instruction1Fragment, R.id.instruction2Fragment, R.id.setupMapFragment,
                R.id.scheduleListFragment, R.id.scheduleEditorFragment, R.id.scheduleCalendarFragment,
                R.id.settingsFragment, R.id.mowerSettingsFragment, R.id.settingsBladeHeightFragment -> {
                    if (supportActionBar != null) {
                        supportActionBar?.hide()
                    }
                }
                else -> {
                    if (supportActionBar != null) {
                        supportActionBar?.show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        Log.d(TAG, "[Enter] onResume")
        super.onResume()
        if (!bluetoothService.bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Utils.REQUEST_LOCATION_SETTINGS -> {
                if (resultCode != RESULT_OK) {
                    Utils.checkLocationSetting(this)
                }
            }
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                } else {
                    if (binding == null) {
                        initView()
                    }
                }
            }
        }
    }

    private fun checkService() {
        (application as App).bluetoothService?.let {
            bluetoothService = it

        } ?: run {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


}