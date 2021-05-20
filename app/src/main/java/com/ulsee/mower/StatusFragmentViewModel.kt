package com.ulsee.mower

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.ulsee.mower.data.BluetoothLeService

private val TAG = StatusFragmentViewModel::class.java.simpleName

class StatusFragmentViewModel: ViewModel() {
//    var bluetoothService: BluetoothLeService? = null
//
//
//    private val serviceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//            componentName: ComponentName,
//            service: IBinder
//        ) {
//            Log.d(TAG, "[Enter] onServiceConnected")
//
//            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
//
//
//        }
//
//        override fun onServiceDisconnected(componentName: ComponentName) {
//            Log.d(TAG, "[Enter] onServiceDisconnected")
//
//            bluetoothService = null
//        }
//    }
//
//    fun bindService(activity: AppCompatActivity) {
//        val gattServiceIntent = Intent(activity, BluetoothLeService::class.java)
//        activity.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
//
//    }
//
//    fun disconnectService() {
//        bluetoothService!!.disconnectDevice()
//
//    }
}