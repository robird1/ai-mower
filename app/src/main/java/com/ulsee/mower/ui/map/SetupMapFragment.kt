package com.ulsee.mower.ui.map

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.jarvislau.destureviewbinder.GestureViewBinder
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.ble.RESPONSE_FAILED
import com.ulsee.mower.ble.RESPONSE_SUCCESS
import com.ulsee.mower.data.*
import com.ulsee.mower.databinding.ActivitySetupMapBinding
import java.util.*
import kotlin.math.abs

private val TAG = SetupMapFragment::class.java.simpleName

class SetupMapFragment: Fragment() {
    lateinit var binding: ActivitySetupMapBinding
    lateinit var viewModel: SetupMapFragmentViewModel
//    lateinit var statusViewModel: StatusFragmentViewModel
    lateinit var bluetoothService: BluetoothLeService
    lateinit var state: SetupMapState
    var isTestOrSaveAppeared = false
    var isSaveOrDiscardAppeared = false
    var isReceiverRegistered = false
    private val emergencyStopIdxList = ArrayList<Int>()
    private val interruptionIdxList = ArrayList<Int>()
    var signalQuality = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[Enter] onCreate()")

//        bluetoothService = (activity as MainActivity).bluetoothService!!
        bluetoothService = (requireActivity().application as App).bluetoothService!!

        initViewModel()

        registerBLEReceiver()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView()")

        binding = ActivitySetupMapBinding.inflate(inflater, container, false)

        activity?.requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE

        viewModel.getStatusPeriodically()

        state = if (MapData.grassData.size > 0) {
            StateControlPanel(this)
        } else {
            StartGrass(this)
        }
        state.createView()

        binding.mapView.initData()

        GestureViewBinder.bind(requireContext(), binding.constraintLayout, binding.mapView);

        initJoyStick()

        addOnBackPressedCallback()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initStatusObserver()
        initStartStopObserver()
        initBorderRecordObserver()
        initGlobalParameterObserver()
        initDeleteMapObserver()
    }

    override fun onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView()")
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "[Enter] onDestroy()")
//        viewModel.gattUpdateReceiver?.let {
//        if (isReceiverRegistered) {
            Log.d(TAG, "[Enter] unregisterReceiver")
            requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
//            isReceiverRegistered = false
//        }
//        }
        super.onDestroy()
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d("777", "[Enter] handleOnBackPressed()")
                    if (!binding.progressView.isVisible) {
                        state.onBackPressed()
                    } else {
                        binding.progressView.isVisible = false
                    }
                }
            })
    }

    fun backToStatusScreen() {
        val dialog = AlertDialog.Builder(context)
        dialog.setMessage("Return to status screen ?")
            .setCancelable(false)
            .setPositiveButton(R.string.button_confirm) { _, _ ->

                activity?.requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
                findNavController().popBackStack()

            }
            .setNegativeButton("cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun registerBLEReceiver() {
//        if (!isReceiverRegistered) {
            Log.d(TAG, "[Enter] registerBLEReceiver")
            val filter = IntentFilter()
            filter.addAction(BLEBroadcastAction.ACTION_STATUS)
            filter.addAction(BLEBroadcastAction.ACTION_BORDER_RECORD)
            filter.addAction(BLEBroadcastAction.ACTION_START_STOP)

            filter.addAction(BLEBroadcastAction.ACTION_GLOBAL_PARAMETER)
            filter.addAction(BLEBroadcastAction.ACTION_GRASS_BOARDER)
            filter.addAction(BLEBroadcastAction.ACTION_OBSTACLE_BOARDER)
            filter.addAction(BLEBroadcastAction.ACTION_GRASS_PATH)
            filter.addAction(BLEBroadcastAction.ACTION_CHARGING_PATH)
            filter.addAction(BLEBroadcastAction.ACTION_REQUEST_DELETE_MAP)
            filter.addAction(BLEBroadcastAction.ACTION_RESPONSE_DELETE_MAP)

            requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)

            isReceiverRegistered = true
//        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, SetupMapFragmentFactory(
            BluetoothLeRepository(bluetoothService)
        )).get(SetupMapFragmentViewModel::class.java)

//        statusViewModel = ViewModelProvider(this, StatusFragmentFactory(
//            BluetoothLeRepository(bluetoothService))).get(StatusFragmentViewModel::class.java)
    }

//    private fun initRequestMapFinishedObserver() {
//        statusViewModel.requestMapFinished.observe(viewLifecycleOwner) { isFinished ->
//            Log.d(TAG, "[Enter] requestMapFinished.observe()")
//            if (isFinished) {
//                binding.mapView.initData()
//                binding.mapView.postInvalidate()
//            }
//        }
//    }

    private fun initGlobalParameterObserver() {
//        if (!viewModel.requestMapFinished.hasObservers()) {
            viewModel.requestMapFinished.observe(viewLifecycleOwner) { isFinished ->
                binding.progressView.isVisible = false
                Log.d(TAG, "[Enter] requestMapFinished.observe")
                Log.d("123", "[Enter] requestMapFinished.observe")
                if (isFinished) {
                    binding.mapView.initData()
                }
            }
//        }

    }

    private fun initStatusObserver() {
        viewModel.statusIntent.observe(viewLifecycleOwner) { intent ->
            val x = intent.getIntExtra("x", 0)
            val y = intent.getIntExtra("y", 0)
            val angle = intent.getFloatExtra("angle", 0F)
            val robotStatus = intent.getStringExtra("robot_status") ?: ""
            val interruptionCode = intent.getStringExtra("interruption_code") ?: ""
            val testingBoundaryState = intent.getIntExtra("testing_boundary", -1)
            signalQuality = intent.getIntExtra("signal_quality", -1)

            checkRobotStatus(robotStatus)
            checkInterruptionCode(interruptionCode)

            when (testingBoundaryState) {
                Status.TestingBoundaryState.WAITING -> {          // 等待指令(饶边或保存)
                    if (!isTestOrSaveAppeared) {
                        showSaveOrTestBoundary()
                        isTestOrSaveAppeared = true
                    }
                }
                Status.TestingBoundaryState.TEST_FAILED -> {      // 绕边失败
                    isTestOrSaveAppeared = false

//                    showTestBoundaryFailed()
                }
                Status.TestingBoundaryState.TEST_SUCCESS -> {     // 绕边成功
                    isTestOrSaveAppeared = false
                    if (!isSaveOrDiscardAppeared) {
                        showSaveOrDiscardBoundary()
                        isSaveOrDiscardAppeared = true
                    }
                }
                Status.TestingBoundaryState.TEST_CANCELLED -> {   // 取消绕边

                }

            }

            binding.mapView.notifyRobotCoordinate(x, y, angle, state)

        }
    }

    private fun initStartStopObserver() {
        viewModel.startStopIntent.observe(viewLifecycleOwner) { intent ->
            val result = intent.getIntExtra("result", -1)
            val command = intent.getIntExtra("command", -1)

            when (result) {
                RESPONSE_SUCCESS -> {
//                    showSaveOrDiscardBoundary()
                    showDialog("success")
                }
                RESPONSE_FAILED -> {
                    showDialog("failed")
                }
                else -> {
                    val info = StartStop.ErrorCode.map[result]
                    showDialog(info ?: "unknown error code")
                }
            }
        }
    }

    private fun initBorderRecordObserver() {
            Log.d(TAG, "[Enter] initBorderRecordObserver")
            viewModel.borderRecordIntent.observe(viewLifecycleOwner) { it ->
                it.getContentIfNotHandled()?.let { intent ->
                    val result = intent.getIntExtra("result", -1)
                    val command = intent.getIntExtra("command", -1)
                    val subject = intent.getIntExtra("subject", -1)
                    Log.d(TAG, "result: $result command: $command subject: $subject")

                    val info = RecordBoundary.ErrorCode.map[result]

                    val cmd = when (command) {
                        0x00 -> "START_RECORD"
                        0x01 -> "FINISH_RECORD"
                        0x02 -> "START_POINT_MODE"
                        0x03 -> "SET_POINT"
                        0x04 -> "FINISH_POINT_MODE"
                        0x05 -> "CANCEL_RECORD"
                        0x06 -> "SAVE_BOUNDARY"
                        0x07 -> "DISCARD_BOUNDARY"
                        else -> ""
                    }
                    Log.d("456", "info: $info command: $cmd")
                    if (result == RESPONSE_SUCCESS) {
                        getBoundaryRecordAction(command, subject, result, intent).execute()

                    } else {
                        showDialog(info ?: "unknown error code")
                    }
                }
            }
    }

    private fun initDeleteMapObserver() {
        viewModel.deleteMapFinished.observe(viewLifecycleOwner) {
            if (it) {
                binding.mapView.resetData()
                viewModel.getMapGlobalParameters()
            }
        }
    }

    private fun showSaveOrTestBoundary() {
        val dialog = AlertDialog.Builder(context)
            .setMessage("Check the working boundary or just save it?")
            .setCancelable(false)
            .setPositiveButton("save") { it, _ ->
                viewModel.recordBoundary(
                    RecordBoundary.Command.SAVE_BOUNDARY,
                    RecordBoundary.Subject.GRASS
                )
                it.dismiss()
            }
            .setNegativeButton("check") { it, _ ->
                binding.progressView.isVisible = false
                viewModel.startStop(StartStop.Command.TEST_WORKING_BOUNDARY)
                it.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun showSaveOrDiscardBoundary() {
        val dialog = AlertDialog.Builder(context)
            .setMessage("Save or discard the working boundary?")
            .setCancelable(false)
            .setPositiveButton("save") { it, _ ->
//                    Log.d(TAG, "[Enter] viewModel.recordBoundary(SAVE_BOUNDARY, GRASS)")
                viewModel.recordBoundary(
                    RecordBoundary.Command.SAVE_BOUNDARY,
                    RecordBoundary.Subject.GRASS
                )
                it.dismiss()
            }
            .setNegativeButton("discard") { it, _ ->
                viewModel.recordBoundary(
                    RecordBoundary.Command.DISCARD_BOUNDARY,
                    RecordBoundary.Subject.GRASS
                )
                it.dismiss()
            }
            .create()

        dialog.show()

    }

    private fun checkInterruptionCode(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !interruptionIdxList.contains(idx)) {
                val message = Status.Interruption.map[idx] ?: "unknown error"
                showInterruptionDialog(message, idx)
                interruptionIdxList.add(idx)
            }
        }
    }

    private fun checkRobotStatus(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !emergencyStopIdxList.contains(idx)) {
                val message = Status.RobotStatus.map[idx] ?: "unknown error"
                showEmergencyStopDialog(message, idx)
                emergencyStopIdxList.add(idx)
            }
        }
    }

    private fun showEmergencyStopDialog(message: String, bitIndex: Int) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Reset") { it, _ ->
                viewModel.startStop(StartStop.Command.RESUME_EMERGENCY_STOP)
                emergencyStopIdxList.remove(bitIndex)
                it.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun showInterruptionDialog(message: String, bitIndex: Int) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Reset") { it, _ ->
                viewModel.startStop(StartStop.Command.RESUME_FROM_INTERRUPT)
                interruptionIdxList.remove(bitIndex)
                it.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun showDialog(result: String) {
        Toast.makeText(context, "result: $result", Toast.LENGTH_SHORT).show()
    }

    private fun getBoundaryRecordAction(command: Int, subject: Int, result: Int, intent: Intent): ActionRecordBoundary {
        return when (command) {
            RecordBoundary.Command.START_RECORD -> {
                ActionStartRecord(subject, result, this)
            }
            RecordBoundary.Command.START_POINT_MODE -> {
                ActionStartPointMode(subject, result, this)
            }
            RecordBoundary.Command.SET_POINT -> {
                ActionSetPoint(subject, result, this)
            }
            RecordBoundary.Command.FINISH_POINT_MODE -> {
                ActionFinishPointMode(subject, result, this)
            }
            RecordBoundary.Command.FINISH_RECORD -> {
                ActionFinishRecord(subject, result, this)
            }
            RecordBoundary.Command.CANCEL_RECORD -> {
                ActionCancelRecord(subject, result, this)
            }
            RecordBoundary.Command.SAVE_BOUNDARY -> {
                ActionSaveBoundary(subject, result, this, intent)
            }
            RecordBoundary.Command.DISCARD_BOUNDARY -> {
                ActionDiscardBoundary(subject, result, this)
            }
            else -> ActionNull(subject, result, this)
        }
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

//            Log.d(TAG, "angle: ${rotation[0].toInt()} movement: ${movement[0]}")
            viewModel.moveRobot(rotation[0].toInt(), movement[0])

        }
    }

}