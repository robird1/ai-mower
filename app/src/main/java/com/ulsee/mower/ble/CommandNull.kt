package com.ulsee.mower.ble

class CommandNull(service: BluetoothLeService): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        // do nothing
    }
}