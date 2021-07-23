package com.ulsee.mower.ble

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
import com.ulsee.mower.ble.CommandChargingPath.Companion.CHARGING_PATH
import com.ulsee.mower.ble.CommandDeleteChargingPath.Companion.DELETE_CHARGING_PATH
import com.ulsee.mower.ble.CommandDeleteGrass.Companion.DELETE_GRASS
import com.ulsee.mower.ble.CommandDeleteGrassPath.Companion.DELETE_GRASS_PATH
import com.ulsee.mower.ble.CommandDeleteObstacle.Companion.DELETE_OBSTACLE
import com.ulsee.mower.ble.CommandGlobalParameter.Companion.GLOBAL_PARAMETER
import com.ulsee.mower.ble.CommandGrassBoundary.Companion.GRASS_BOUNDARY
import com.ulsee.mower.ble.CommandGrassPath.Companion.GRASS_PATH
import com.ulsee.mower.ble.CommandObstacleBoundary.Companion.OBSTACLE_BOUNDARY
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_CONNECT_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_DEVICE_NOT_FOUND
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_CONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_DISCONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_NOT_SUCCESS
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_FAILED
import com.ulsee.mower.data.BLECommandTable
import com.ulsee.mower.utils.MD5
import com.ulsee.mower.utils.Utils
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private val TAG = BluetoothLeService::class.java.simpleName

private const val MANUFACTURER_ID = 741
private val SERVICE_UUID = UUID.fromString("0000abf0-0000-1000-8000-00805f9b34fb")
private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("0000abf1-0000-1000-8000-00805f9b34fb")
private val CHARACTERISTIC_READ_UUID = UUID.fromString("0000abf2-0000-1000-8000-00805f9b34fb")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private const val GATT_MAX_MTU_SIZE = 512

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
//    private var bleState: BLEState = RobotListState()
//    private var isMovingState = false

    private val operationQueue = ConcurrentLinkedQueue<BleOperationType>()
    private var pendingOperation: BleOperationType? = null
    private var commandSerialNumber : Byte = 0
    private var commandPayload = ByteArray(0)

    private var verificationTask: Runnable? = null
    private var commandTimeoutTask: Runnable? = null
    private var getStatusTask: Runnable? = null
    private var getMowingDataTask: Runnable? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var serviceLooper: Looper? = null
    private lateinit var moveHandler: Handler
    private lateinit var statusHandler: StatusHandler
    private lateinit var mowingDataHandler: Handler


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
        abstract var lambda: ()-> Unit

        class CONNECT(override var lambda: ()-> Unit) : BleOperationType()
        class DISCONNECT(override var lambda: ()-> Unit): BleOperationType()
        class DISCOVER_SERVICE(override var lambda: () -> Unit): BleOperationType()
        class CHARACTERISTIC_WRITE(var payload: ByteArray, override var lambda: () -> Unit) : BleOperationType()
        class DESCRIPTOR_WRITE(override var lambda: () -> Unit) : BleOperationType()
        class MTU_REQUEST(override var lambda: () -> Unit) : BleOperationType()
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

        HandlerThread("mowingDataThread", android.os.Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
//            serviceLooper = looper
            mowingDataHandler = Handler(looper)
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

    fun cancelStatusTask() {
        getStatusTask?.let {
            statusHandler.removeCallbacks(it)
        }
    }

    fun getStatusPeriodically() {
        getStatusTask?.let {
            statusHandler.removeCallbacks(it)
        }
        getStatusTask = Runnable {
            val payload = CommandStatus(this).getSendPayload()
            enqueueCommand(payload)
            statusHandler.postDelayed(getStatusTask!!, 1000)
        }

        statusHandler.post(getStatusTask!!)
    }

    fun moveRobot(rotation: Int, movement: Double) {
        moveHandler.postDelayed({
            val payload = CommandMove(this@BluetoothLeService, rotation, movement).getSendPayload()
            enqueueOperation(BleOperationType.CHARACTERISTIC_WRITE(payload) {
                writeCharacteristic(payload, true)
            })
        }, 50)
    }

    fun recordBoundary(command: Int, subject: Int) {
        val payload = CommandRecordBoundary(this).getSendPayload(command, subject)
        enqueueCommand(payload)
    }

    fun startStop(command: Int) {
        val payload = CommandStartStop(this).getSendPayload(command)
        enqueueCommand(payload)
    }

    fun getMapGlobalParameters() {
        val payload = CommandGlobalParameter(this).getSendPayload()
        enqueueCommand(payload)
    }

    fun getGrassBoundary(grassNumber: Byte, packetNumber: Byte) {
        val payload = CommandGrassBoundary(this).getSendPayload(grassNumber, packetNumber)
        enqueueCommand(payload)
    }

    fun getObstacleBoundary(grassNumber: Byte, packetNumber: Byte, obstacleNumber: Byte) {
        val payload = CommandObstacleBoundary(this).getSendPayload(grassNumber, packetNumber, obstacleNumber)
        enqueueCommand(payload)
    }

    fun getGrassPath(grassNumber: Byte, packetNumber: Byte, targetNumber: Byte, pathNumber: Byte) {
        val payload = CommandGrassPath(this).getSendPayload(grassNumber, packetNumber, targetNumber, pathNumber)
        enqueueCommand(payload)
    }

    fun getChargingPath(grassNumber: Byte, packetNumber: Byte, pathNumber: Byte) {
        val payload = CommandChargingPath(this).getSendPayload(grassNumber, packetNumber, pathNumber)
        enqueueCommand(payload)
    }

    fun deleteGrass(grassNumber: Byte) {
        val payload = CommandDeleteGrass(this).getSendPayload(grassNumber)
        enqueueCommand(payload)
    }

    fun deleteObstacle(grassNumber: Byte, obstacleNumber: Byte) {
        val payload = CommandDeleteObstacle(this).getSendPayload(grassNumber, obstacleNumber)
        enqueueCommand(payload)
    }

    fun deleteChargingPath(grassNumber: Byte, pathNumber: Byte) {
        val payload = CommandDeleteChargingPath(this).getSendPayload(grassNumber, pathNumber)
        enqueueCommand(payload)
    }

    fun deleteGrassPath(grassNumber: Byte, targetGrassNumber: Byte, pathNumber: Byte) {
        val payload = CommandDeleteGrassPath(this).getSendPayload(grassNumber, targetGrassNumber, pathNumber)
        enqueueCommand(payload)
    }

    fun getMowingData(packetNumber: Int) {
        getMowingDataTask?.let {
            mowingDataHandler.removeCallbacks(it)
        }

        getMowingDataTask = Runnable {
            val payload = CommandMowingData(this).getSendPayload(packetNumber)
            enqueueCommand(payload)
            mowingDataHandler.postDelayed(getMowingDataTask!!, 5000)
        }

        mowingDataHandler.post(getMowingDataTask!!)
    }

    private fun enqueueCommand(payload: ByteArray) {
        enqueueOperation(BleOperationType.CHARACTERISTIC_WRITE(payload) {
            writeCharacteristic(payload)
        })
    }

    private fun enqueuePreemptiveCommand(payload: ByteArray) {
        Log.d(TAG, "[Enter] enqueuePreemptiveCommand()")
        val commandList = operationQueue.toMutableList()

        updateQueueCommandSN(commandList)

        addPreemptiveCommandToFirst(commandList, payload)

        operationQueue.clear()
        operationQueue.addAll(commandList)

        signalEndOfOperation()

    }

    private fun addPreemptiveCommandToFirst(commandList: MutableList<BleOperationType>, payload: ByteArray) {
        commandList.add(0, BleOperationType.CHARACTERISTIC_WRITE(payload) {
            writeCharacteristic(payload)
        })
    }

    private fun updateQueueCommandSN(commandList: MutableList<BleOperationType>) {
        commandList.forEach {
            when (it) {
                is BleOperationType.CHARACTERISTIC_WRITE -> {
                    val oldPayload = it.payload
                    val instructionType = oldPayload[3]
                    if (instructionType == BLECommandTable.MAP_DATA.toByte() || instructionType == BLECommandTable.MOWING_DATA.toByte()) {              // TODO 获取小车最新涂覆数据及升级指令帧
                        val snIndex = 1
                        val newPayload = oldPayload.clone()
                        newPayload[snIndex] = (oldPayload[snIndex] + 1).toByte()

                        val checkSumArray = newPayload.sliceArray(IntRange(0, newPayload.size - 3))
                        val checksumIdx = newPayload.size - 2
                        newPayload[checksumIdx] = getCheckSum(checkSumArray).toByte()

                        it.payload = newPayload
                        it.lambda = {
                            writeCharacteristic(newPayload)
                        }
                    }
                }
                else -> {
                    Log.d(
                        TAG,
                        "is NOT BleOperationType.CHARACTERISTIC_WRITE ============================="
                    )
                }
            }

        }
    }

    fun getCheckSum(chars: ByteArray): Int {
        var XOR = 0
        for (element in chars) {
            XOR = XOR xor element.toInt()
        }
        return XOR
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

//                    getStatusPeriodically()

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
//            Log.d(TAG, "[Enter] onCharacteristicWrite")

            with(characteristic) {

                // move command is writing without response, so signal end of operation
                // must be executed here.
                val instructionType = value[3].toInt()
                if (instructionType == BLECommandTable.MOVE) {
                    signalEndOfOperation()
                }

                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
//                        Log.d(TAG, "Wrote to characteristic $uuid | value: ${value.toHexString()}")
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
        }

        override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
        ) {

            with(characteristic) {
                val instructionType = value[3].toInt()
                if (instructionType != BLECommandTable.STATUS) {
                    Log.d(TAG, "instructionType: $instructionType")
                    Log.d(TAG, "instructionType2: ${value[3]}")
                    Log.d(TAG, "Characteristic changed | value: ${value.toHexString()}")
                    Log.d("888", "instructionType: $instructionType")
//                    Log.d("888", "Characteristic changed | value: ${value.toHexString()}")
                }

                val command = getCommandInstance(instructionType)
                command.receive(value)

                if (instructionType == BLECommandTable.VERIFICATION || instructionType == BLECommandTable.MOVE || instructionType == BLECommandTable.STATUS) {
                    signalEndOfOperation()

                } else {

                    val serialNumber = value[1]
                    val isSerialNumberValid = checkSerialNumber(serialNumber)
                    if (isSerialNumberValid) {

                        if (isAllPacketsObtained(instructionType)) {          // TODO 获取小车最新涂覆数据及升级指令帧
                            signalEndOfOperation()

                        } else {
                            Log.d(TAG, "All packets are not obtained. Please wait..............................................")

                            configNewPayload(serialNumber, command, instructionType)

                            enqueuePreemptiveCommand(commandPayload)
                        }

                    } else {
                        // TODO
                        Log.d(TAG, "[Enter] serialNumber != commandSerialNumber ......................................................")
                    }

                }
            }
        }

        private fun BluetoothGattCharacteristic.configNewPayload(serialNumber: Byte, command: AbstractCommand, instructionType: Int) {
            Log.d("888", "[Before] commandPayload: ${commandPayload.toHexString()}")

            // update packet number
            when (instructionType) {
                BLECommandTable.MAP_DATA -> {
                    val packetReceivedIdx = value.indexOf(0x63) + 1
                    val packetNumber = value[packetReceivedIdx]
                    val packetSendIdx = commandPayload.indexOf(0x62) + 1
                    commandPayload[packetSendIdx] = (packetNumber + 1).toByte()
                }
                BLECommandTable.MOWING_DATA -> {
                    Log.d("888", "[Enter] BLECommandTable.MOWING_DATA -> ")
                    val tempArray = byteArrayOf(value[10]) + value[11]
                    val packetNumber = Utils.convert(tempArray).toShort()
                    val byteArray = Utils.intToBytes((packetNumber + 1).toShort())

                    Log.d("888", "[Enter] configNewPayload() packetNumber: $packetNumber byteArray: ${byteArray.toHexString()}")

                    val packetSendIdx = 5
                    commandPayload[packetSendIdx] = byteArray[0]
                    commandPayload[packetSendIdx + 1] = byteArray[1]

                }
                else -> {
                    Log.d("888", "[Enter] configNewPayload() else section................")
                }
            }

            // update serial number
            val snIndex = 1
            commandPayload[snIndex] = (serialNumber + 1).toByte()
            AbstractCommand.serialNumber++

            // update checksum
            val checkSumArray = commandPayload.sliceArray(IntRange(0, commandPayload.size - 3))
            val checksumIdx = commandPayload.size - 2
            commandPayload[checksumIdx] = command.getCheckSum(checkSumArray).toByte()

            Log.d("888", "[After] commandPayload: ${commandPayload.toHexString()}")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            with(descriptor) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Wrote to descriptor ${this?.uuid} | value: ${this?.value?.toHexString()}")
                        startVerificationTimer()

                        val payload = CommandVerification(this@BluetoothLeService, serialNumberInput!!).getSendPayload()
                        enqueueOperation(BleOperationType.CHARACTERISTIC_WRITE(payload) {
                            writeCharacteristic(payload)
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

    private fun checkSerialNumber(serialNumber: Byte): Boolean = (serialNumber == commandSerialNumber)


    private fun BluetoothGattCharacteristic.isAllPacketsObtained(instructionType: Int): Boolean {
//        Log.d("888", "[Enter] isAllPacketsObtained() instructionType: $instructionType")
        var isAllPacketObtained = true
        if (instructionType == BLECommandTable.MAP_DATA) {
            val packetCount = value[7]
            val packetNumber = value[9].toInt()
            isAllPacketObtained = (packetNumber == packetCount - 1)
        } else if (instructionType == BLECommandTable.MOWING_DATA) {
//            Log.d("888", "instructionType == BLECommandTable.MOWING_DATA")
            val packetCountArray = byteArrayOf(value[7]) + value[8]
            val packetCount = Utils.convert(packetCountArray).toInt()
            val packetNumberArray = byteArrayOf(value[10]) + value[11]
            val packetNumber = Utils.convert(packetNumberArray).toInt()
            Log.d("888", "packetCount: $packetCount packetNumber: $packetNumber")

            isAllPacketObtained = (packetNumber == packetCount - 1)
        }
        return isAllPacketObtained
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
        if (payload.size < 4) {
            return
        }
        if (payload[3].toInt() == 0x70) {
            val type = when (payload[5].toInt()) {
                0x00 -> "[START RECORD]"
                0x01 -> "[FINISH RECORD]"
                0x02 -> "[START POINT RECORD]"
                0x03 -> "[SET POINT]"
                0x04 -> "[FINISH SET POINT]"
                0x05 -> "[CANCEL RECORD]"
                0x06 -> "[SAVE BOUNDARY]"
                0x07 -> "[DISCARD BOUNDARY]"
                else -> "[NULL]"
            }
            Log.d("123", "[Enter] writeCharacteristic() instruction: $type")
        }

        val showLog = (payload[3].toInt() != BLECommandTable.STATUS) && (payload[3].toInt() != BLECommandTable.MOVE)
        if (showLog) {
            Log.d(TAG, "[Enter] writeCharacteristic() payload: ${payload.toHexString()}")
        }
        val serialNumber = payload[1]
//        Log.d(TAG, "serialNumber: $serialNumber")
        if (serialNumber.toInt() != 0) {         // do not consider the verification, move robot and status command
            commandSerialNumber = serialNumber
            commandPayload = payload
        }
//        Log.d(TAG, "commandSerialNumber: $commandSerialNumber")

        if (bluetoothGatt == null) {
            Toast.makeText(this, "disconnected state", Toast.LENGTH_SHORT).show()
            return
        }
        val characteristic = bluetoothGatt?.getService(SERVICE_UUID)
            ?.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        val writeType = when {
            characteristic?.isWritable() == true -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic?.isWritableWithoutResponse() == true -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic?.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = if (isWriteWithoutResponse) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
//                Log.d(TAG, "WRITE_TYPE_NO_RESPONSE")
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//                Log.d(TAG, "WRITE_TYPE_DEFAULT")
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
//        Log.d(TAG, "[Enter] enqueueOperation $operation")
        operationQueue.add(operation)
        if (pendingOperation == null) {
            doNextOperation()
        }
//        else {
//            Log.d(TAG, "pendingOperation != null")
//        }
    }

    @Synchronized
    private fun doNextOperation() {
//        Log.d(TAG, "[Enter] doNextOperation")

        if (pendingOperation != null) {
            Log.e(TAG, "doNextOperation() called when an operation is pending! Aborting.")
            return
        }

        val operation = operationQueue.poll() ?: run {
            Log.d(TAG, "Operation queue empty, returning")
            return
        }
        pendingOperation = operation

        startCommandTimeoutTimer()

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
//        Log.d(TAG, "[Enter] signalEndOfOperation() serialNumber: $commandSerialNumber")
        pendingOperation = null

        handler.removeCallbacks(commandTimeoutTask!!)

        if (operationQueue.isNotEmpty()) {
            doNextOperation()
        }
    }

    private fun startVerificationTimer() {
//        Log.d(TAG, "[Enter] startVerificationTimer")
        verificationTask = Runnable {
            broadcastUpdate(ACTION_VERIFICATION_FAILED)
        }
        handler.postDelayed(verificationTask!!, 5000)
    }

    private fun startCommandTimeoutTimer() {
//        Log.d(TAG, "[Enter] startVerificationTimer")
        commandTimeoutTask = Runnable {
            Log.d(TAG, "[Enter] resend command due to timeout")
            Log.d("123", "[Enter] resend command due to timeout")
            // command送出3秒後，逾時未回應則重送
            writeCharacteristic(commandPayload)
            handler.post {
                Toast.makeText(this, "resend command due to timeout", Toast.LENGTH_SHORT).show()
            }
//            signalEndOfOperation()
        }
        handler.postDelayed(commandTimeoutTask!!, 5000)
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

    fun broadcastUpdate(action: String, message: String = "") {
        Log.d(TAG, "[Enter] broadcastUpdate() action: $action message: $message")
        val intent = Intent(action)
        if (message.isNotEmpty()) {
            intent.putExtra("message", message)
        }
        sendBroadcast(intent)
    }

    private fun BluetoothGattCharacteristic.getCommandInstance(instructionType: Int): AbstractCommand {
        return when (instructionType) {
            BLECommandTable.VERIFICATION -> {
                handler.removeCallbacks(verificationTask!!)
                CommandVerification(this@BluetoothLeService, serialNumberInput!!)
            }
            BLECommandTable.SCHEDULE -> {
                CommandSchedule(this@BluetoothLeService)
            }
            BLECommandTable.START_STOP -> {
                CommandStartStop(this@BluetoothLeService)
            }
            BLECommandTable.STATUS -> {
                CommandStatus(this@BluetoothLeService)
            }
            BLECommandTable.MAP_DATA -> {
                if (value[5].toInt() == 0x01) {
                    val elementType = value[11].toInt()
                    when (elementType) {
                        GLOBAL_PARAMETER -> CommandGlobalParameter(this@BluetoothLeService)
                        GRASS_BOUNDARY -> CommandGrassBoundary(this@BluetoothLeService)
                        OBSTACLE_BOUNDARY -> CommandObstacleBoundary(this@BluetoothLeService)
                        CHARGING_PATH -> CommandChargingPath(this@BluetoothLeService)
                        GRASS_PATH -> CommandGrassPath(this@BluetoothLeService)
                        else -> CommandNull(this@BluetoothLeService)
                    }
                } else {
                    // TODO
                    Log.d(TAG, "command response error...")
                    Toast.makeText(
                        this@BluetoothLeService,
                        "command response error",
                        Toast.LENGTH_SHORT
                    ).show()
                    CommandNull(this@BluetoothLeService)
                }
            }
            BLECommandTable.RECORD_BOUNDARY -> {
                val type = when (value[7].toInt()) {
                    0x00 -> "[START RECORD]"
                    0x01-> "[FINISH RECORD]"
                    0x02-> "[START POINT RECORD]"
                    0x03-> "[SET POINT]"
                    0x04-> "[FINISH SET POINT]"
                    0x05-> "[CANCEL RECORD]"
                    0x06-> "[SAVE BOUNDARY]"
                    0x07-> "[DISCARD BOUNDARY]"
                    else -> "[NULL]"
                }
                Log.d("123", "[Enter] onCharacteristicChanged() instruction: $type")

                CommandRecordBoundary(this@BluetoothLeService)

            }
            BLECommandTable.SETTINGS -> {
                CommandSettings(this@BluetoothLeService)
            }
            BLECommandTable.DELETE_MAP -> {
                val commandType = value[7].toInt()
                when (commandType) {
                    DELETE_GRASS -> CommandDeleteGrass(this@BluetoothLeService)
                    DELETE_OBSTACLE -> CommandDeleteObstacle(this@BluetoothLeService)
                    DELETE_CHARGING_PATH -> CommandDeleteChargingPath(this@BluetoothLeService)
                    DELETE_GRASS_PATH -> CommandDeleteGrassPath(this@BluetoothLeService)
                    else -> CommandNull(this@BluetoothLeService)
                }
            }
            BLECommandTable.MOWING_DATA -> {
                if (value[5].toInt() == 0x01) {
                    CommandMowingData(this@BluetoothLeService)
                } else {
                    Log.d("888", "[异常] BLECommandTable.MOWING_DATA")
                    CommandNull(this@BluetoothLeService)
                }
            }
            else ->
                CommandNull(this@BluetoothLeService)
        }
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

//        private fun getCheckSum2(byteArray: ByteArray): Int {
//            var xorChecksum: Int = 0
//            for (element in byteArray) {
//                xorChecksum = xorChecksum xor element.toInt()
//            }
////            Log.d("test123", "Checksum Via xoring is : $xorChecksum")
//            return xorChecksum.toInt()
//        }
//
//        private fun verifyChecksum(byteArray: ByteArray): Int {
//            val sliceArray = byteArray.sliceArray(0.. (byteArray.size-3))
//            Log.d("test123", "sliceArray: ${sliceArray.toHexString()}")
//            val checkSum = getCheckSum2(sliceArray)
//            Log.d("test123", "checkSum: $checkSum")
//            return checkSum
//        }

}