package com.ulsee.mower.ble

import android.util.Log
import com.ulsee.mower.ui.map.toHexString
import java.util.*

private val TAG = BluetoothLeRepository::class.java.simpleName

class BluetoothLeRepository(private var bleService: BluetoothLeService?) {

    fun startBLEScan() {
        bleService?.startScan()
    }

    fun stopScan() {
        bleService?.stopScan()
    }

    fun connectDevice(serialNumber: String) {
        bleService?.connectDevice(serialNumber)
    }

    fun disconnectDevice() {
        bleService?.disconnectDevice()
    }

    fun getStatusPeriodically() {
        bleService?.getStatusPeriodically()
    }

    fun cancelStatusTask() {
        bleService?.cancelStatusTask()
    }

    fun moveRobot(rotation: Int, movement: Double) {
        bleService?.moveRobot(rotation, movement)
    }

    fun recordBoundary(command: Int, subject: Int) {
        bleService?.let {
            val payload = CommandRecordBoundary(it).getSendPayload(command, subject)
            it.enqueueCommand(payload)
        }
    }

    fun startStop(command: Int) {
        bleService?.let {
            val payload = CommandStartStop(it).getSendPayload(command)
            it.enqueueCommand(payload)
        }
    }

    fun getMapGlobalParameters() {
        bleService?.getMapGlobalParameters()
    }

    fun getGrassBoundary(grassNumber: Byte, packetNumber: Byte) {
        bleService?.let {
            val payload = CommandGrassBoundary(it).getSendPayload(grassNumber, packetNumber)
            it.enqueueCommand(payload)
        }
    }

    fun getObstacleBoundary(grassNumber: Byte, packetNumber: Byte, obstacleNumber: Byte) {
        bleService?.let {
            val payload = CommandObstacleBoundary(it).getSendPayload(grassNumber, packetNumber, obstacleNumber)
            it.enqueueCommand(payload)
        }
    }

    fun getGrassPath(grassNumber: Byte, packetNumber: Byte, targetNumber: Byte, pathNumber: Byte) {
        bleService?.let {
            val payload = CommandGrassPath(it).getSendPayload(grassNumber, packetNumber, targetNumber, pathNumber)
            it.enqueueCommand(payload)
        }
    }

    fun getChargingPath(grassNumber: Byte, packetNumber: Byte, pathNumber: Byte) {
        bleService?.let {
            val payload = CommandChargingPath(it).getSendPayload(grassNumber, packetNumber, pathNumber)
            it.enqueueCommand(payload)
        }
    }

    fun deleteGrass(grassNumber: Byte) {
        bleService?.let {
            val payload = CommandDeleteGrass(it).getSendPayload(grassNumber)
            it.enqueueCommand(payload)
        }
    }

    fun deleteObstacle(grassNumber: Byte, obstacleNumber: Byte) {
        bleService?.let {
            val payload = CommandDeleteObstacle(it).getSendPayload(grassNumber, obstacleNumber)
            it.enqueueCommand(payload)
        }
    }

    fun deleteChargingPath(grassNumber: Byte, pathNumber: Byte) {
        bleService?.let {
            val payload = CommandDeleteChargingPath(it).getSendPayload(grassNumber, pathNumber)
            it.enqueueCommand(payload)
        }
    }

    fun deleteGrassPath(grassNumber: Byte, targetGrassNumber: Byte, pathNumber: Byte) {
        bleService?.let {
            val payload = CommandDeleteGrassPath(it).getSendPayload(grassNumber, targetGrassNumber, pathNumber)
            it.enqueueCommand(payload)
        }
    }

    fun deleteAllMap() {
        bleService?.let {
            val payload = CommandDeleteAll(it).getSendPayload()
            it.enqueueCommand(payload)
        }
    }

    fun getMowingData(packetNumber: Int) {
        bleService?.getMowingData(packetNumber)
    }

    fun cancelGetMowingData() {
        bleService?.cancelGetMowingData()
    }

    fun configSettings(instructionType: Int, value: Byte) {
        bleService?.let {
            val payload = CommandSettings(it).getConfigPayload(instructionType, value)
            it.enqueueCommand(payload)
        }
    }

    fun lookupSettings() {
        bleService?.let {
            val payload = CommandSettings(it).getLookupPayload()
            it.enqueueCommand(payload)
        }
    }

    fun configSchedule(utcOffset: Short, calendarList: ArrayList<Int>, mowerCount: Int) {
        bleService?.let {
            val payload = CommandSchedule(it).getConfigPayload(utcOffset, calendarList, mowerCount)
            Log.i(TAG, "configSchedule ${payload.toHexString()}")
            it.enqueueCommand(payload)
        }
    }

    fun lookupSchedule() {
        bleService?.let {
            val payload = CommandSchedule(it).getLookupPayload()
            it.enqueueCommand(payload)
        }
    }

    fun doVerification() {
        bleService?.doVerification()
    }


}