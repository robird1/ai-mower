package com.ulsee.mower.data

private val TAG = BluetoothLeRepository::class.java.simpleName

//class BluetoothLeRepository(private val bleService: BluetoothLeService) {
class BluetoothLeRepository {
    private var bleService: BluetoothLeService? = null

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


}