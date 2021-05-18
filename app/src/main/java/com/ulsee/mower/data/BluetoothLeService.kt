//package com.ulsee.mower.data
//
//import android.app.Application
//import android.app.Service
//import android.bluetooth.*
//import android.bluetooth.le.ScanCallback
//import android.bluetooth.le.ScanResult
//import android.bluetooth.le.ScanSettings
//import android.content.Context
//import android.content.Intent
//import android.os.Binder
//import android.os.Handler
//import android.os.IBinder
//import android.os.Looper
//import android.util.Log
//import com.ulsee.mower.MD5
//import com.ulsee.mower.RobotListFragment
//import com.ulsee.mower.Utils
//import java.util.*
//
//private val TAG = BluetoothLeService::class.java.simpleName
//
//private const val MANUFACTURER_ID = 741
//private val SERVICE_UUID = UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb")
//private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb")
//private val CHARACTERISTIC_READ_UUID = UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb")
//private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//
//class BluetoothLeService : Service() {
//
//    private val binder = LocalBinder()
//
//    val bluetoothAdapter: BluetoothAdapter by lazy {
//        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        bluetoothManager.adapter
//    }
//    private val bleScanner by lazy {
//        bluetoothAdapter.bluetoothLeScanner
//    }
//
//    private val handler: Handler = Handler(Looper.getMainLooper())
//    private var verificationTask: Runnable? = null
//
//    private val scanSettings = ScanSettings.Builder()
//            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//            .build()
//    private var scanResults: ArrayList<ScanResult> = ArrayList()
//    private var bluetoothGatt: BluetoothGatt? = null
//
//    private var serialNumberInput: String? = null
//
//
//
//    override fun onBind(intent: Intent): IBinder? {
//        return binder
//    }
//
//    inner class LocalBinder : Binder() {
//        fun getService() : BluetoothLeService {
//            return this@BluetoothLeService
//        }
//    }
//
//    private val scanCallback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
////            Log.d(TAG, "[Enter] onScanResult")
//            with(result.device) {
//                val scanRecord = result.scanRecord
//                val manufacturerData = scanRecord?.manufacturerSpecificData ?: return
//                if (manufacturerData.size() > 0) {
//                    val manufacturerId = manufacturerData.keyAt(0)
//                    if (manufacturerId == MANUFACTURER_ID) {
//                        val manufacturerData = scanRecord.getManufacturerSpecificData(manufacturerId)?.toHexString()
//                        Log.d(TAG, "manufacturerId: $manufacturerId manufacturerData: $manufacturerData")
////                        storeSerialNumber(manufacturerData!!, result)
//
//                        val indexQuery =  scanResults.indexOfFirst { it.device.address == address }
//                        if (indexQuery != -1) {  // A scan result already exists with the same address
//                            scanResults[indexQuery] = result
//                        } else {
//                            scanResults.add(result)
//                        }
////                        _scanResult.postValue(scanResults)
////                        Log.d(TAG, "device.hashCode(): ${result.device.hashCode()}")
//                    }
//                }
//            }
//        }
//    }
//
//    fun startBLEScan(fragment: RobotListFragment) {
//
//        Utils.checkLocationSetting(fragment.requireActivity())
//
////        Log.d(TAG, "[Enter] startBLEScan()")
//        if (!fragment.isLocationPermissionGranted) {
//            fragment.requestLocationPermission()
//        }
//        else {
////        val filters = arrayListOf(filter)
////            if (_isScanning.value == true)
//            stopBleScan()
//
//            bleScanner.startScan(null, scanSettings, scanCallback)
////            _isScanning.value = true
//        }
//    }
//
//    fun stopBleScan() {
//        Log.d(TAG, "[Enter] stopBleScan()")
//        bleScanner.stopScan(scanCallback)
////        _isScanning.value = false
//    }
//
//    fun connectBLEDevice(context: Context, serialNumber: String) {
//        stopBleScan()
//
//        serialNumberInput = serialNumber
//        val inputSN = inputSerialNumberToMD5()
//        for (i in scanResults) {
//            if (inputSN == getScanDeviceSN(i)) {
//                Log.d(TAG, "inputSN == getScanDeviceSN(i)")
//
//                Handler(Looper.getMainLooper()).postDelayed({
//                    Log.d(TAG, "[Enter] connectGatt()")
//                    i.device.connectGatt(context, false, gattCallback)
//                }, 3000)
//
//                return
//            }
//        }
//
////        _isDeviceFound.value = false
//        broadcastUpdate(ACTION_DEVICE_NOT_FOUND)
//    }
//
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            val deviceAddress = gatt.device.address
//
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    Log.d(TAG, "Successfully connected to $deviceAddress")
//                    bluetoothGatt = gatt
//                    Handler(Looper.getMainLooper()).post {
//                        bluetoothGatt?.discoverServices()
//                    }
//
////                    Handler(Looper.getMainLooper()).post {
////                        _gattStatusCode.value = status
////                    }
//                    broadcastUpdate(ACTION_GATT_CONNECTED)
//
//
//                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    Log.d(TAG, "Successfully disconnected from $deviceAddress")
//
//                    broadcastUpdate(ACTION_GATT_DISCONNECTED)
//
//                    gatt.close()
//                }
//            } else {
//                Log.d(TAG, "Error $status encountered for $deviceAddress! Disconnecting...")
////                Handler(Looper.getMainLooper()).post {
////                    _connectFailedLog.value = "Error $status"
////                }
////                setValueInMainThread {
////                    _connectFailedLog.value = "Error $status"
////                }
//                broadcastUpdate(ACTION_GATT_NOT_SUCCESS, status)
//
//                gatt.close()
//            }
//
////            Handler(Looper.getMainLooper()).post {
////                _gattStatusCode.value = status
////            }
//
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            with(gatt) {
//                Log.d(TAG, "Discovered ${services.size} services for ${device.address}")
//                printGattTable() // See implementation just above this section
//                // Consider connection setup as complete here
//                enableNotifications()
//
//                Handler(Looper.getMainLooper()).postDelayed({
//                    writeCharacteristic()
//                }, 3000)
//
//            }
//        }
//
//        override fun onCharacteristicRead(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int
//        ) {
//            with(characteristic) {
//                when (status) {
//                    BluetoothGatt.GATT_SUCCESS -> {
//                        Log.d(TAG, "Read characteristic $uuid:\n${value.toHexString()}")
//                    }
//                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
//                        Log.d(TAG, "Read not permitted for $uuid!")
//                    }
//                    else -> {
//                        Log.d(TAG, "Characteristic read failed for $uuid, error: $status")
//                    }
//                }
//            }
//        }
//
//        override fun onCharacteristicWrite(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int
//        ) {
//            with(characteristic) {
//                when (status) {
//                    BluetoothGatt.GATT_SUCCESS -> {
//                        Log.i(TAG, "Wrote to characteristic $uuid | value: ${value.toHexString()}")
//                    }
//                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
//                        Log.e(TAG, "Write exceeded connection ATT MTU!")
//                    }
//                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
//                        Log.e(TAG, "Write not permitted for $uuid!")
//                    }
//                    else -> {
//                        Log.e(TAG, "Characteristic write failed for $uuid, error: $status")
//                        setValueInMainThread {
//                            _connectFailedLog.value = "Characteristic write failed for $uuid, error: $status"
//                        }
//                    }
//                }
//            }
//        }
//
//        override fun onCharacteristicChanged(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic
//        ) {
//            Log.i(TAG, "[Enter] onCharacteristicChanged")
//
//            with(characteristic) {
//                Log.i(TAG, "Characteristic $uuid changed | value: ${value.toHexString()}")
//                handler.removeCallbacks(verificationTask!!)
//                setValueInMainThread {
//                    _isVerificationSuccess.value = true
//                }
//            }
//        }
//
//        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
//            with(descriptor) {
//                when (status) {
//                    BluetoothGatt.GATT_SUCCESS -> {
//                        Log.i(TAG, "Wrote to descriptor ${this?.uuid} | value: ${this?.value?.toHexString()}")
//                    }
//                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
//                        Log.e(TAG, "Write exceeded connection ATT MTU!")
//                    }
//                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
//                        Log.e(TAG, "Write not permitted for ${this?.uuid}!")
//                    }
//                    else -> {
//                        Log.e(TAG, "descriptor write failed for ${this?.uuid}, error: $status")
//                        setValueInMainThread {
//                            _connectFailedLog.value = "descriptor write failed for ${this?.uuid}, error: $status"
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun enableNotifications() {
//        val characteristic = bluetoothGatt?.getService(com.ulsee.mower.SERVICE_UUID)?.getCharacteristic(com.ulsee.mower.CHARACTERISTIC_READ_UUID)
//        val payload = when {
//            characteristic?.isIndicatable() == true -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
//            characteristic?.isNotifiable() == true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//            else -> {
//                Log.e(com.ulsee.mower.TAG, "${characteristic?.uuid} doesn't support notifications/indications")
//                return
//            }
//        }
//
//        characteristic.getDescriptor(com.ulsee.mower.CCCD_UUID)?.let { cccDescriptor ->
//            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
//                Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
//                setValueInMainThread {
//                    _connectFailedLog.value = "setCharacteristicNotification failed for ${characteristic.uuid}"
//                }
//                return
//            }
//            writeDescriptor(cccDescriptor, payload)
//        } ?: Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
//    }
//
//    private fun inputSerialNumberToMD5(): String {
//        return MD5.convertMD5(serialNumberInput)
//    }
//
//    private fun getScanDeviceSN(scanResult: ScanResult): String {
//        val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID)?.toHexString()
//        val temp = manufacturerData?.split(" ")
//        val temp2 = temp?.subList(4, temp.size)
//
//        val strbul = StringBuilder()
//        for (str in temp2!!) {
//            strbul.append(str)
//        }
//        return strbul.toString()
//    }
//
//    private fun broadcastUpdate(action: String) {
//        val intent = Intent(action)
//        sendBroadcast(intent)
//    }
//
//    private fun BluetoothGatt.printGattTable() {
//        if (services.isEmpty()) {
//            Log.d(TAG, "No service and characteristic available, call discoverServices() first?")
//            return
//        }
//        services.forEach { service ->
//            val characteristicsTable = service.characteristics.joinToString(
//                    separator = "\n|--",
//                    prefix = "|--"
//            ) { it.uuid.toString() }
//            Log.d(TAG, "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
//            )
//        }
//    }
//
////    fun initialize(): Boolean {
////        // If bluetoothManager is null, try to set it
////        if (bluetoothManager == null) {
////            bluetoothManager = getSystemService(BluetoothManager::class.java)
////            if (bluetoothManager == null) {
////                Log.e(TAG, "Unable to initialize BluetoothManager.")
////                return false
////            }
////        }
////        // For API level 18 and higher, get a reference to BluetoothAdapter through
////        // BluetoothManager.
////        bluetoothManager?.let { manager ->
////            bluetoothAdapter = manager.adapter
////            if (bluetoothAdapter == null) {
////                Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
////                return false
////            }
////            return true
////        } ?: return false
////
////    }
//
//    fun ByteArray.toHexString(): String =
//            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
//
//
//}