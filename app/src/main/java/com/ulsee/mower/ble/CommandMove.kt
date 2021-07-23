package com.ulsee.mower.ble

class CommandMove(service: BluetoothLeService, private val rotation: Int, movement: Double): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + 0x00 + 0x06 + 0x20 + 0x21 + integerToTwoBytes(rotation) + 0x22 + 0x32
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    override fun receive(value: ByteArray) {
        TODO("Not yet implemented")
    }

    private fun integerToTwoBytes(value: Int): ByteArray {
        val result = ByteArray(2)
//        if (value > Math.pow(2.0, 31.0) || value < 0) {
//            throw UtilityException("Integer value $value is larger than 2^31")
//        }
        result[1] = (value ushr 8 and 0xFF).toByte()
        result[0] = (value and 0xFF).toByte()
        return result
    }

}