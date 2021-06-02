package com.ulsee.mower.data

private val TAG = BluetoothLeRepository::class.java.simpleName

class BluetoothLeRepository(private var bleService: BluetoothLeService?) {
//class BluetoothLeRepository {
//    private var bleService: BluetoothLeService? = null

    fun setBleService(service: BluetoothLeService) {
        bleService = service
    }

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

    fun getStatus() {
        bleService?.getStatus()
    }

    fun moveRobot(rotation: Int, movement: Double) {
        bleService?.moveRobot(rotation, movement)
    }


}