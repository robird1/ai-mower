package com.ulsee.mower.ble

import android.content.Intent
import android.util.Log
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

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
        intent.putExtra("mowing_status", getMowingStatus(value))
        intent.putExtra("charging_status", getChargingStatus(value))
        intent.putExtra("interruption_code", getInterruptionCode(value))
        intent.putExtra("total_area", getTotalWorkingArea(value))
        intent.putExtra("finished_area", getFinishedArea(value))
        intent.putExtra("estimated_time", getEstimatedWorkingTime(value))
        intent.putExtra("elapsed_time", getWorkingElapsedTime(value))

        val testBoundaryIndex = getIndex(value, 0x61)
        if (testBoundaryIndex != -1) {
            intent.putExtra("testing_boundary", value[testBoundaryIndex+ 3].toInt())
        }

        sendBroadcast(intent)
    }

    private fun getWorkingElapsedTime(value: ByteArray): String {
        val idx = getIndex(value, 0x58)
        val byteArray = byteArrayOf(value[idx + 1]) + value[idx + 2]
        val elapsedTime = Utils.convert(byteArray).toShort()
//        Log.d("666", "elapsedTime: $elapsedTime")
        val hours = elapsedTime / 60
        val minutes = elapsedTime % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    private fun getEstimatedWorkingTime(value: ByteArray): String {
        val idx = getIndex(value, 0x65)
        val byteArray = byteArrayOf(value[idx + 1]) + value[idx + 2]
        val totalMinutes = Utils.convert(byteArray).toShort()
//        Log.d("666", "totalMinutes: $totalMinutes")
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours, minutes)
    }

    private fun getFinishedArea(value: ByteArray): Short {
        val idx = getIndex(value, 0x59)
        val byteArray = byteArrayOf(value[idx + 1]) + value[idx + 2]
        return Utils.convert(byteArray).toShort()
    }

    private fun getTotalWorkingArea(value: ByteArray): Short {
        val idx = getIndex(value, 0x5A)
        val byteArray = byteArrayOf(value[idx + 1]) + value[idx + 2]
        return Utils.convert(byteArray).toShort()
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
//        Log.d("888", "[Enter] getInterruptionCode() $code")
        return code

    }

    /**
     * 充電狀態
     */
    private fun getChargingStatus(value: ByteArray): Boolean {
        var status = false
        val idx = getIndex(value,  0x54)
        val intValue = value[idx + 3].toInt()
        var temp = Integer.toBinaryString(intValue)
        while (temp.length < 8) {
            temp = "0$temp"
        }

//        Log.d("666", "robotStatus: $temp")
        temp.forEachIndexed { idx, value ->
//            Log.d("666", "idx: $idx value: $value")
            if (idx == 6 && value == '1') {
                status = true
            }
        }
//        Log.d("666", "[Enter] getMowingStatus() $status")
        return status
    }

    /**
     * 刀盤開啟狀態
     */
    private fun getMowingStatus(value: ByteArray): Boolean {
        var status = false
        val idx = getIndex(value,  0x54)
        val intValue = value[idx + 3].toInt()
        var temp = Integer.toBinaryString(intValue)
        while (temp.length < 8) {
            temp = "0$temp"
        }

//        Log.d("666", "robotStatus: $temp")
        temp.forEachIndexed { idx, value ->
//            Log.d("666", "idx: $idx value: $value")
            if (idx == 5 && value == '1') {
                status = true
            }
        }
//        Log.d("666", "[Enter] getMowingStatus() $status")
        return status

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
//        Log.d("888", "[Enter] getRobotStatus() $status")
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
                    && idx > lastCoordinateIdx && idx != 14 && idx != 15 && idx != 26 && idx != 27
                    && idx != 31 && idx != 32 && idx != 34 && idx != 35 && idx != 37 && idx != 38
                    && idx != 40 && idx != 41 && idx != 45 && idx != 46
            if (byte.toInt() == byteNumber && isDesiredIndex) {
                index = idx
            }
        }
        return index
    }

}