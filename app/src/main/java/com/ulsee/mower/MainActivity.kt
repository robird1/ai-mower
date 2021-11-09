package com.ulsee.mower

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.ulsee.mower.ble.BluetoothLeService
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.data.*
import com.ulsee.mower.databinding.ActivityMainBinding
import com.ulsee.mower.ui.login.LoginActivity
import com.ulsee.mower.utils.Utils
import java.util.concurrent.TimeUnit

private val TAG = MainActivity::class.java.simpleName
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1

class MainActivity: AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    var viewModel: MainActivityViewModel? = null
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

        initViewModel()
        initAWSConnectFailedObserver()
        viewModel?.keepUploadingStatus()
        startRefreshCookieWorkManager()
    }

    override fun onStart() {
        super.onStart()
        registerBLEReceiver()
    }

    override fun onStop() {
        unregisterBLEReceiver()
        super.onStop()
    }

    override fun onResume() {
        Log.d(TAG, "[Enter] onResume")
        super.onResume()
        if (!bluetoothService.bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun initAWSConnectFailedObserver() {
        viewModel?.awsConnectFailedLog?.observe(this) {
            it.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
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
    private fun initViewModel() {
        val bluetoothService = (application as App).bluetoothService!!
        val bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MainActivityViewModelFactory(bluetoothService, bleRepository))
            .get(MainActivityViewModel::class.java)
    }

    private fun startRefreshCookieWorkManager() {
        val request = PeriodicWorkRequest
            .Builder(CookieRefreshWorkManager::class.java, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("CookieRefreshWorkManager", ExistingPeriodicWorkPolicy.KEEP, request)
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

    // =================================================
    // ================== broadcast ====================
    // =================================================
    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(BLEBroadcastAction.ACTION_STATUS)
        filter.addAction(BLEBroadcastAction.ACTION_SETTINGS)
        filter.addAction(BLEBroadcastAction.ACTION_ON_DISCONNECT_DEVICE)
        filter.addAction(BLEBroadcastAction.ACTION_GATT_CONNECTED)

        filter.addAction(StatusFragmentBroadcast.LIFECYCLE_ONRESUME)
        filter.addAction(StatusFragmentBroadcast.LIFECYCLE_ONPAUSE)
        filter.addAction(StatusFragmentBroadcast.MOWER_STATUS_MOWING)
        filter.addAction(StatusFragmentBroadcast.MOWER_STATUS_PAUSE)
        filter.addAction(StatusFragmentBroadcast.MOWER_STATUS_STOP)
        if (viewModel != null) registerReceiver(viewModel!!.gattUpdateReceiver, filter)
    }

    private fun unregisterBLEReceiver() {
        if (viewModel != null) unregisterReceiver(viewModel!!.gattUpdateReceiver)
    }
}