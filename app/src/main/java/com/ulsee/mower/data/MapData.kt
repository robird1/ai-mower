package com.ulsee.mower.data

import android.graphics.PointF
import android.util.Log
import com.ulsee.mower.ui.map.SetupMapState

private val TAG = MapData::class.java.simpleName

class MapData {
    companion object {
        var grassData = HashMap<String, ArrayList<PointF>>()
        var obstacleData = HashMap<String, ArrayList<PointF>>()
        var grassPathData = HashMap<String, ArrayList<PointF>>()
        var chargingPathData = HashMap<String, ArrayList<PointF>>()

        fun clear() {
            Log.d(TAG, "[Enter] clear()")
            grassData.clear()
            obstacleData.clear()
            grassPathData.clear()
            chargingPathData.clear()

            for (i in grassData.iterator()) {
                Log.d(TAG, "key: ${i.key} listSize: ${i.value.size}")
            }

        }
    }
}