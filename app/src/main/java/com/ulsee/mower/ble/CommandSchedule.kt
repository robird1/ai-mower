package com.ulsee.mower.ble

class CommandSchedule(service: BluetoothLeService): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        TODO("Not yet implemented")
    }
}