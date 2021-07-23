package com.ulsee.mower.ble

import android.content.Intent
import android.util.Log
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_BORDER_RECORD
import com.ulsee.mower.data.RecordBoundary.Command.Companion.DISCARD_BOUNDARY

//private val TAG = CommandRecordBoundary::class.java.simpleName

class CommandRecordBoundary(service: BluetoothLeService): AbstractCommand(service) {

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(ACTION_BORDER_RECORD)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        intent.putExtra("subject", value[9].toInt())

        val subject = value[value.indexOf(0x73)+1].toInt()
        when (subject) {
            0x00 -> {
                intent.putExtra("grass_number", value[value.indexOf(0x74)+1].toInt())
            }
            0x01 -> {
                intent.putExtra("grass_number", value[value.indexOf(0x74)+1].toInt())
                intent.putExtra("obstacle_number", value[value.indexOf(0x75)+1].toInt())
            }
            0x02 -> {
                intent.putExtra("grass_number", value[value.indexOf(0x74)+1].toInt())
                intent.putExtra("path_number", value[value.indexOf(0x77)+1].toInt())
            }
            0x03 -> {
                intent.putExtra("grass_number", value[value.indexOf(0x74)+1].toInt())
                intent.putExtra("target_grass_number", value[value.indexOf(0x76)+1].toInt())
                intent.putExtra("path_number", value[value.indexOf(0x77)+1].toInt())
            }
            else -> {
                // do nothing
            }
        }

        if (value[7].toInt() == DISCARD_BOUNDARY) {
            Log.d("456", "[DISCARD_BOUNDARY] value: ${value.toHexString()}")
        }
        sendBroadcast(intent)
    }

    fun getSendPayload(command: Int, subject: Int): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x05 + 0x70 + 0x71 + command.toByte() + 0x72 + subject.toByte()
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }
}