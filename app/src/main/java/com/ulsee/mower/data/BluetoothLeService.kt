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
import android.widget.Toast
import com.ulsee.mower.utils.MD5
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

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
const val ACTION_STATUS_RESPONSE = "action_status_response"

private const val GATT_MAX_MTU_SIZE = 200

class BluetoothLeService : Service() {

    private val binder = LocalBinder()

    val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    private var scanResults: ArrayList<ScanResult> = ArrayList()
    private var bluetoothGatt: BluetoothGatt? = null

    private var serialNumberInput: String? = null

    // TODO update value when gatt is closed
    private var bleState: BLEState = RobotListState()
//    private var isMovingState = false

    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private var pendingOperation: BleOperationType? = null

    private var verificationTask: Runnable? = null
    private var getStatusTask: Runnable? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var serviceLooper: Looper? = null
    private lateinit var moveHandler: Handler
    private lateinit var statusHandler: StatusHandler


    // Handler that receives messages from the thread
    private inner class StatusHandler(looper: Looper) : Handler(looper) {

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

    sealed class BleOperationType {
        abstract val lambda: ()-> Unit

        class CONNECT(override val lambda: ()-> Unit) : BleOperationType()
        class DISCONNECT(override val lambda: ()-> Unit): BleOperationType()
        class DISCOVER_SERVICE(override val lambda: () -> Unit): BleOperationType()
        class CHARACTERISTIC_WRITE(override val lambda: () -> Unit) : BleOperationType()
        class DESCRIPTOR_WRITE(override val lambda: () -> Unit) : BleOperationType()
        class MTU_REQUEST(override val lambda: () -> Unit) : BleOperationType()
    }

    override fun onCreate() {
        HandlerThread("moveThread", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            moveHandler = Handler(looper)
        }

        HandlerThread("statusThread", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
//            serviceLooper = looper
            statusHandler = StatusHandler(looper)
        }
    }

    override fun onBind(intent: Intent): IBinder {
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
                        val data = scanRecord.getManufacturerSpecificData(manufacturerId)?.toHexString()
                        Log.d(TAG, "manufacturerId: $manufacturerId manufacturerData: $data")

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
                enqueueOperation(BleOperationType.CONNECT {
                    i.device.connectGatt(this, false, gattCallback)
                })

                return
            }
        }

        broadcastUpdate(ACTION_DEVICE_NOT_FOUND)
    }

    fun disconnectDevice() {
        Log.d(TAG, "[Enter] disconnectDevice()")
        enqueueOperation(BleOperationType.DISCONNECT {
            if (bluetoothGatt != null) {
                bluetoothGatt!!.close()
                bluetoothGatt = null
            }
            pendingOperation = null
            operationQueue.clear()
            getStatusTask?.let { statusHandler.removeCallbacks(it) }
        })
    }

    fun getStatus() {
        getStatusTask = Runnable {
            enqueueOperation(BleOperationType.CHARACTERISTIC_WRITE {
                writeCharacteristic(getStatusPayload())
            })
            statusHandler.postDelayed(getStatusTask!!, 3000)
        }

        statusHandler.post(getStatusTask!!)
    }

    fun moveRobot(rotation: Int, movement: Double) {
        moveHandler.postDelayed({
            writeCharacteristic(getMovePayload(rotation, movement), true)
        }, 50)
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt

                    if (pendingOperation is BleOperationType.CONNECT) {
                        signalEndOfOperation()
                    }

                    enqueueOperation(BleOperationType.MTU_REQUEST {
                        bluetoothGatt!!.requestMtu(GATT_MAX_MTU_SIZE)
                    })

                    enqueueOperation(BleOperationType.DISCOVER_SERVICE {
                        bluetoothGatt?.discoverServices()
                    })

                    broadcastUpdate(ACTION_GATT_CONNECTED, status.toString())

                    getStatus()

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Successfully disconnected from $deviceAddress")

                    broadcastUpdate(ACTION_GATT_DISCONNECTED)

                    gatt.close()
                    bluetoothGatt = null
                    pendingOperation = null
                    operationQueue.clear()
                    getStatusTask?.let { statusHandler.removeCallbacks(it) }

                }
            } else {
                broadcastUpdate(ACTION_GATT_NOT_SUCCESS, "Error $status encountered for $deviceAddress! Disconnecting...")

                gatt.close()
                bluetoothGatt = null
                pendingOperation = null
                operationQueue.clear()
                getStatusTask?.let { statusHandler.removeCallbacks(it) }

            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.d(TAG, "Discovered ${services.size} services for ${device.address}")
                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here

                if (pendingOperation is BleOperationType.DISCOVER_SERVICE) {
                    signalEndOfOperation()
                }

                enqueueOperation(BleOperationType.DESCRIPTOR_WRITE {
                    enableNotifications()
                })

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
                        Log.d(TAG, "Wrote to characteristic $uuid | value: ${value.toHexString()}")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write not permitted for $uuid!")
                    }
                    else -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }

            if (pendingOperation is BleOperationType.CHARACTERISTIC_WRITE) {
                signalEndOfOperation()
            }
        }

        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {
            Log.d(TAG, "[Enter] onCharacteristicChanged")

            with(characteristic) {
                Log.i(TAG, "Characteristic changed | value: ${value.toHexString()}")

//                broadcastUpdate(ACTION_VERIFICATION_SUCCESS)
//                bleState.onCharacteristicChanged(this@BluetoothLeService, value.toHexString())
//                bleState = bleState.getNextState()

                val instructionType = value[3].toInt()
                Log.d(TAG, "instructionType: $instructionType")
                when (instructionType) {
                    16 -> {
                        broadcastUpdate(ACTION_VERIFICATION_SUCCESS, value.toHexString())
                        handler.removeCallbacks(verificationTask!!)
                    }
                    80 -> {
                        val xByteArray = byteArrayOf(value[6]) + value[7] + value[8] + value[9]
                        val yByteArray = byteArrayOf(value[10]) + value[11] + value[12] + value[13]
                        val angle = byteArrayOf(value[15]) + value[16]
                        broadcastUpdate(ACTION_STATUS_RESPONSE, xByteArray, yByteArray, angle)
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to descriptor ${this?.uuid} | value: ${this?.value?.toHexString()}")
                        startVerificationTimer()

                        enqueueOperation(BleOperationType.CHARACTERISTIC_WRITE {
                            writeCharacteristic(getVerificationPayload())
                        })

                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "Write not permitted for ${this?.uuid}!")
                    }
                    else -> {
                        broadcastUpdate(ACTION_CONNECT_FAILED, "descriptor write failed for ${this?.uuid}, error: $status")
                    }
                }
            }
            if (pendingOperation is BleOperationType.DESCRIPTOR_WRITE) {
                signalEndOfOperation()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
            if (pendingOperation is BleOperationType.MTU_REQUEST) {
                signalEndOfOperation()
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
                broadcastUpdate(ACTION_CONNECT_FAILED, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun writeCharacteristic(payload: ByteArray, isWriteWithoutResponse: Boolean = false) {
        Log.d(TAG, "[Enter] writeCharacteristic")

        if (bluetoothGatt == null) {
            Toast.makeText(this, "disconnected state", Toast.LENGTH_SHORT).show()
            return
        }
        val characteristic = bluetoothGatt?.getService(SERVICE_UUID)?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        val writeType = when {
            characteristic?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic?.isWritableWithoutResponse() == true -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic?.uuid} cannot be written to")
        }

//        Log.d(TAG, "characteristic?.isWritable(): ${characteristic.isWritable()}")
//        Log.d(TAG, "characteristic?.isWritableWithoutResponse(): ${characteristic.isWritableWithoutResponse()}")

//        val checksumArray = byteArrayOf(0xFA.toByte(), 0x00, 0x17, 0x10, 0x11, 0x4a, 0x43, 0x46, 0x32, 0x30, 0x32, 0x31, 0x30, 0x33, 0x30, 0x32, 0x48, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x12, 0x00)
//        val payload =       byteArrayOf(0xFA.toByte(), 0x00, 0x17, 0x10, 0x11, 0x4a, 0x43, 0x46, 0x32, 0x30, 0x32, 0x31, 0x30, 0x33, 0x30, 0x32, 0x48, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x12, 0x00, checkSum.toByte(), 0xFF.toByte())
//        val payload = getPayload()

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = if (isWriteWithoutResponse) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                Log.d(TAG, "WRITE_TYPE_NO_RESPONSE")
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                Log.d(TAG, "WRITE_TYPE_DEFAULT")
            }

            characteristic.value = payload
//            startVerificationTimer()
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            val result = gatt.writeDescriptor(descriptor)
//            Log.d(TAG, "writeDescriptor result: $result")
            if (!result) {
                broadcastUpdate(ACTION_CONNECT_FAILED, "writeDescriptor result: $result")
            }
        } ?: error("Not connected to a BLE device!")
    }

    @Synchronized
    private fun enqueueOperation(operation: BleOperationType) {
        Log.d(TAG, "[Enter] enqueueOperation $operation")
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
    }

    @Synchronized
    private fun doNextOperation() {
        Log.d(TAG, "[Enter] doNextOperation")

        if (pendingOperation != null) {
            Log.e(TAG, "doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.d(TAG, "Operation queue empty, returning")
            return
        }
        pendingOperation = operation

        when (operation) {
            is BleOperationType.CONNECT -> operation.lambda()
            is BleOperationType.DISCONNECT -> operation.lambda()
            is BleOperationType.DISCOVER_SERVICE -> operation.lambda()
            is BleOperationType.CHARACTERISTIC_WRITE -> operation.lambda()
            is BleOperationType.DESCRIPTOR_WRITE -> operation.lambda()
            is BleOperationType.MTU_REQUEST -> operation.lambda()
        }
    }

    @Synchronized
    private fun signalEndOfOperation() {
        Log.d(TAG, "[Enter] signalEndOfOperation() End of $pendingOperation")
        pendingOperation = null
        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    private fun startVerificationTimer() {
        Log.d(TAG, "[Enter] startVerificationTimer")

        verificationTask = Runnable {
            broadcastUpdate(ACTION_VERIFICATION_FAILED)
        }
        handler.postDelayed(verificationTask!!, 5000)
    }

    private fun getMovePayload(rotation: Int, movement: Double): ByteArray {
//        val rotation2 = integerToTwoBytes(rotation)
//        Log.d(TAG, "rotation2: ${rotation2.toHexString()}")

        val checksumArray = byteArrayOf(0xFA.toByte()) + 0x00 + 0x06 + 0x20 + 0x21 + integerToTwoBytes(rotation) + 0x22 + 0x32
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    private fun integerToTwoBytes(value: Int): ByteArray {
        val result = ByteArray(2)
//        if (value > Math.pow(2.0, 31.0) || value < 0) {
//            throw UtilityException("Integer value $value is larger than 2^31")
//        }
        result[1] = (value ushr 8 and 0xFF).toByte()
        result[0] = (value and 0xFF).toByte()
        return result
    }

    private fun getStatusPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + 0x00 + 0x02 + 0x50 + 0x00
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    private fun getVerificationPayload(): ByteArray {
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
        Log.d(TAG, "[Enter] broadcastUpdate() action: $action message: $message")
        val intent = Intent(action)
        if (message.isNotEmpty()) {
            intent.putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, x: ByteArray, y: ByteArray, angle: ByteArray) {
//        Log.d(TAG, "[Enter] broadcastUpdate() action: $action message: $message")
        val intent = Intent(action)
//        if (message.isNotEmpty()) {
        intent.putExtra("x", x)
        intent.putExtra("y", y)
        intent.putExtra("angle", angle)
//        }
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