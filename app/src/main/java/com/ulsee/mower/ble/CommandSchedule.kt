package com.ulsee.mower.ble

import android.content.Intent
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Utils

class CommandSchedule(service: BluetoothLeService): AbstractCommand(service) {
    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_SCHEDULING)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("operation_mode", getOperationMode(value))
        intent.putExtra("utc", getResponseUTC(value))
        intent.putExtra("calendar", getCalendar(value))
        sendBroadcast(intent)
    }

    fun getConfigPayload(utcOffset: Short, calendarList: ArrayList<Int>): ByteArray {
        var checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x4F + 0xF0.toByte() + 0xF1.toByte() + 0x01 + 0xF2.toByte() + 0x01 + 0xF3.toByte() + getUtcByteArray(utcOffset) + 0xF4.toByte() + getCalendarByteArray(calendarList)
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    fun getLookupPayload(): ByteArray {
        var checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x03 + 0xF0.toByte() + 0xF1.toByte() + 0x00
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    private fun getCalendar(value: ByteArray): ArrayList<Byte> {
        val indices = IntRange(14, 83)
        val byteArray = value.sliceArray(indices)
        return byteArray.toList() as ArrayList<Byte>
    }

    private fun getResponseUTC(value: ByteArray): Short {
        val idx = getIndex(value, 0xF4)
        val byteArray = byteArrayOf(value[idx + 1]) + value[idx + 2]
        return Utils.convert(byteArray).toShort()
    }

    /**
     * 讀取或配置
     */
    private fun getOperationMode(value: ByteArray) = value[getIndex(value, 0xF2) + 1].toInt()

    private fun getUtcByteArray(utcOffset: Short): ByteArray {
        return Utils.intToBytes(utcOffset)
    }

    private fun getCalendarByteArray(calendarList: ArrayList<Int>): ByteArray {
        return ByteArray(calendarList.size) { pos -> calendarList[pos].toByte() }
    }

}