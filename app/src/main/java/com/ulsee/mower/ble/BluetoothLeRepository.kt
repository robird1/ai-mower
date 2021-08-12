package com.ulsee.mower.ble

import java.util.*

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
        bleService?.recordBoundary(command, subject)
    }

    fun startStop(command: Int) {
        bleService?.startStop(command)
    }

    fun getMapGlobalParameters() {
        bleService?.getMapGlobalParameters()
    }

    fun getGrassBoundary(grassNumber: Byte, packetNumber: Byte) {
        bleService?.getGrassBoundary(grassNumber, packetNumber)
    }

    fun getObstacleBoundary(grassNumber: Byte, packetNumber: Byte, obstacleNumber: Byte) {
        bleService?.getObstacleBoundary(grassNumber, packetNumber, obstacleNumber)
    }

    fun getGrassPath(grassNumber: Byte, packetNumber: Byte, targetNumber: Byte, pathNumber: Byte) {
        bleService?.getGrassPath(grassNumber, packetNumber, targetNumber, pathNumber)
    }

    fun getChargingPath(grassNumber: Byte, packetNumber: Byte, pathNumber: Byte) {
        bleService?.getChargingPath(grassNumber, packetNumber, pathNumber)
    }

    fun deleteGrass(grassNumber: Byte) {
        bleService?.deleteGrass(grassNumber)
    }

    fun deleteObstacle(grassNumber: Byte, obstacleNumber: Byte) {
        bleService?.deleteObstacle(grassNumber, obstacleNumber)
    }

    fun deleteChargingPath(grassNumber: Byte, pathNumber: Byte) {
        bleService?.deleteChargingPath(grassNumber, pathNumber)
    }

    fun deleteGrassPath(grassNumber: Byte, targetGrassNumber: Byte, pathNumber: Byte) {
        bleService?.deleteGrassPath(grassNumber, targetGrassNumber, pathNumber)
    }

    fun deleteAllMap() {
        bleService?.deleteAllMap()
    }

    fun getMowingData(packetNumber: Int) {
        bleService?.getMowingData(packetNumber)
    }

    fun cancelGetMowingData() {
        bleService?.cancelGetMowingData()
    }

    fun configSettings(instructionType: Int, value: Byte) {
        bleService?.configSettings(instructionType, value)
    }

    fun lookupSettings() {
        bleService?.lookupSettings()
    }

    fun configSchedule(utcOffset: Short, calendarList: ArrayList<Int>, mowerCount: Int) {
        bleService?.configSchedule(utcOffset, calendarList, mowerCount)
    }

    fun lookupSchedule() {
        bleService?.lookupSchedule()
    }

    fun doVerification() {
        bleService?.doVerification()
    }


}