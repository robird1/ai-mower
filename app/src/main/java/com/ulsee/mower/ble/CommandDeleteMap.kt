package com.ulsee.mower.ble

import android.content.Intent
import com.ulsee.mower.data.BLEBroadcastAction


class CommandDeleteGrass(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val DELETE_GRASS = 0x00
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x05 + 0x90.toByte() + 0x91.toByte() + 0x00 + 0x92.toByte() + grassNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandDeleteObstacle(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val DELETE_OBSTACLE = 0x01
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, obstacleNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x07 + 0x90.toByte() + 0x91.toByte() + 0x01 + 0x92.toByte() + grassNumber + 0x93.toByte() + obstacleNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandDeleteChargingPath(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val DELETE_CHARGING_PATH = 0x02
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, pathNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x07 + 0x90.toByte() + 0x91.toByte() + 0x02 + 0x92.toByte() + grassNumber + 0x95.toByte() + pathNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandDeleteGrassPath(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val DELETE_GRASS_PATH = 0x03
    }

    override fun getSendPayload(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

    fun getSendPayload(grassNumber: Byte, targetGrassNumber: Byte, pathNumber: Byte): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x09 + 0x90.toByte() + 0x91.toByte() + 0x03 + 0x92.toByte() + grassNumber + 0x94.toByte() + targetGrassNumber + 0x95.toByte() + pathNumber
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

}


class CommandDeleteAll(service: BluetoothLeService): AbstractCommand(service) {
    companion object {
        const val DELETE_ALL = 0x06
    }

    override fun getSendPayload(): ByteArray {
        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x03 + 0x90.toByte() + 0x91.toByte() + 0x06
        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
    }

    override fun receive(value: ByteArray) {
        val intent = Intent(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)
        intent.putExtra("result", value[5].toInt())
        intent.putExtra("command", value[7].toInt())
        sendBroadcast(intent)
    }

//    fun getSendPayload(): ByteArray {
//        val checksumArray = byteArrayOf(0xFA.toByte()) + getSerialNumber().toByte() + 0x03 + 0x90.toByte() + 0x91.toByte() + 0x06
//        return checksumArray + getCheckSum(checksumArray).toByte() + 0xFF.toByte()
//    }

}