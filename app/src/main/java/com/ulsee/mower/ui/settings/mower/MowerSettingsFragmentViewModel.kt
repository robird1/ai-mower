package com.ulsee.mower.ui.settings.mower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.MapData
import com.ulsee.mower.utils.Event
import kotlinx.coroutines.launch

class MowerSettingsFragmentViewModel(private val bleRepository: BluetoothLeRepository) : ViewModel() {

    private val TAG = "MowerSettingsFragmentViewModel"
    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
    private var mSettings = MutableLiveData<MowerSettings>()
    val settings : LiveData<MowerSettings>
        get() = mSettings

    private var _writeSettingsSucceedLog = MutableLiveData<Event<String>>()
    val writeSettingsSucceedLog : LiveData<Event<String>>
        get() = _writeSettingsSucceedLog
    private var _fetchSettingsFailedLog = MutableLiveData<Event<String>>()
    val fetchSettingsFailedLog : LiveData<Event<String>>
        get() = _fetchSettingsFailedLog

    private var _deleteMapFailedLog = MutableLiveData<Event<String>>()
    val deleteMapFailedLog : LiveData<Event<String>>
        get() = _deleteMapFailedLog

    private var _deleteMapOkLog = MutableLiveData<Event<String>>()
    val deleteMapOkLog : LiveData<Event<String>>
        get() = _deleteMapOkLog

    fun getSettings() {
        mIsLoading.value = true
        viewModelScope.launch {
            bleRepository.lookupSettings()
        }
    }

    fun updateWorkingOnRainlyDay(isWorkingOnRainlyDay: Boolean) {
        mIsLoading.value = true
        viewModelScope.launch {
            val value : Byte = if(isWorkingOnRainlyDay) 0x01 else 0x00
            Log.i(TAG, "updateWorkingOnRainlyDay $value")
            bleRepository.configSettings(-125/*0x83*/, value)
        }
    }

    fun clearMap() {
        mIsLoading.value = true
        viewModelScope.launch {
            bleRepository.deleteAllMap()
        }
    }

    fun updateWorkingMode(workingMode: MowerWorkingMode) {
        mIsLoading.value = true
        viewModelScope.launch {
            val value : Byte = when(workingMode) {
                MowerWorkingMode.learning -> 0x00
                MowerWorkingMode.working -> 0x01
                MowerWorkingMode.learnAndWork -> 0x02
                MowerWorkingMode.gradual -> 0x03
                MowerWorkingMode.explosive -> 0x04
                else -> 0x00
            }
            Log.i(TAG, "updateWorkingMode $value")
            bleRepository.configSettings(-126/*82*/, value)
        }
    }

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "gattUpdateReceiver.onReceive ${intent.action}")
            when (intent.action){
                BLEBroadcastAction.ACTION_SETTINGS -> {
                    try {
                        mIsLoading.value = false
                        val result = intent.getIntExtra("result", -1) // 1 for ok, 0 for error
                        val operation_mode = intent.getIntExtra("operation_mode", -1)
                        val operationString = if (operation_mode == 1) "fetch" else "write"
                        val working_mode = intent.getIntExtra("working_mode", -1)
                        val rain_mode = intent.getIntExtra("rain_mode", -1)
                        val mower_count = intent.getIntExtra("mower_count", -1)
                        val knife_height = intent.getIntExtra("knife_height", -1)

                        Log.i(
                            TAG,
                            "gattUpdateReceiver.onReceive [$operation_mode.$operationString] ${intent.action}, result=$result, operation_mode=$operation_mode, working_mode=$working_mode, rain_mode=$rain_mode, mower_count=$mower_count, knife_height=$knife_height"
                        )

                        if (result != 1) {
                            _fetchSettingsFailedLog.value =
                                Event("[$operation_mode]($result)Failed to $operationString data")
//                            return
                        }
                        if (result == 1 && operation_mode == 0) {
                            _writeSettingsSucceedLog.value = Event("update succeed")
                        }

                        val settings = MowerSettings(
                            MowerWorkingMode(working_mode),
                            rain_mode,
                            mower_count,
                            knife_height
                        )
                        mSettings.value = settings
                    } catch (e: Exception) {
                        Log.e(TAG, "gattUpdateReceiver.onReceive exception: ${e.message}")
                        e.printStackTrace()
                        _fetchSettingsFailedLog.value = Event("Failed to fetch data: ${e.message}")
                    }
                }
                BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP -> {
                    mIsLoading.value = false
                    val result = intent.getIntExtra("result", -1) // 1 for ok, 0 for error
                    val command = intent.getIntExtra("command", -1)

                    Log.i(
                        TAG,
                        "gattUpdateReceiver.onReceive [$command] ${intent.action}, result=$result, command=$command"
                    )

                    if (result != 1) {
                        _fetchSettingsFailedLog.value =
                            Event("[$command]Failed to clear map")
//                            return
                    } else {
                        MapData.clear()
                        _deleteMapOkLog.value = Event("Clear Map Succeed!")
                    }
                }
            }
        }
    }

}