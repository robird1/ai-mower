package com.ulsee.mower

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.ui.map.StatusFragmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private val TAG = MainActivityViewModel::class.java.simpleName

class MainActivityViewModel(): ViewModel() {

    fun bindService(activity: MainActivity) {
        viewModelScope.launch {
            bind(activity)
        }
    }

    suspend fun bind(activity: MainActivity) = suspendCoroutine<BluetoothLeService?> {
        Log.d(TAG, "[Enter] bindService()")
        val gattServiceIntent = Intent(activity, BluetoothLeService::class.java)

        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(
                componentName: ComponentName,
                service: IBinder
            ) {
                Log.d(TAG, "[Enter] onServiceConnected")
                 val bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
                it.resume(bluetoothService)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                it.resume(null)
            }
        }

        activity.bindService(gattServiceIntent, serviceConnection, androidx.appcompat.app.AppCompatActivity.BIND_AUTO_CREATE)
    }

}


//class MainActivityFactory(private val bleRepository: BluetoothLeRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(StatusFragmentViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return StatusFragmentViewModel(bleRepository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}