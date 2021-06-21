package com.ulsee.mower

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.RobotStatusState.Companion.ACTION_STATUS_RESPONSE

private val TAG = StatusFragmentViewModel::class.java.simpleName

class StatusFragmentViewModel(private val bleRepository: BluetoothLeRepository): ViewModel() {
    private var _powerIndication = MutableLiveData<String>()
    val powerIndication : LiveData<String>
        get() = _powerIndication

//    init {
//        getMowerStatus()
//    }

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val message = intent.getStringExtra("message")
            Log.d(TAG, "action: $action message: $message")
            when (action) {
                ACTION_STATUS_RESPONSE -> {
//                    val temp = message!!.split(" ")[17]
//                    val value = Integer.parseInt(temp, 16)
//                    _powerIndication.value = value.toString()
                }
            }
        }
    }

//    fun getMowerStatus() {
//        bleRepository.getStatus()
//    }

    fun disconnectDevice() {
        bleRepository.disconnectDevice()
    }

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