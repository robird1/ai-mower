package com.ulsee.mower.ble

import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.ulsee.mower.data.BLEBroadcastAction

private val TAG = CommandGlobalParameter::class.java.simpleName

class CommandGlobalParameter(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val GLOBAL_PARAMETER = 0x00
    }

    data class  GlobalParameter(val grassNumber: Byte, val obstacleCount: Byte, val chargingPathCount: Byte,
                               val targetGrassCount: Byte, val targetGrass: ArrayList<TargetGrass>)

    data class TargetGrass(val targetGrassNumber: Byte, val targetPathCount: Byte)


    override fun getSendPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x05 + 0x60 + 0x61 + 0x00 + 0x62 + 0x00
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    override fun receive(value: ByteArray) {
        val arrayList = ArrayList<GlobalParameter>()
        val grassCountIndex = getIndex(value, 0x6D) + 1
        val grassCount = value[grassCountIndex]
        var index = grassCountIndex + 1
        for (i in 0 until grassCount) {
            val grassNumber = value[index++]
            val obstacleCount = value[index++]
            val chargingPathCount = value[index++]
            index++
            val targetGrassCount = value[index++]
            var targetGrassNumber: Byte
            var targetPathCount: Byte

            val targetGrassList = ArrayList<TargetGrass>()
//            if (targetGrassCount > 0) {
                for (i in 0 until targetGrassCount) {
                    targetGrassNumber = value[index++]
                    targetPathCount = value[index++]
                    targetGrassList.add(TargetGrass(targetGrassNumber, targetPathCount))
                }
//            }
            val data = GlobalParameter(grassNumber, obstacleCount, chargingPathCount, targetGrassCount, targetGrassList)
            arrayList.add(data)
        }
        val intent = Intent(BLEBroadcastAction.ACTION_GLOBAL_PARAMETER)
        intent.putExtra("data", Gson().toJson(arrayList))
        sendBroadcast(intent)
    }

}


class CommandGrassBoundary(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val GRASS_BOUNDARY = 0x01
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        Log.d("654", "value: ${value.toHexString()}")
        val serialNumber = value[1]
        val packetCount = value[7]
        val packetNumber = value[9]
        val grassNumber = value[13]

        val intent = Intent(BLEBroadcastAction.ACTION_GRASS_BOARDER)
        intent.putExtra("serialNumber", serialNumber)
        intent.putExtra("grassNumber", grassNumber)
        intent.putExtra("packetCount", packetCount)
        intent.putExtra("packetNumber", packetNumber)

        val idx = getIndex(value, 0x6D)
        val dataIndexRange = if (packetNumber.toInt() == 0) {
            IntRange(idx + 3, value.size - 3)
        } else {
            IntRange(idx + 1, value.size - 3)
        }

        Log.d("654", "value.sliceArray: ${value.sliceArray(dataIndexRange).toHexString()}")
        intent.putExtra("data", value.sliceArray(dataIndexRange))
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, packetNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x07 + 0x60 + 0x61 + 0x01 + 0x62 + packetNumber + 0x63 + grassNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandObstacleBoundary(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val OBSTACLE_BOUNDARY = 0x02
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val serialNumber = value[1]
        val packetCount = value[7]
        val packetNumber = value[9]
        val grassNumber = value[13]
        val obstacleNumber = value[getIndex(value, 0x68)+1]

        val intent = Intent(BLEBroadcastAction.ACTION_OBSTACLE_BOARDER)
        intent.putExtra("serialNumber", serialNumber)
        intent.putExtra("grassNumber", grassNumber)
        intent.putExtra("obstacleNumber", obstacleNumber)
        intent.putExtra("packetCount", packetCount)
        intent.putExtra("packetNumber", packetNumber)

        val idx = getIndex(value, 0x6D)
        val dataIndexRange = if (packetNumber.toInt() == 0) {
            IntRange(idx + 3, value.size - 3)
        } else {
            IntRange(idx + 1, value.size - 3)
        }
        intent.putExtra("data", value.sliceArray(dataIndexRange))
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, packetNumber: Byte, obstacleNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x09 + 0x60 + 0x61 + 0x02 + 0x62 + packetNumber + 0x63 + grassNumber + 0x64 + obstacleNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandChargingPath(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val CHARGING_PATH = 0x03
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val serialNumber = value[1]
        val packetCount = value[7]
        val packetNumber = value[9]
        val grassNumber = value[13]
        val pathNumber = value[getIndex(value, 0x6B)+1]

        val intent = Intent(BLEBroadcastAction.ACTION_CHARGING_PATH)
        intent.putExtra("serialNumber", serialNumber)
        intent.putExtra("grassNumber", grassNumber)
        intent.putExtra("packetCount", packetCount)
        intent.putExtra("packetNumber", packetNumber)
        intent.putExtra("pathNumber", pathNumber)

        val idx = getIndex(value, 0x6D)
        val dataIndexRange = if (packetNumber.toInt() == 0) {
            IntRange(idx + 3, value.size - 3)
        } else {
            IntRange(idx + 1, value.size - 3)
        }
        intent.putExtra("data", value.sliceArray(dataIndexRange))
        sendBroadcast(intent)

    }

    fun getSendPayload(grassNumber: Byte, packetNumber: Byte, pathNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x09 + 0x60 + 0x61 + 0x03 + 0x62 + packetNumber + 0x63 + grassNumber + 0x66 + pathNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandGrassPath(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val GRASS_PATH = 0x04
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val serialNumber = value[1]
        val packetCount = value[7]
        val packetNumber = value[9]
        val grassNumber = value[13]
        val targetGrassNumber = value[getIndex(value, 0x6A)+1]
        val pathNumber = value[value.indexOf(0x6B)+1]

        val intent = Intent(BLEBroadcastAction.ACTION_GRASS_PATH)
        intent.putExtra("serialNumber", serialNumber)
        intent.putExtra("grassNumber", grassNumber)
        intent.putExtra("targetGrassNumber", targetGrassNumber)
        intent.putExtra("pathNumber", pathNumber)
        intent.putExtra("packetCount", packetCount)
        intent.putExtra("packetNumber", packetNumber)

        val idx = getIndex(value, 0x6D)
        val dataIndexRange = if (packetNumber.toInt() == 0) {
            IntRange(idx + 3, value.size - 3)
        } else {
            IntRange(idx + 1, value.size - 3)
        }
        intent.putExtra("data", value.sliceArray(dataIndexRange))
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, packetNumber: Byte, targetNumber: Byte, pathNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x0B + 0x60 + 0x61 + 0x04 + 0x62 + packetNumber + 0x63 + grassNumber + 0x65 + targetNumber + 0x66 + pathNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}

