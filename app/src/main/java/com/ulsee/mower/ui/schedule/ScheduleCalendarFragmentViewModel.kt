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
import com.ulsee.mower.ui.settings.mower.MowerSettings
import com.ulsee.mower.ui.settings.mower.MowerWorkingMode
import com.ulsee.mower.utils.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class ScheduleCalendarFragmentViewModel(private val bleRepository: BluetoothLeRepository) : ViewModel() {

    private val TAG = "ScheduleCalendarFragmentViewModel"

    private var mIsLoading : MutableLiveData<Boolean> = MutableLiveData()
    val isLoading : LiveData<Boolean>
        get() = mIsLoading
    private var mSchedules = MutableLiveData<Calendar>()
    val schedules : LiveData<Calendar>
        get() = mSchedules
    private var _fetchScheduleFailedLog = MutableLiveData<Event<String>>()
    val fetchScheduleFailedLog : LiveData<Event<String>>
        get() = _fetchScheduleFailedLog


    fun getSchedule() {
        mIsLoading.value = true
        viewModelScope.launch {
            bleRepository.lookupSchedule()
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
                            _fetchScheduleFailedLog.value = Event("[$operation_mode.$operationString]($result)Failed to fetch calendar")
                            return
                        }
                        val data = Calendar(
                            utc.toInt(),
                            calendar
                        )
                        mSchedules.value = data
                    } catch(e: Exception) {
                        Log.e(TAG, "gattUpdateReceiver.onReceive exception: ${e.message}")
                        e.printStackTrace()
                        _fetchScheduleFailedLog.value = Event("Failed to fetch calendar: ${e.message}")
                    }
                }
            }
        }
    }
}