package com.ulsee.mower.ble

import android.content.Intent
import com.ulsee.mower.data.BLEBroadcastAction

class CommandSettings(service: BluetoothLeService): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_SETTINGS)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("operation_mode", value[getIndex(value, 0x82)+1].toInt())
        intent.putExtra("working_mode", value[getIndex(value, 0x83)+1].toInt())
        intent.putExtra("rain_mode", value[getIndex(value, 0x84)+1].toInt())
        intent.putExtra("knife_height", getKnifeHeight(value))
        sendBroadcast(intent)
    }

    private fun getKnifeHeight(value: ByteArray): Int {
        val grassCount = value[getIndex(value, 0x86) + 1].toInt()
        val knifeHeightIdx = 38 + grassCount * 9
        return value[knifeHeightIdx].toInt()
    }

    fun getConfigPayload(instructionType: Int, value: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x05 + 0x80.toByte() + 0x81.toByte() + 0x00 + instructionType.toByte() + value
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    fun getLookupPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x03 + 0x80.toByte() + 0x81.toByte() + 0x01
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}