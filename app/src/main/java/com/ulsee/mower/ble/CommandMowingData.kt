package com.ulsee.mower.ble

import android.content.Intent
import android.graphics.PointF
import android.util.Log
import com.google.gson.Gson
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Utils

class CommandMowingData(service: BluetoothLeService): AbstractCommand(service) {

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        Log.d("888", "[Enter] CommandMowingData.receive() value: ${value.toHexString()}")

        val packetCountArray = byteArrayOf(value[7]) + value[8]
        val packetCount = Utils.convert(packetCountArray).toInt()
        val packetNumberArray = byteArrayOf(value[10]) + value[11]
        val packetNumber = Utils.convert(packetNumberArray).toInt()
        var index = value.indexOf(0xB4.toByte()) + 1
        var hasDataGrassCount = 0
        var dataLen = 0
        if (packetNumber == 0) {
            hasDataGrassCount = value[index++].toInt()
            val lenArray = byteArrayOf(value[index++]) + value[index++]
            dataLen = Utils.convert(lenArray).toInt()
        } else {

        }

//        if (hasDataGrassCount > 0) {
//            val grassNumber = value[index++]

            Log.d("888", "packetCount: $packetCount packetNumber: $packetNumber dataLen: $dataLen")

            val arrayList = ArrayList<PointF>()
//            while (index < index + dataLen * 8) {
        while (index < value.size - 2) {
            val xByteArray = byteArrayOf(value[index++]) + value[index++] + value[index++] + value[index++]
                val yByteArray = byteArrayOf(value[index++]) + value[index++] + value[index++] + value[index++]
                val x = Utils.convert(xByteArray).toInt()
                val y = Utils.convert(yByteArray).toInt()
                arrayList.add(PointF(x.toFloat(), y.toFloat()))
            }

            val intent = Intent(BLEBroadcastAction.ACTION_MOWING_DATA)
            intent.putExtra("packetCount", packetCount)
            intent.putExtra("packetNumber", packetNumber)
            intent.putExtra("data", Gson().toJson(arrayList))
            sendBroadcast(intent)
//        }

    }

    fun getSendPayload(packetNumber: Int): ByteArray {
        val byteArray = Utils.intToBytes(packetNumber.toShort())
        Log.d("888", "[Enter] getSendPayload() packetNumber byteArray: ${byteArray.toHexString()}")
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x04 + 0xB0.toByte() + 0xB1.toByte() + byteArray
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}