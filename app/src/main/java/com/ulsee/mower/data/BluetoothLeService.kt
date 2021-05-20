package com.ulsee.mower.data

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.ulsee.mower.utils.MD5
import java.util.*

private val TAG = BluetoothLeService::class.java.simpleName

private const val MANUFACTURER_ID = 741
private val SERVICE_UUID = UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb")
private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb")
private val CHARACTERISTIC_READ_UUID = UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

const val ACTION_CONNECT_FAILED = "action_connect_failed"
const val ACTION_DEVICE_NOT_FOUND = "action_device_not_found"
const val ACTION_GATT_CONNECTED = "action_gatt_connected"
const val ACTION_GATT_DISCONNECTED = "action_gatt_disconnected"
const val ACTION_GATT_NOT_SUCCESS = "action_gatt_not_success"
const val ACTION_VERIFICATION_SUCCESS = "action_verification_success"
const val ACTION_VERIFICATION_FAILED = "action_verification_failed"

class BluetoothLeService : Service() {

    private val binder = LocalBinder()

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val handler: Handler = Handler(Looper.getMainLooper())
    private var verificationTask: Runnable? = null

    private val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    private var scanResults: ArrayList<ScanResult> = ArrayList()
    private var bluetoothGatt: BluetoothGatt? = null

    private var serialNumberInput: String? = null

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null


    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        HandlerThread("ServiceStartArguments", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            Log.d(TAG, "[Enter] onScanResult")
            with(result.device) {
                val scanRecord = result.scanRecord
                val manufacturerData = scanRecord?.manufacturerSpecificData ?: return
                if (manufacturerData.size() > 0) {
                    val manufacturerId = manufacturerData.keyAt(0)
                    if (manufacturerId == MANUFACTURER_ID) {
                        val manufacturerData = scanRecord.getManufacturerSpecificData(manufacturerId)?.toHexString()
                        Log.d(TAG, "manufacturerId: $manufacturerId manufacturerData: $manufacturerData")

                        val indexQuery =  scanResults.indexOfFirst { it.device.address == address }
                        if (indexQuery != -1) {  // A scan result already exists with the same address
                            scanResults[indexQuery] = result
                        } else {
                            scanResults.add(result)
                        }
                    }
                }
            }
        }
    }

    fun startScan() {
        Log.d(TAG, "[Enter] startScan()")
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    fun stopScan() {
        Log.d(TAG, "[Enter] stopScan()")
        bleScanner.stopScan(scanCallback)
//        _isScanning.value = false
    }

    fun connectDevice(serialNumber: String) {
        Log.d(TAG, "[Enter] connectDevice()")
        stopScan()

        serialNumberInput = serialNumber
        val inputSN = inputSerialNumberToMD5()
        for (i in scanResults) {
            if (inputSN == getScanDeviceSN(i)) {
//                Log.d(TAG, "inputSN == getScanDeviceSN(i)")
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "[Enter] connectGatt()")
                    i.device.connectGatt(this, false, gattCallback)
                }, 3000)

                return
            }
        }

        Log.d(TAG, "[Enter] broadcastUpdate(ACTION_DEVICE_NOT_FOUND)")
        broadcastUpdate(ACTION_DEVICE_NOT_FOUND)
    }

    fun disconnectDevice() {
        Log.d(TAG, "[Enter] disconnectDevice()")
        if (bluetoothGatt != null) {
            bluetoothGatt!!.close()
            bluetoothGatt = null
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }

                    broadcastUpdate(ACTION_GATT_CONNECTED, status.toString())


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Successfully disconnected from $deviceAddress")

                    broadcastUpdate(ACTION_GATT_DISCONNECTED)

                    gatt.close()
                    bluetoothGatt = null
                }
            } else {
                Log.d(TAG, "Error $status encountered for $deviceAddress! Disconnecting...")
                broadcastUpdate(ACTION_GATT_NOT_SUCCESS, "Error $status encountered for $deviceAddress! Disconnecting...")

                gatt.close()
                bluetoothGatt = null
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.d(TAG, "Discovered ${services.size} services for ${device.address}")
                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
                enableNotifications()

//                Handler(Looper.getMainLooper()).postDelayed({
//                    writeCharacteristic()
//                }, 3000)

            }
        }

        override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.d(TAG, "Read characteristic $uuid:\n${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.d(TAG, "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.d(TAG, "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to characteristic $uuid | value: ${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e(TAG, "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG, "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic write failed for $uuid, error: $status")
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "[Enter] onCharacteristicChanged")

            with(characteristic) {
                Log.i(TAG, "Characteristic $uuid changed | value: ${value.toHexString()}")
                handler.removeCallbacks(verificationTask!!)
                Log.d(TAG, "[Enter] handler.removeCallbacks(verificationTask!!)")

                broadcastUpdate(ACTION_VERIFICATION_SUCCESS)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to descriptor ${this?.uuid} | value: ${this?.value?.toHexString()}")
                        writeCharacteristic()
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e(TAG, "Write exceeded connection ATT MTU!")
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e(TAG, "Write not permitted for ${this?.uuid}!")
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write not permitted for ${this?.uuid}!")
                    }
                    else -> {
                        Log.e(TAG, "descriptor write failed for ${this?.uuid}, error: $status")
                        broadcastUpdate(ACTION_CONNECT_FAILED, "descriptor write failed for ${this?.uuid}, error: $status")
                    }
                }
            }
        }
    }

    private fun enableNotifications() {
        Log.d(TAG, "[Enter] enableNotifications")
        val characteristic = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_READ_UUID)
        val payload = when {
            characteristic?.isIndicatable() == true -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic?.isNotifiable() == true -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e(TAG, "${characteristic?.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(CCCD_UUID)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                broadcastUpdate(ACTION_CONNECT_FAILED, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun writeCharacteristic() {
        Log.d(TAG, "[Enter] writeCharacteristic")

        val characteristic = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        val writeType = when {
            characteristic?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic?.isWritableWithoutResponse() == true -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic?.uuid} cannot be written to")
        }

        Log.d(TAG, "characteristic?.isWritable(): ${characteristic.isWritable()}")
        Log.d(TAG, "characteristic?.isWritableWithoutResponse(): ${characteristic.isWritableWithoutResponse()}")

//        val checksumArray = byteArrayOf(0xFA.toByte(), 0x00, 0x17, 0x10, 0x11, 0x4a, 0x43, 0x46, 0x32, 0x30, 0x32, 0x31, 0x30, 0x33, 0x30, 0x32, 0x48, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x12, 0x00)
//        val payload =       byteArrayOf(0xFA.toByte(), 0x00, 0x17, 0x10, 0x11, 0x4a, 0x43, 0x46, 0x32, 0x30, 0x32, 0x31, 0x30, 0x33, 0x30, 0x32, 0x48, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x12, 0x00, checkSum.toByte(), 0xFF.toByte())
//        val payload = getPayload()

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = getPayload()
            startVerificationTimer()
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    private fun startVerificationTimer() {
        Log.d(TAG, "[Enter] startVerificationTimer")

        verificationTask = Runnable {
//            _connectFailedLog.value = "verification failed"
            Log.d(TAG, "[Enter] broadcastUpdate(ACTION_VERIFICATION_FAILED)")

            broadcastUpdate(ACTION_VERIFICATION_FAILED)
        }
        handler.postDelayed(verificationTask!!, 5000)
    }

    private fun getPayload(): ByteArray {
        val checksumArray = getCheckSumArray()
//        Log.d(TAG, "checksumArray: ${checksumArray.toHexString()}")
        val checkSum = getCheckSum(checksumArray)
        return checksumArray + checkSum.toByte() + 0xFF.toByte()
    }

    private fun getCheckSumArray(): ByteArray {
        val snInAscii = snToAscii()
        val instrucLen = (snInAscii.size + 4).toByte()
        return byteArrayOf(0xFA.toByte()) + 0x00 + instrucLen + 0x10 + 0x11 + snInAscii + 0x12 + 0x00
    }

    private fun getCheckSum(chars: ByteArray): Int {
        var XOR = 0
        for (element in chars) {
            XOR = XOR xor element.toInt()
        }
        return XOR
    }

    private fun snToAscii(): ByteArray {
        val array = ByteArray(serialNumberInput!!.length)
        val snCharArray = serialNumberInput!!.toCharArray()
        for (i in serialNumberInput!!.indices) {
            val ascii = snCharArray[i].toInt().toByte()
            array[i] = ascii
//            Log.d(TAG, "ascii: $ascii")
        }
        return array
    }

    private fun inputSerialNumberToMD5(): String {
        return MD5.convertMD5(serialNumberInput)
    }

    private fun getScanDeviceSN(scanResult: ScanResult): String {
        val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(MANUFACTURER_ID)?.toHexString()
        val temp = manufacturerData?.split(" ")
        val temp2 = temp?.subList(4, temp.size)

        val strbul = StringBuilder()
        for (str in temp2!!) {
            strbul.append(str)
        }
        return strbul.toString()
    }

    private fun broadcastUpdate(action: String, message: String = "") {
        val intent = Intent(action)
        if (message.isNotEmpty()) {
            intent.putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.d(TAG, "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                    separator = "\n|--",
                    prefix = "|--"
            ) { it.uuid.toString() }
            Log.d(TAG, "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            val result = gatt.writeDescriptor(descriptor)
            Log.d(TAG, "writeDescriptor result: $result")
            if (!result) {
                broadcastUpdate(ACTION_CONNECT_FAILED, "writeDescriptor result: $result")
            }
        } ?: error("Not connected to a BLE device!")
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
            containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)


}