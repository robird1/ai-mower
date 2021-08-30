package com.ulsee.mower.ui.map

import android.content.Intent
import android.graphics.PointF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ulsee.mower.data.MapData
import com.ulsee.mower.utils.Event
import com.ulsee.mower.utils.Utils

abstract class RequestMapAction(val intent: Intent, private val lastItemKey: String) {
    companion object {
        private var _requestMapFinished = MutableLiveData<Event<Boolean>>()
        val requestMapFinished : LiveData<Event<Boolean>>
            get() = _requestMapFinished

        val grassData = HashMap<String, ByteArray>()
        val obstacleData = HashMap<String, ByteArray>()
        val grassPathData = HashMap<String, ByteArray>()
        val chargingPathData = HashMap<String, ByteArray>()

        fun clear() {
            grassData.clear()
            obstacleData.clear()
            grassPathData.clear()
            chargingPathData.clear()
            MapData.clear()
        }
    }

    abstract fun onKeyIndex(): String

    fun execute() {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val packetCount = intent.getByteExtra("packetCount", -1)
        val packetNumber = intent.getByteExtra("packetNumber", -1)
        val data = intent.getByteArrayExtra("data")!!

        val keyIndex = "${onKeyIndex()}"
        val (previousPacketData, mapData) = initValues(data)

        if (packetNumber.toInt() == packetCount-1) {       // 所有數據包已取得
            mapData[keyIndex] = getCoordinateData(previousPacketData)

            if (lastItemKey == keyIndex) {
                _requestMapFinished.value = Event(true)
            }
        }
    }

    private fun getCoordinateData(previousPacketData: HashMap<String, ByteArray>): ArrayList<PointF> {
        val list = ArrayList<PointF>()
        var index = 0
        val borderData = previousPacketData[onKeyIndex()]!!
//        Log.d(TAG, "grassNumber: $grassNumber borderData.size: ${borderData.size}")
        while (index < borderData.size) {
            val xByteArray =
                byteArrayOf(borderData[index++]) + borderData[index++] + borderData[index++] + borderData[index++]
            val yByteArray =
                byteArrayOf(borderData[index++]) + borderData[index++] + borderData[index++] + borderData[index++]
            val x = Utils.convert(xByteArray).toInt()
            val y = Utils.convert(yByteArray).toInt()
            list.add(PointF(x.toFloat(), y.toFloat()))

            index++
//            Log.d("654","xByteArray: \n${xByteArray.toHexString()}")
//            Log.d("654","yByteArray: \n${yByteArray.toHexString()}")
//            Log.d("654","x: \n${x}")
//            Log.d("654","y: \n${y}")
        }
        return list
    }

    private fun initValues(data: ByteArray): Pair<HashMap<String, ByteArray>, HashMap<String, ArrayList<PointF>>> {
        val tempData: HashMap<String, ByteArray>
        val mapData: HashMap<String, ArrayList<PointF>>
        val keyIndex = onKeyIndex()
        when (this) {
            is ActionGrassBoarder -> {
                tempData = grassData
                mapData = MapData.grassData
            }
            is ActionObstacleBoarder -> {
                tempData = obstacleData
                mapData = MapData.obstacleData
            }
            is ActionGrassPath -> {
                tempData = grassPathData
                mapData = MapData.grassPathData
            }
            is ActionChargingPath -> {
                tempData = chargingPathData
                mapData = MapData.chargingPathData
            }
            else -> {
                tempData = HashMap()
                mapData = HashMap()
            }
        }
        if (tempData[keyIndex] == null) {
            tempData[keyIndex] = ByteArray(0)
        }
        tempData[keyIndex] = tempData[keyIndex]!!.plus(data)

        return Pair(tempData, mapData)
    }
}


class ActionGrassBoarder(intent: Intent, lastItemKey: String): RequestMapAction(intent, lastItemKey) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        return "grass.$grassNumber"
    }
}


class ActionObstacleBoarder(intent: Intent, lastItemKey: String): RequestMapAction(intent, lastItemKey) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val obstacleNumber = intent.getByteExtra("obstacleNumber", -1)
        return "obstacle.$grassNumber.$obstacleNumber"
    }
}


class ActionGrassPath(intent: Intent, lastItemKey: String): RequestMapAction(intent, lastItemKey) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val targetGrassNumber = intent.getByteExtra("targetGrassNumber", -1)
        val pathNumber = intent.getByteExtra("pathNumber", -1)
        return "route.$grassNumber.$targetGrassNumber.$pathNumber"
    }
}


class ActionChargingPath(intent: Intent, lastItemKey: String): RequestMapAction(intent, lastItemKey) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val pathNumber = intent.getByteExtra("pathNumber", -1)
        return "charging.$grassNumber.$pathNumber"
    }
}

fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
