package com.ulsee.mower

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class ConnectDeviceActivity : AppCompatActivity() {
//    private lateinit var scanButton: Button
//    private lateinit var connectButton: Button
//    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ConstraintLayout
    private lateinit var viewModel: ConnectDeviceActivityViewModel
//    private val bluetoothAdapter: BluetoothAdapter by lazy {
//        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }
//    private val bleScanner by lazy {
//        bluetoothAdapter.bluetoothLeScanner
//    }
    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        initViewModel()
        initProgressBar()

//        initRecyclerView()
//        initScanStatusObserver()
//        initScanResultObserver()
        initDeviceNotFoundObserver()
        initConnectFailedObserver()
        initGattStatusObserver()
        initVerificationObserver()
//        initServiceDiscoveredObserver()
//        configScanButton()
//        connectButton = findViewById(R.id.button2)
//        connectButton.setOnClickListener {
//            startBleConnect()
//        }
        configConnectBtn()
    }

    private fun initVerificationObserver() {
        viewModel.isVerificationSuccess.observe(this) { isSuccess ->
            progressBar.isVisible = false
            if (isSuccess) {
                val intent = Intent(this, StatusActivity::class.java)
                startActivity(intent)
            }
//            else {
//                Toast.makeText(this, "error: Device not found", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun initConnectFailedObserver() {
        viewModel.connectFailedLog.observe(this) {
            progressBar.isVisible = false
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initDeviceNotFoundObserver() {
        viewModel.isDeviceFound.observe(this) { isFound ->
            progressBar.isVisible = false
            if (!isFound) {
                Toast.makeText(this, "error: Device not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initProgressBar() {
        progressBar = findViewById(R.id.progress_view)
    }

    private fun configConnectBtn() {
        val connectBtn = findViewById<Button>(R.id.button)
        connectBtn.setOnClickListener {
            val textInput = findViewById<TextInputEditText>(R.id.sn_input)
            Log.d(javaClass.name, "textInput: ${textInput.text.toString()}")
            progressBar.isVisible = true
            viewModel.connectBLEDevice(this, textInput.text.toString())
        }
    }

//    private fun initRecyclerView() {
//        recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
//        progressBar = findViewById(R.id.progressBar)
//        recyclerView.adapter = ScanListAdapter(viewModel,progressBar)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//    }
//
//    private fun configScanButton() {
//        scanButton = findViewById(R.id.button)
//        scanButton.setOnClickListener {
//            if (viewModel.isScanning.value == true) {
//                stopBleScan()
//            } else {
////                progressBar.isVisible = true
////                scanButton.isVisible = false
//                startBleScan()
//            }
//        }
//    }
//
//    private fun initScanStatusObserver() {
//        viewModel.isScanning.observe(this) {
//            scanButton.text = if (it) "Stop Scan" else "Start Scan"
//        }
//    }
//
//    private fun initScanResultObserver() {
//        viewModel.scanResult.observe(this) {
////            Log.d(javaClass.name, "[Enter] observe scan result...")
//            scanButton.isVisible = false
//
//            (recyclerView.adapter as ScanListAdapter).setList(it)
////            when(it.first) {
////                0 -> recyclerView.adapter!!.notifyItemChanged(it.second)
////                1 -> recyclerView.adapter!!.notifyItemInserted(scanResults.size - 1)
////            }
//        }
//    }

    private fun initGattStatusObserver() {
        progressBar.isVisible = false
        viewModel.gattStatusCode.observe(this) {
            when(it) {
                0 -> Toast.makeText(this, "BluetoothGatt.GATT_SUCCESS", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "status != BluetoothGatt.GATT_SUCCESS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initServiceDiscoveredObserver() {
        viewModel.serviceDiscoveredStatus.observe(this) {
//            progressBar.isVisible = false
        }
    }


    override fun onResume() {
        super.onResume()
//        if (!bluetoothAdapter.isEnabled) {
//            promptEnableBluetooth()
//        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            ENABLE_BLUETOOTH_REQUEST_CODE -> {
//                if (resultCode != Activity.RESULT_OK) {
//                    promptEnableBluetooth()
//                }
//            }
//        }
//    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
//        if (!bluetoothAdapter.isEnabled) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
//        }
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun startBleScan() {
        Log.d(javaClass.name, "[Enter] startBleScan")
        if (!isLocationPermissionGranted) {
            requestLocationPermission()
        }
        else {
            viewModel.startBLEScan()
        }
    }

    private fun stopBleScan() {
        Log.d(javaClass.name, "[Enter] stopBleScan")
        viewModel.stopBleScan()
    }

//    private fun startBleConnect() {
//        viewModel.connectBLEDevice(this)
//    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Location permission required")
                    .setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                    "location access in order to scan for BLE devices.")
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }.show()
//            alert {
//                title = "Location permission required"
//                message = "Starting from Android M (6.0), the system requires apps to be granted " +
//                        "location access in order to scan for BLE devices."
//                isCancelable = false
//                positiveButton(android.R.string.ok) {
//                    requestPermission(
//                            Manifest.permission.ACCESS_FINE_LOCATION,
//                            LOCATION_PERMISSION_REQUEST_CODE
//                    )
//                }
//            }.show()
        }
    }

    fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private fun initViewModel() {
//        viewModel = ViewModelProvider(this, MainActivityFactory()).get(MainActivityViewModel::class.java)
        viewModel = ViewModelProvider.AndroidViewModelFactory(application).create(ConnectDeviceActivityViewModel::class.java)

    }

}