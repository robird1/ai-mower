package com.ulsee.mower.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.RobotStatusState
import com.ulsee.mower.databinding.ActivitySetupMapBinding

private val TAG = SetupMapFragmentViewModel::class.java.simpleName

class SetupMapFragmentViewModel(private val bleRepository: BluetoothLeRepository, private val binding: ActivitySetupMapBinding): ViewModel() {

//    val gattUpdateReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                RobotStatusState.ACTION_STATUS_RESPONSE -> {
//                    val x = intent.getIntExtra("x", 0)
//                    val y = intent.getIntExtra("y", 0)
//                    val angle = intent.getFloatExtra("angle", 0F)
//                    Log.d(TAG, "x: $x y: $y angle: $angle")
//
//                    // TODO OperationType
//                    binding.mapView.updateRobotPosition(x, y, angle, MapView.OperationType.WorkingBorder)
//                }
//
//                RobotStatusState.ACTION_BORDER_RECORD_RESPONSE -> {
//
//                }
//            }
//        }
//    }

    fun moveRobot(rotation: Int, movement: Double) {
        bleRepository.moveRobot(rotation, movement)
    }

//    fun getStatus() {
//        bleRepository.getStatus()
//    }

    fun getStatusPeriodically() {
        bleRepository.getStatusPeriodically()
    }

    fun startRecordChargingPath() {
        bleRepository.startRecordChargingPath()

    }

//    fun setWorkingBorderPoint() {
//        bleRepository.setWorkingBorderPoint()
//    }

}


class SetupMapFragmentFactory(private val bleRepository: BluetoothLeRepository, private val binding: ActivitySetupMapBinding) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupMapFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupMapFragmentViewModel(bleRepository, binding) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}