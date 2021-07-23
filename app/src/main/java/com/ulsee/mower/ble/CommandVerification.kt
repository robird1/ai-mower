package com.ulsee.mower.ble

import com.ulsee.mower.data.BLEBroadcastAction

class CommandVerification(service: BluetoothLeService, private val serialNumberInput: String): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        val checksumArray = getCheckSumArray()
//        Log.d(TAG, "checksumArray: ${checksumArray.toHexString()}")
        val checkSum = getCheckSum(checksumArray)
        return checksumArray + checkSum.toByte() + 0xFF.toByte()
    }

    override fun receive(value: ByteArray) {
        service.broadcastUpdate(BLEBroadcastAction.ACTION_VERIFICATION_SUCCESS, value.toHexString())
//        handler.removeCallbacks(verificationTask!!)
    }

    private fun getCheckSumArray(): ByteArray {
        val snInAscii = snToAscii()
        val instrucLen = (snInAscii.size + 4).toByte()
        return byteArrayOf(0xFA.toByte()) + 0x00 + instrucLen + 0x10 + 0x11 + snInAscii + 0x12 + 0x00
    }

    private fun snToAscii(): ByteArray {
        val array = ByteArray(serialNumberInput!!.length)
        val snCharArray = serialNumberInput!!.toCharArray()
        for (i in serialNumberInput!!.indices) {
            val ascii = snCharArray[i].toInt().toByte()
            array[i] = ascii
//            Log.d(TAG, "ascii: $ascii")
        }
        return array
    }

}