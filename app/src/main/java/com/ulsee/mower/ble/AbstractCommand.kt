package com.ulsee.mower.ble

import android.content.Intent
import android.util.Log

const val RESPONSE_SUCCESS = 1
const val RESPONSE_FAILED = 0
const val INDEX_SN = 1
const val INDEX_LENGTH = 2

abstract class AbstractCommand(val service: BluetoothLeService) {
    companion object {
        var serialNumber = 1
        set(value) {
            field = if (value == 256) {      //  0xFF + 1 = 256
                0       // 序號一開始從1開始，循環往復則從0開始...
            } else {
                value
            }
        }

        fun resetSerialNumber() {
            serialNumber = 1
        }

        fun getIndex(value: ByteArray, byteNumber: Int): Int {
            var index = -1
            val checksumIndex = value.size - 2
            value.forEachIndexed { idx, byte ->
                val isDesiredIndex = idx != INDEX_SN && idx != INDEX_LENGTH && idx != checksumIndex
                if (byte.toInt() == byteNumber && isDesiredIndex) {
                    return idx
                }
            }
            return index
        }
    }

    abstract fun getSendPayload(): ByteArray
    abstract fun receive(value: ByteArray)

    fun getCheckSum(chars: ByteArray): Int {
        var XOR = 0
        for (element in chars) {
            XOR = XOR xor element.toInt()
        }
        return XOR
    }

    fun sendBroadcast(intent: Intent) {
        service.sendBroadcast(intent)
    }

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    fun getSerialNumber(): Int {
        synchronized(serialNumber) {
            Log.d("666", "[Enter] getSerialNumber() serialNumber: $serialNumber")
            return serialNumber++
        }
    }

//    open fun getIndex(value: ByteArray, byteNumber: Int): Int {
//        var index = -1
//        val checksumIndex = value.size - 2
//        value.forEachIndexed { idx, byte ->
//            val isDesiredIndex = idx != INDEX_SN && idx != INDEX_LENGTH && idx != checksumIndex
//            if (byte.toInt() == byteNumber && isDesiredIndex) {
//                return idx
//            }
//        }
//        return index
//    }

}
