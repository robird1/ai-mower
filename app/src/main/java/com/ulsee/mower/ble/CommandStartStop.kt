package com.ulsee.mower.ble

import android.content.Intent
import com.ulsee.mower.data.BLEBroadcastAction

class CommandStartStop(service: BluetoothLeService): AbstractCommand(service) {

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_START_STOP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

    fun getSendPayload(command: Int): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x04 + 0x40 + command.toByte() + 0x41 + 0x01
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}