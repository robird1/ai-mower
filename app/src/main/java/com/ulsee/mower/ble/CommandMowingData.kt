package com.ulsee.mower.ble

import android.content.Intent
import android.graphics.PointF
import android.util.Log
import com.google.gson.Gson
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Utils

class CommandMowingData(service: BluetoothLeService): AbstractCommand(service) {

    companion object {
        fun convertBytesToCoordinate(list: ArrayList<Byte>): ArrayList<PointF> {
            val coordinateList = ArrayList<PointF>()
            try {
                var index = 0
                while (index < list.size) {
                    val xByteArray = byteArrayOf(list[index++]) + list[index++] + list[index++] + list[index++]
                    val yByteArray = byteArrayOf(list[index++]) + list[index++] + list[index++] + list[index++]
                    val x = Utils.convert(xByteArray).toInt()
                    val y = Utils.convert(yByteArray).toInt()
//                    Log.d("666", "xByteArray: ${xByteArray.toHexString()}")
//                    Log.d("666", "yByteArray: ${yByteArray.toHexString()}")
                    coordinateList.add(PointF(x.toFloat(), y.toFloat()))
                }
            } catch (e: Exception) {
                coordinateList.clear()
                Log.d("666", "[Exception] convertBytesToCoordinate() =================================")
            }
            return coordinateList
        }
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        Log.d("666", "[Enter] CommandMowingData.receive() value: ${value.toHexString()}")
        val packetCountArray = byteArrayOf(value[7]) + value[8]
        val packetCount = Utils.convert(packetCountArray).toInt()
        val packetNumberArray = byteArrayOf(value[10]) + value[11]
        val packetNumber = Utils.convert(packetNumberArray).toInt()

        // 0xB4.toByte() -> -76
        var index = getIndex(value, -76) + 1

        if (packetNumber == 0) {
            val finishGrassCount = value[index++].toInt()
//            Log.d("666", "finishGrassCount: $finishGrassCount")
            var finishGrassNumber = -1
            for (i in 0 until finishGrassCount) {
                finishGrassNumber = value[index++].toInt()
//                Log.d("666", "finishGrassNumber: $finishGrassNumber")
            }
            var dataLen = value[index++]
            dataLen = value[index++]
        }

        val arrayList = ArrayList<Byte>()
        while (index < value.size - 2) {
            arrayList.add(value[index++])
        }

        if (arrayList.size > 0) {
            val intent = Intent(BLEBroadcastAction.ACTION_MOWING_DATA)
            intent.putExtra("packetCount", packetCount)
            intent.putExtra("packetNumber", packetNumber)
            intent.putExtra("data", Gson().toJson(arrayList))
            sendBroadcast(intent)
        }

    }

    fun getSendPayload(packetNumber: Int): ByteArray {
        val byteArray = Utils.intToBytes(packetNumber.toShort())
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x04 + 0xB0.toByte() + 0xB1.toByte() + byteArray
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}