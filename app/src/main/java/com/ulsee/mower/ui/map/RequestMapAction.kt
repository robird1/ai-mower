package com.ulsee.mower.ui.map

import android.content.Intent
import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ulsee.mower.data.MapData
import com.ulsee.mower.utils.Utils

//abstract class RequestMapAction(val intent: Intent, val viewModel: StatusFragmentViewModel) {
abstract class RequestMapAction(val intent: Intent, val lastItemKey: String, val requestMapFinished: MutableLiveData<Boolean>) {
    open fun execute() {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val packetCount = intent.getByteExtra("packetCount", -1)
        val packetNumber = intent.getByteExtra("packetNumber", -1)
        val data = intent.getByteArrayExtra("data")!!

        val keyIndex = "${onKeyIndex()}"
        val viewModelData = onViewModelData()
//        val liveData = onViewModelLiveData()

        if (viewModelData[keyIndex] == null) {
            viewModelData[keyIndex] = ByteArray(0)
        }
        viewModelData[keyIndex] = viewModelData[keyIndex]!!.plus(data)

        if (packetNumber.toInt() == packetCount-1) {       // 所有數據包已取得
            val list = ArrayList<PointF>()
            var index = 0
            val borderData = viewModelData[keyIndex]!!
//            Log.d(TAG, "grassNumber: $grassNumber borderData.size: ${borderData.size}")
            while (index < borderData.size) {
                val xByteArray = byteArrayOf(borderData[index++]) + borderData[index++] + borderData[index++] + borderData[index++]
                val yByteArray = byteArrayOf(borderData[index++]) + borderData[index++] + borderData[index++] + borderData[index++]
                val x = Utils.convert(xByteArray).toInt()
                val y = Utils.convert(yByteArray).toInt()
                list.add(PointF(x.toFloat(), y.toFloat()))

                index++
//                Log.d("654","xByteArray: \n${xByteArray.toHexString()}")
//                Log.d("654","yByteArray: \n${yByteArray.toHexString()}")
//                Log.d("654","x: \n${x}")
//                Log.d("654","y: \n${y}")
            }

//            binding.statusView.drawGrass(elementId, list)
//            liveData.value = Pair(keyIndex, list)

            val mapData = onMapData()
            mapData[keyIndex] = list

            Log.d("999", "keyIndex: $keyIndex")
            Log.d("999", "lastItemKey: $lastItemKey")
            if (lastItemKey == keyIndex) {
                requestMapFinished.value = true
            }
        }
    }

    abstract fun onViewModelData(): HashMap<String, ByteArray>

    abstract fun onKeyIndex(): String

//    abstract fun onViewModelLiveData(): MutableLiveData<Pair<String, ArrayList<PointF>>>

    abstract fun onMapData(): HashMap<String, ArrayList<PointF>>

}


class ActionGrassBoarder(intent: Intent, val data: HashMap<String, ByteArray>, lastItemKey: String, requestMapFinished: MutableLiveData<Boolean>): RequestMapAction(intent, lastItemKey, requestMapFinished) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        return "grass.$grassNumber"
    }

    override fun onViewModelData() = data

//    override fun onViewModelLiveData() = viewModel._grassData

    override fun onMapData() = MapData.grassData
}


class ActionObstacleBoarder(intent: Intent, val data: HashMap<String, ByteArray>, lastItemKey: String, requestMapFinished: MutableLiveData<Boolean>): RequestMapAction(intent, lastItemKey, requestMapFinished) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val obstacleNumber = intent.getByteExtra("obstacleNumber", -1)
        return "obstacle.$grassNumber.$obstacleNumber"
    }

    override fun onViewModelData() = data

//    override fun onViewModelLiveData() = viewModel._obstacleData

    override fun onMapData() = MapData.obstacleData
}


class ActionGrassPath(intent: Intent, val data: HashMap<String, ByteArray>, lastItemKey: String, requestMapFinished: MutableLiveData<Boolean>): RequestMapAction(intent, lastItemKey, requestMapFinished) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val targetGrassNumber = intent.getByteExtra("targetGrassNumber", -1)
        val pathNumber = intent.getByteExtra("pathNumber", -1)
        return "route.$grassNumber.$targetGrassNumber.$pathNumber"
    }

    override fun onViewModelData() = data

//    override fun onViewModelLiveData() = viewModel._grassPathData

    override fun onMapData() = MapData.grassPathData
}


class ActionChargingPath(intent: Intent, val data: HashMap<String, ByteArray>, lastItemKey: String, requestMapFinished: MutableLiveData<Boolean>): RequestMapAction(intent, lastItemKey, requestMapFinished) {

    override fun onKeyIndex(): String {
        val grassNumber = intent.getByteExtra("grassNumber", -1)
        val pathNumber = intent.getByteExtra("pathNumber", -1)
        return "charging.$grassNumber.$pathNumber"
    }

    override fun onViewModelData() = data

//    override fun onViewModelLiveData() = viewModel._chargingPathData

    override fun onMapData() = MapData.chargingPathData
}

fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
