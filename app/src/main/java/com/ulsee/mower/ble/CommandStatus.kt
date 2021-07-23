package com.ulsee.mower.ble

import android.content.Intent
import android.util.Log
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Utils

class CommandStatus(service: BluetoothLeService): AbstractCommand(service) {

    override fun getSendPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + 0x00 + 0x02 + 0x50 + 0x00
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    override fun receive(value: ByteArray) {
        val xByteArray = byteArrayOf(value[5]) + value[6] + value[7] + value[8]
        val yByteArray = byteArrayOf(value[9]) + value[10] + value[11] + value[12]
        val angleByteArray = byteArrayOf(value[14]) + value[15]
        val x = Utils.convert(xByteArray).toInt()
        val y = Utils.convert(yByteArray).toInt()
        val angle = Utils.convert(angleByteArray).toShort() / 10f
        val power = value[getIndex(value, 0x53) + 1].toInt()
        val signalQuality = value[getIndex(value,0x55) + 1].toInt()

        val intent = Intent(BLEBroadcastAction.ACTION_STATUS)
        intent.putExtra("x", x)
        intent.putExtra("y", y)
        intent.putExtra("angle", angle)
        intent.putExtra("power", power)
        intent.putExtra("signal_quality", signalQuality)
        intent.putExtra("working_mode", getWorkingMode(value))
        intent.putExtra("working_error_code", getWorkingErrorCode(value))
        intent.putExtra("robot_status", getRobotStatus(value))
//        intent.putExtra("robot_position_status", getRobotPositionStatus(value))
        intent.putExtra("interruption_code", getInterruptionCode(value))

        val testBoundaryIndex = getIndex(value, 0x61)
        if (testBoundaryIndex != -1) {
            intent.putExtra("testing_boundary", value[testBoundaryIndex+ 3].toInt())
        }

        sendBroadcast(intent)
    }

    private fun getInterruptionCode(value: ByteArray): String {
        var code = ""
        val idx = getIndex(value,  0x64)

        for (i in 1 until 5) {                // 4 bytes
            val intValue = value[idx + i].toInt()
            var temp = Integer.toBinaryString(intValue)
            while (temp.length < 8) {
                temp = "0$temp"
            }
            code += temp
        }
        Log.d("888", "[Enter] getInterruptionCode() $code")
        return code

    }

    private fun getRobotStatus(value: ByteArray): String {
        var status = ""
        val idx = getIndex(value,  0x54)

        for (i in 1 until 3) {                // only the first 2 bytes indicate the abnormal status
            val intValue = value[idx + i].toInt()
            var temp = Integer.toBinaryString(intValue)
            while (temp.length < 8) {
                temp = "0$temp"
            }
            status += temp
        }
        Log.d("888", "[Enter] getRobotStatus() $status")
        return status
    }

    private fun getWorkingErrorCode(value: ByteArray): Int {
        val idx = getIndex(value,  0x5C)
        return value[idx + 1].toInt()
    }

    private fun getWorkingMode(value: ByteArray): Int {
        val idx = getIndex(value,  0x57)
        return value[idx + 1].toInt()
    }

    override fun getIndex(value: ByteArray, byteNumber: Int): Int {
        var index = -1
        val checksumIndex = value.size - 2
        val lastCoordinateIdx = 12
        value.forEachIndexed { idx, byte ->
            val isDesiredIndex = idx != INDEX_SN && idx != INDEX_LENGTH && idx != checksumIndex
                    && idx > lastCoordinateIdx
            if (byte.toInt() == byteNumber && isDesiredIndex) {
                index = idx
            }
        }
        return index
    }

    //    private fun getRobotPositionStatus(value: ByteArray): String {
//        Log.d("888", "[Enter] getRobotPositionStatus()")
//        var status = ""
//        val idx = getIndex(value,  0x54)
//
//        for (i in 3 until 5) {                // get the last 2 bytes
//            val intValue = value[idx + i].toInt()
////            Log.d("888", "intValue: $intValue")
//            var temp = Integer.toBinaryString(intValue)
////            Log.d("888", "Integer.toBinaryString(intValue): ${temp}")
//            while (temp.length < 8) {
//                temp = "0$temp"
//            }
////            Log.d("888", "after add 0: ${temp}")
//            status += temp
//        }
//        for (i in 0 until 16) {
//            status = "0$status"
//        }
//        Log.d("888", "final status string: ${status}")
//        return status
//    }

}