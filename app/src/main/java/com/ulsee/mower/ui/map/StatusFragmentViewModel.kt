package com.ulsee.mower.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.CommandGlobalParameter.GlobalParameter
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.StartStop
import kotlinx.coroutines.launch
import java.lang.reflect.Type


private val TAG = StatusFragmentViewModel::class.java.simpleName

open class StatusFragmentViewModel(private val bleRepository: BluetoothLeRepository): ViewModel() {
    private var _hasMapData = MutableLiveData<Boolean>()
    val hasMapData : LiveData<Boolean>
        get() = _hasMapData
    private var _requestMapFinished = MutableLiveData<Boolean>()
    val requestMapFinished : LiveData<Boolean>
        get() = _requestMapFinished
    private var _startStopResult = MutableLiveData<Pair<Boolean,String>>()
    val startStopResult : LiveData<Pair<Boolean,String>>
        get() = _startStopResult
    private var _statusIntent = MutableLiveData<Intent>()
    val statusIntent : LiveData<Intent>
        get() = _statusIntent
//    private var _requestMowingFinished = MutableLiveData<Boolean>()
//    val requestMowingFinished : LiveData<Boolean>
//        get() = _requestMowingFinished

    var grassDataMap = HashMap<String, ByteArray>()
    var obstacleDataMap = HashMap<String, ByteArray>()
    var grassPathDataMap = HashMap<String, ByteArray>()
    var chargingPathDataMap = HashMap<String, ByteArray>()

    private var globalList = ArrayList<GlobalParameter> ()
    private var grassCount = 0

    private var lastItemKey = ""

    private var mowingList = ArrayList<PointF> ()
    private var _mowingDataList = MutableLiveData<ArrayList<PointF>>()
    val mowingDataList : LiveData<ArrayList<PointF>>
        get() = _mowingDataList



    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                BLEBroadcastAction.ACTION_STATUS -> {
                    _statusIntent.value = intent
                }
                BLEBroadcastAction.ACTION_START_STOP -> {
                    val result = intent.getIntExtra("result", -1)
                    if (result == 0x01) {
                        val command = intent.getIntExtra("command", -1)
//                        when (command) {
//                            START_MOWING, PAUSE_MOWING, RESUME_MOWING, BACK_CHARGING_STATION -> {
                                _startStopResult.value = Pair(true, command.toString())
//                            }
//                            else -> {
//
//                            }
//                        }

                    } else {
                        val errorMessage = StartStop.ErrorCode.map[result] ?: ""
                        _startStopResult.value = Pair(false, errorMessage)
                    }
                }
                BLEBroadcastAction.ACTION_GLOBAL_PARAMETER -> {
                    Log.d(TAG, "[ACTION_GLOBAL_PARAMETER]")
                    val jsonString = intent.getStringExtra("data")
                    val listOfMyClassObject: Type = object : TypeToken<ArrayList<GlobalParameter?>?>() {}.type
                    globalList = Gson().fromJson(jsonString, listOfMyClassObject)
                    grassDataMap.clear()
                    obstacleDataMap.clear()
                    grassPathDataMap.clear()
                    chargingPathDataMap.clear()

                    globalList.apply {
                        if (size == 0) {
                            _hasMapData.value = false

                        } else {
                            _hasMapData.value = true

                            lastItemKey = getLastItemKey()

                            grassCount = size

                            requestMapData()
                            Log.d("777", "[Enter] requestMapData()")

                        }
                    }
                }
                BLEBroadcastAction.ACTION_GRASS_BOARDER -> {
                    Log.d(TAG, "[ACTION_GRASS_BOARDER]")
                    ActionGrassBoarder(intent, grassDataMap, lastItemKey, _requestMapFinished).execute()
                }
                BLEBroadcastAction.ACTION_OBSTACLE_BOARDER -> {
                    Log.d(TAG, "[ACTION_OBSTACLE_BOARDER]")
                    ActionObstacleBoarder(intent, obstacleDataMap, lastItemKey, _requestMapFinished).execute()
                }
                BLEBroadcastAction.ACTION_GRASS_PATH -> {
                    Log.d(TAG, "[ACTION_GRASS_PATH]")
                    ActionGrassPath(intent, grassPathDataMap, lastItemKey, _requestMapFinished).execute()

                }
                BLEBroadcastAction.ACTION_CHARGING_PATH -> {
                    Log.d(TAG, "[ACTION_CHARGING_PATH]")
                    ActionChargingPath(intent, chargingPathDataMap, lastItemKey, _requestMapFinished).execute()
                }
                BLEBroadcastAction.ACTION_MOWING_DATA -> {
                    Log.d(TAG, "[ACTION_MOWING_DATA]")
                    val packetCount = intent.getIntExtra("packetCount", -1)
                    val packetNumber = intent.getIntExtra("packetNumber", -1)

                    if (packetNumber == 0) {
                        mowingList.clear()
                    }

                    if (packetNumber < packetCount - 1) {
                        val jsonString = intent.getStringExtra("data")
                        val listOfMyClassObject: Type = object : TypeToken<ArrayList<PointF?>?>() {}.type
                        val tempList: ArrayList<PointF> = Gson().fromJson(jsonString, listOfMyClassObject)
                        mowingList.addAll(tempList)

//                        getMowingData(packetNumber + 1)

                    } else {
                        _mowingDataList.value = mowingList
                    }
                }
            }
        }
    }

    private fun ArrayList<GlobalParameter>.getLastItemKey(): String {
        var key = ""
        for (i in iterator()) {
            key = "grass.${i.grassNumber.toString()}"

            for (obstacleNumber in 0 until i.obstacleCount) {
                key = "obstacle.${i.grassNumber.toString() + "." + obstacleNumber}"
            }

            for (pathNumber in 0 until i.chargingPathCount) {
                key = "charging.${i.grassNumber.toString() + "." + pathNumber}"
            }

            for (j in i.targetGrass) {
                for (pathNumber in 0 until j.targetPathCount) {
                    key = "route.${i.grassNumber.toString() + "." + j.targetGrassNumber + "." + pathNumber}"
                }
            }
        }
        return key
    }

    private fun ArrayList<GlobalParameter>.requestMapData() {
        for (i in iterator()) {
            requestGrassData(i)
            requestObstacleData(i)
            requestChargingPathData(i)
            requestGrassPathData(i)
        }
    }

    private fun requestChargingPathData(i: GlobalParameter) {
        for (pathNumber in 0 until i.chargingPathCount) {
            getChargingPath(i.grassNumber, 0x00, pathNumber.toByte())
        }
    }

    private fun requestGrassPathData(i: GlobalParameter) {
        for (j in i.targetGrass) {
            for (pathNumber in 0 until j.targetPathCount) {
                getGrassPath(i.grassNumber, 0x00, j.targetGrassNumber, pathNumber.toByte())
            }
        }
    }

    private fun requestObstacleData(i: GlobalParameter) {
        for (obstacleNumber in 0 until i.obstacleCount) {
            getObstacleBoundary(i.grassNumber, 0x00, obstacleNumber.toByte())
        }
    }

    private fun requestGrassData(i: GlobalParameter) {
        getGrassBoundary(i.grassNumber, 0x00)
    }

    fun getStatusPeriodically() {
        viewModelScope.launch {
            bleRepository.getStatusPeriodically()
        }
    }

    fun disconnectDevice() {
        bleRepository.disconnectDevice()
    }

    fun startStop(command: Int) {
        bleRepository.startStop(command)
    }

    fun getMapGlobalParameters() {
        bleRepository.getMapGlobalParameters()
    }

    private fun getGrassBoundary(grassNumber: Byte, packetNumber: Byte) {
        bleRepository.getGrassBoundary(grassNumber, packetNumber)
    }

    private fun getObstacleBoundary(grassNumber: Byte, packetNumber: Byte, obstacleNumber: Byte) {
        bleRepository.getObstacleBoundary(grassNumber, packetNumber, obstacleNumber)
    }

    private fun getGrassPath(grassNumber: Byte, packetNumber: Byte, targetNumber: Byte, pathNumber: Byte) {
        bleRepository.getGrassPath(grassNumber, packetNumber, targetNumber, pathNumber)
    }

    private fun getChargingPath(grassNumber: Byte, packetNumber: Byte, pathNumber: Byte) {
        bleRepository.getChargingPath(grassNumber, packetNumber, pathNumber)
    }

    fun getMowingData(packetNumber: Int) {
        viewModelScope.launch {
            bleRepository.getMowingData(packetNumber)
        }
    }

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

}


class StatusFragmentFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatusFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatusFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}