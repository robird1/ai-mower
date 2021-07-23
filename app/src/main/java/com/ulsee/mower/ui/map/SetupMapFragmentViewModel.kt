package com.ulsee.mower.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.CommandGlobalParameter.GlobalParameter
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.utils.Event
import kotlinx.coroutines.launch
import java.lang.reflect.Type

private val TAG = SetupMapFragmentViewModel::class.java.simpleName

class SetupMapFragmentViewModel(private val bleRepository: BluetoothLeRepository): ViewModel() {
    private var _statusIntent = MutableLiveData<Intent>()
    val statusIntent : LiveData<Intent>
        get() = _statusIntent
    private var _startStopIntent = MutableLiveData<Intent>()
    val startStopIntent : LiveData<Intent>
        get() = _startStopIntent
    private var _borderRecordIntent = MutableLiveData<Event<Intent>>()
    val borderRecordIntent : LiveData<Event<Intent>>
        get() = _borderRecordIntent

    private var _requestMapFinished = MutableLiveData<Boolean>()
    val requestMapFinished : LiveData<Boolean>
        get() = _requestMapFinished

    private var _deleteMapFinished = MutableLiveData<Boolean>()
    val deleteMapFinished : LiveData<Boolean>
        get() = _deleteMapFinished

    var grassDataMap = HashMap<String, ByteArray>()
    var obstacleDataMap = HashMap<String, ByteArray>()
    var grassPathDataMap = HashMap<String, ByteArray>()
    var chargingPathDataMap = HashMap<String, ByteArray>()

    private var globalList = ArrayList<GlobalParameter> ()
    private var grassCount = 0
    private var lastItemKey = ""

    var gattUpdateReceiver: BroadcastReceiver? = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
//            Log.d(TAG, "[Enter] onReceive() intent.action: ${intent.action}")
            when (intent.action) {
                BLEBroadcastAction.ACTION_STATUS -> {
                    _statusIntent.value = intent

                }
                BLEBroadcastAction.ACTION_START_STOP -> {
                    _startStopIntent.value = intent

                }
                BLEBroadcastAction.ACTION_BORDER_RECORD -> {
                    Log.d(TAG, "[ACTION_BORDER_RECORD]")
                    Log.d("123", "_borderRecordIntent.value = Event(intent)")
                    _borderRecordIntent.value = Event(intent)

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
//                            _hasMapData.value = false

                        } else {
//                            _hasMapData.value = true

                            lastItemKey = getLastItemKey()

                            grassCount = size

                            requestMapData()
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
                BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP -> {
                    Log.d(TAG, "[ACTION_REQUEST_DELETE_MAP]")
                    val type = intent.getStringExtra("type")
                    when (type) {
                        "grass" -> {
                            val grassNumber = intent.getByteExtra("grassNumber", -1)
                            Log.d("789", "$grassNumber")
                            deleteGrass(grassNumber)

                        }
                        "obstacle" -> {
                            val grassNumber = intent.getByteExtra("grassNumber", -1)
                            val obstacleNumber = intent.getByteExtra("obstacleNumber", -1)
                            Log.d("789", "$grassNumber.$obstacleNumber")
                            deleteObstacle(grassNumber, obstacleNumber)

                        }
                        "charging" -> {
                            val grassNumber = intent.getByteExtra("grassNumber", -1)
                            val pathNumber = intent.getByteExtra("pathNumber", -1)
                            Log.d("789", "$grassNumber.$pathNumber")
                            deleteChargingPath(grassNumber, pathNumber)
                        }
                        "route" -> {
                            val grassNumber = intent.getByteExtra("grassNumber", -1)
                            val targetGrassNumber = intent.getByteExtra("targetGrassNumber", -1)
                            val pathNumber = intent.getByteExtra("pathNumber", -1)
                            Log.d("789", "$grassNumber.$targetGrassNumber.$pathNumber")
                            deleteGrassPath(grassNumber, targetGrassNumber, pathNumber)
                        }
                    }
                }
                BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP -> {
                    Log.d(TAG, "[ACTION_RESPONSE_DELETE_MAP]")
                    val command = intent.getIntExtra("command", -1)
                    val result = intent.getIntExtra("result", -1)

                    val resultString = when (result) {
                        0 -> "failed"
                        1 -> "success"
                        2 -> "操作对象不存在"
                        3 -> "FLASH空间不足"
                        4 -> "FLASH擦除错误"
                        255 -> "未知错误"
                        else -> ""
                    }

                    if (result == 1) {
                        _deleteMapFinished.value = true
                    }

                    Log.d("789", "result: $resultString")
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

    fun moveRobot(rotation: Int, movement: Double) {
        bleRepository.moveRobot(rotation, movement)
    }

    fun getStatusPeriodically() {
        viewModelScope.launch {
            bleRepository.getStatusPeriodically()
        }
    }

    fun cancelStatusTask() {
        bleRepository.cancelStatusTask()
    }

    fun recordBoundary(command: Int, subject: Int) {
        bleRepository.recordBoundary(command, subject)
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

    private fun deleteGrass(grassNumber: Byte) {
        bleRepository.deleteGrass(grassNumber)
    }

    private fun deleteObstacle(grassNumber: Byte, obstacleNumber: Byte) {
        bleRepository.deleteObstacle(grassNumber, obstacleNumber)
    }

    private fun deleteChargingPath(grassNumber: Byte, pathNumber: Byte) {
        bleRepository.deleteChargingPath(grassNumber, pathNumber)
    }

    private fun deleteGrassPath(grassNumber: Byte, targetGrassNumber: Byte, pathNumber: Byte) {
        bleRepository.deleteGrassPath(grassNumber, targetGrassNumber, pathNumber)
    }


}


class SetupMapFragmentFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupMapFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupMapFragmentViewModel(bleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}