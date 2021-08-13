package com.ulsee.mower.ui.schedule

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
import com.ulsee.mower.data.model.Device
import com.ulsee.mower.ui.settings.mower.MowerSettings
import com.ulsee.mower.ui.settings.mower.MowerWorkingMode
import com.ulsee.mower.utils.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class ScheduleEditorFragmentViewModel(private val bleRepository: BluetoothLeRepository) : ViewModel() {

    private val TAG = "ScheduleEditorFragmentViewModel"

    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
    private var mSchedules = MutableLiveData<Calendar>()
    val schedules : LiveData<Calendar>
        get() = mSchedules
    private var _fetchScheduleFailedLog = MutableLiveData<Event<String>>()
    val fetchScheduleFailedLog : LiveData<Event<String>>
        get() = _fetchScheduleFailedLog

    private var mSettings = MutableLiveData<MowerSettings>()
    val settings : LiveData<MowerSettings>
        get() = mSettings

    private var _writeScheduleSuccessLog = MutableLiveData<Event<String>>()
    val writeScheduleSuccessLog : LiveData<Event<String>>
        get() = _writeScheduleSuccessLog
    private var _fetchSettingsFailedLog = MutableLiveData<Event<String>>()
    val fetchSettingsFailedLog : LiveData<Event<String>>
        get() = _fetchSettingsFailedLog

    fun getSettings() {
        mIsLoading.value = true
        viewModelScope.launch {
            bleRepository.lookupSettings()
        }
    }

    fun getSchedule() {
        mIsLoading.value = true
        viewModelScope.launch {
            // todo: fetch settings

            delay(500)
//            mSchedules.value = ....

            mIsLoading.value = false
        }
    }

    fun save(calendar: Calendar) {
        mIsLoading.value = true
        if (mSettings.value == null) {
            _fetchScheduleFailedLog.value = Event("Error: settings not found")
            return
        }
        val mowerCount = mSettings.value!!.mowerCount
        viewModelScope.launch {
            val calendarDataList = ArrayList<Int>()

            for(weekday in 1..7) {
                val schedules = calendar.schedules[weekday-1]
                for(scheduleIdx in 1..5) {
                    val idx = scheduleIdx-1
                    if(schedules.size > idx) {
                        val schedule = schedules[idx]
                        calendarDataList.add(schedule.beginAt)
                        calendarDataList.add(schedule.duration)
                    } else {
                        calendarDataList.add(0)
                        calendarDataList.add(0)
                    }
                }
            }
            bleRepository.configSchedule(calendar.utc.toShort(), calendarDataList, mowerCount)
        }
    }

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "gattUpdateReceiver.onReceive ${intent.action}")
            when (intent.action){
                BLEBroadcastAction.ACTION_SCHEDULING -> {
                    try {
                        mIsLoading.value = false

                        val result = intent.getIntExtra("result", -1) // 1 for ok, 0 for error
                        val operation_mode = intent.getIntExtra("operation_mode", -1)
                        val operationString = if(operation_mode == 0) "read" else "write"
                        val utc = intent.getShortExtra("utc", -1)
                        val calendar = intent.getSerializableExtra("calendar") as List<Byte>

                        Log.i(
                            TAG,
                            "gattUpdateReceiver.onReceive [$operation_mode.$operationString] ${intent.action}, result=$result, operation_mode=$operation_mode, utc=$utc, calendar=$calendar"
                        )

                        if (calendar.size != 70) {
                            Log.e(TAG, "gattUpdateReceiver.onReceive [$operation_mode.$operationString] ${intent.action}, result=$result, operation_mode=$operation_mode, utc=$utc, calendar=$calendar, but calendar size must be 70")
                            _fetchScheduleFailedLog.value = Event("[$operation_mode.$operationString] Error calendar size ${calendar.size} (70 expected)")
                            return
                        }
                        if (result != 1) {
                            _fetchScheduleFailedLog.value = Event("[$operation_mode.$operationString]($result)Failed to $operationString calendar")
                            return
                        }
                        val data = Calendar(
                            utc.toInt(),
                            calendar
                        )
                        if (result == 1 && operation_mode == 1) {
                            _writeScheduleSuccessLog.value = Event("Update Succeed!")
                        }
                        mSchedules.value = data
                    } catch(e: Exception) {
                        Log.e(TAG, "gattUpdateReceiver.onReceive exception: ${e.message}")
                        e.printStackTrace()
                        _fetchScheduleFailedLog.value = Event("Failed to fetch calendar: ${e.message}")
                    }
                }
                BLEBroadcastAction.ACTION_SETTINGS -> {
                    try {
                        mIsLoading.value = false
                        val result = intent.getIntExtra("result", -1) // 1 for ok, 0 for error
                        val operation_mode = intent.getIntExtra("operation_mode", -1)
                        val operationString = if(operation_mode == 1) "read" else "write"
                        val working_mode = intent.getIntExtra("working_mode", -1)
                        val rain_mode = intent.getIntExtra("rain_mode", -1)
                        val mower_count = intent.getIntExtra("mower_count", -1)
                        val knife_height = intent.getIntExtra("knife_height", -1)

                        Log.i(
                            TAG,
                            "gattUpdateReceiver.onReceive [$operation_mode.$operationString] ${intent.action}, result=$result, operation_mode=$operation_mode, working_mode=$working_mode, rain_mode=$rain_mode, mower_count=$mower_count, knife_height=$knife_height"
                        )

                        if (result != 1) {
                            _fetchSettingsFailedLog.value = Event("[$operation_mode]($result)Failed to $operationString data")
//                            return
                        }
                        val settings = MowerSettings(
                            MowerWorkingMode(working_mode),
                            rain_mode,
                            mower_count,
                            knife_height
                        )
                        mSettings.value = settings
                    } catch(e: Exception) {
                        Log.e(TAG, "gattUpdateReceiver.onReceive exception: ${e.message}")
                        e.printStackTrace()
                        _fetchSettingsFailedLog.value = Event("Failed to fetch data: ${e.message}")
                    }
                }
            }
        }
    }
}