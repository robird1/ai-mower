package com.ulsee.mower.ui.map

import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.MainActivity
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.data.RobotStatusState
import com.ulsee.mower.databinding.ActivitySetupMapBinding
import java.util.*
import kotlin.math.abs

private val TAG = SetupMapFragment::class.java.simpleName

class SetupMapFragment: Fragment() {
    lateinit var binding: ActivitySetupMapBinding
    lateinit var viewModel: SetupMapFragmentViewModel
    lateinit var bluetoothService: BluetoothLeService
    lateinit var state: SetupMapState


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[Enter] onCreate()")

//        bluetoothService = (activity as MainActivity).bluetoothService!!
        bluetoothService = (requireActivity().application as App).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView()")

        binding = ActivitySetupMapBinding.inflate(inflater, container, false)

        activity?.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE

        initViewModel()

        state = SetChargingStation(this)
        state.createView()


        initJoyStick()

        registerBLEReceiver()

        return binding.root
    }

    // TODO only unregister receiver if it is registered
    override fun onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView()")
        state.gattUpdateReceiver?.let {
            requireActivity().unregisterReceiver(state.gattUpdateReceiver)
            state.gattUpdateReceiver = null
        }
        super.onDestroyView()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, SetupMapFragmentFactory(
            BluetoothLeRepository(bluetoothService), binding
        )).get(SetupMapFragmentViewModel::class.java)
    }

    private fun registerBLEReceiver() {
        Log.d(TAG, "[Enter] registerBLEReceiver")
        val filter = IntentFilter()
        filter.addAction(RobotStatusState.ACTION_STATUS_RESPONSE)
        requireActivity().registerReceiver(state.gattUpdateReceiver, filter)
    }

    private fun initJoyStick() {
        val isStop = booleanArrayOf(false)
        val movement = doubleArrayOf(0.0)
        val rotation = doubleArrayOf(0.0)

        binding.joystick.setOnMoveListener { intAngle: Int, intStrength: Int ->
            val angle = intAngle.toDouble()
            val strength = intStrength.toDouble()
            rotation[0] = 0.0
            movement[0] = strength / 100.0
            isStop[0] = intAngle == 0 && intStrength == 0
            //            isStop[0] = intStrength == 0;
            if (isStop[0]) {
//                stopMove()
                // TODO
                return@setOnMoveListener
            }
            // calculate movement forward & backward
            /* 0~ 1*/if (angle > 0 && angle <= 90) movement[0] = movement[0] * angle / 90.0
            /* 1~ 0*/if (angle > 90 && angle <= 180) movement[0] = movement[0] * (1.0 - (angle - 90.0) / 90.0)
            /* 0~-1*/if (angle > 180 && angle <= 270) movement[0] = movement[0] * (-(angle - 180.0) / 90.0)
            /*-1~ 0*/if (angle > 270 && angle < 360) movement[0] = movement[0] * (-1 + (angle - 270.0) / 90.0)
            // calculate rotate
//            /*-1~ 0*/if (angle > 0 && angle <= 90) rotation[0] = -(90.0 - angle) / 90.0
//            /* 0~ 1*/if (angle > 90 && angle <= 180) rotation[0] = (angle - 90.0) / 90.0
//            /* 1~ 0*/if (angle > 180 && angle <= 270) rotation[0] = 1 - (angle - 180.0) / 90.0
//            /* 0~-1*/if (angle > 270 && angle < 360) rotation[0] = -(angle - 270.0) / 90
            // 靠近上下時不轉彎，靠近左右時不前後移動
            if (angle > 70 && angle < 110 || angle > 250 && angle < 290) rotation[0] = 0.0
            if (angle > 340 || angle < 20 || angle > 160 && angle < 200) movement[0] = 0.0
            // 下方，靠近正左正右時，轉彎方向不變
//            if (angle > 340 || angle > 180 && angle < 200) rotation[0] = -rotation[0];


//            Log.d(TAG, String.format("angle=%f, strength=%f, rotation[0]=%f, movement[0]=%f", angle, strength, rotation[0], movement[0]));
            if (isStop[0]) return@setOnMoveListener
//            movement[0] *= getSpeedRatio()

            rotation[0] = angle - 90
            if (rotation[0] < 0)
                rotation[0] = rotation[0] + 360

            if (rotation[0] > 0)
                rotation[0] = 360 - rotation[0]

            movement[0] = abs(movement[0]) * 50

            Log.d(TAG, "angle: ${rotation[0].toInt()} movement: ${movement[0]}")
            viewModel.moveRobot(rotation[0].toInt(), movement[0])

        }
    }

}