package com.ulsee.mower.ui.map

import android.app.AlertDialog
import android.content.IntentFilter
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
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.StartStop.Command.Companion.BACK_CHARGING_STATION
import com.ulsee.mower.data.StartStop.Command.Companion.PAUSE_MOWING
import com.ulsee.mower.data.StartStop.Command.Companion.RESUME_EMERGENCY_STOP
import com.ulsee.mower.data.StartStop.Command.Companion.RESUME_FROM_INTERRUPT
import com.ulsee.mower.data.StartStop.Command.Companion.RESUME_MOWING
import com.ulsee.mower.data.StartStop.Command.Companion.START_MOWING
import com.ulsee.mower.data.Status.*
import com.ulsee.mower.data.Status.WorkingMode.Companion.LEARNING_MODE
import com.ulsee.mower.data.Status.WorkingMode.Companion.SUSPEND_WORKING_MODE
import com.ulsee.mower.data.Status.WorkingMode.Companion.WORKING_MODE
import com.ulsee.mower.databinding.ActivityStatusBinding

private val TAG = StatusFragment::class.java.simpleName

class StatusFragment: Fragment() {
    private lateinit var binding: ActivityStatusBinding
    lateinit var viewModel: StatusFragmentViewModel
    lateinit var bluetoothService: BluetoothLeService
    private var isReceiverRegistered = false
    private var workingErrorCode = -1
    private val emergencyStopIdxList = ArrayList<Int>()
    private val interruptionIdxList = ArrayList<Int>()
    private var signalQuality = -1

    private var state = MowingState.Stop
    enum class MowingState {
        Mowing, Pause, Stop
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")

        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!

        initViewModel()

//        registerBLEReceiver()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "[Enter] onCreateView")

        binding = ActivityStatusBinding.inflate(inflater, container, false)

        GestureViewBinder.bind(requireContext(), binding.rootLayout, binding.statusView);
//        GestureViewBinder.setFullGroup(true)

        addOnBackPressedCallback()

        initTestButtonListener()
        initSetupButtonListener()
        initParkingButtonListener()
        initStartButtonListener()
        initPauseButtonListener()
        initScheduleButton()
        initSettingButton()

        registerBLEReceiver()

        viewModel.getStatusPeriodically()

        binding.progressView.isVisible = true
        viewModel.getMapGlobalParameters()

        return binding.root
    }

    private fun initTestButtonListener() {
        binding.testButton.setOnClickListener {
            findNavController().navigate(R.id.setupMapFragment)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        initPowerObserver()
        initStatusObserver()
        initStartStopObserver()
        initMapDataObserver()
        initGlobalParameterObserver()
        initMowingDataObserver()
    }

    private fun initMowingDataObserver() {
        viewModel.mowingDataList.observe(viewLifecycleOwner) {
            binding.statusView.updateMowingArea(it)
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView")
        if (isReceiverRegistered) {
            requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
            isReceiverRegistered = false
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
//        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
        super.onDestroy()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, StatusFragmentFactory(
            BluetoothLeRepository(bluetoothService))).get(StatusFragmentViewModel::class.java)
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.progressView.isVisible) {
                        binding.progressView.isVisible = false
                        return
                    }

                    val dialog = AlertDialog.Builder(context)
                    dialog.setMessage("Disconnect and return to device list page ?")
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_confirm) { _, _ ->

                            viewModel.disconnectDevice()
                            findNavController().popBackStack()

                        }
                        .setNegativeButton("cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            })
    }

    private fun registerBLEReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter()
            filter.addAction(BLEBroadcastAction.ACTION_STATUS)
            filter.addAction(BLEBroadcastAction.ACTION_START_STOP)
            filter.addAction(BLEBroadcastAction.ACTION_GLOBAL_PARAMETER)
            filter.addAction(BLEBroadcastAction.ACTION_GRASS_BOARDER)
            filter.addAction(BLEBroadcastAction.ACTION_OBSTACLE_BOARDER)
            filter.addAction(BLEBroadcastAction.ACTION_GRASS_PATH)
            filter.addAction(BLEBroadcastAction.ACTION_CHARGING_PATH)
            filter.addAction(BLEBroadcastAction.ACTION_MOWING_DATA)
            requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)

            isReceiverRegistered = true
        }
    }

    private fun initMapDataObserver() {
        viewModel.hasMapData.observe(viewLifecycleOwner) { result->
            if (result) {
                binding.constraintLayoutNoMap.isVisible = false
                binding.constraintLayoutCustomView.isVisible = true

            } else {
                binding.progressView.isVisible = false

                binding.constraintLayoutNoMap.isVisible = true
                binding.constraintLayoutCustomView.isVisible = false
            }
        }
    }

    private fun initGlobalParameterObserver() {
        viewModel.requestMapFinished.observe(viewLifecycleOwner) { isFinished ->
            Log.d("777", "[Enter] requestMapFinished.observe()")
            binding.progressView.isVisible = false
            if (isFinished) {
                binding.statusView.initData()
            }
        }
    }

    private fun initStartStopObserver() {
        viewModel.startStopResult.observe(viewLifecycleOwner) { (isSuccess, type) ->
            if (isSuccess) {
                when (type.toInt()) {
                    START_MOWING -> {
                        Log.d("678", "startStopResult.observe() START_MOWING")
                        binding.startMowingBtn.isVisible = false
                        binding.startText.isVisible = false
                        binding.pauseButton.isVisible = true
                        binding.pauseText.isVisible = true
                        state = MowingState.Mowing

                        viewModel.getMowingData(0x00)
                    }
                    PAUSE_MOWING -> {
                        Log.d("678", "startStopResult.observe() PAUSE_MOWING")
                        binding.pauseText.text = "Resume"
                        state = MowingState.Pause
                    }
                    RESUME_MOWING -> {
                        Log.d("678", "startStopResult.observe() RESUME_MOWING")
                        binding.pauseText.text = "Pause"
                        state = MowingState.Mowing

                        viewModel.getMowingData(0x00)
                    }
                    BACK_CHARGING_STATION -> {
                        Log.d("678", "startStopResult.observe() BACK_CHARGING_STATION")
                        binding.startMowingBtn.isVisible = true
                        binding.startText.isVisible = true
                        binding.pauseButton.isVisible = false
                        binding.pauseText.isVisible = false
                        state = MowingState.Stop
                    }
                }
            } else {
                Toast.makeText(context, "$type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initStatusObserver() {
        viewModel.statusIntent.observe(viewLifecycleOwner) { intent ->
            val x = intent.getIntExtra("x", 0)
            val y = intent.getIntExtra("y", 0)
            val angle = intent.getFloatExtra("angle", 0F)
            val power = intent.getIntExtra("power", -1)
            val workingMode = intent.getIntExtra("working_mode", -1)
            val errorCode = intent.getIntExtra("working_error_code", -1)
            val robotStatus = intent.getStringExtra("robot_status") ?: ""
//            val robotPositionStatus = intent.getStringExtra("robot_position_status") ?: ""
            val interruptionCode = intent.getStringExtra("interruption_code") ?: ""
            signalQuality = intent.getIntExtra("signal_quality", -1)

//            Log.d("888", "robotStatus: $robotStatus")

            binding.powerPercentage.text = "$power %"

            checkSatelliteSignal()

            binding.statusView.notifyRobotCoordinate(x, y, angle)

            checkWorkingErrorCode(errorCode)
            checkRobotStatus(robotStatus)
//            checkRobotPositionStatus(robotPositionStatus)
            checkInterruptionCode(interruptionCode)
            checkWorkingMode(workingMode)
        }
    }

    private fun checkSatelliteSignal() {
        when (signalQuality) {
            1 -> {
                binding.signalStatus.text = "Available"
                binding.signalView.isVisible = true
            }
            0 -> {
                binding.signalStatus.text = "Unavailable"
                binding.signalView.isVisible = false
            }
        }
    }

    private fun checkWorkingMode(workingMode: Int) {
        Log.d("678", "workingMode: $workingMode")
        when (workingMode) {
            WORKING_MODE, LEARNING_MODE -> {
                Log.d("678", "[Enter] WORKING_MODE")

                binding.startMowingBtn.isVisible = false
                binding.startText.isVisible = false
                binding.pauseButton.isVisible = true
                binding.pauseText.isVisible = true
                state = MowingState.Mowing
            }
            SUSPEND_WORKING_MODE -> {
                Log.d("678", "[Enter] SUSPEND_WORKING_MODE")

                binding.startMowingBtn.isVisible = false
                binding.startText.isVisible = false
                binding.pauseButton.isVisible = true
                binding.pauseText.isVisible = true
                binding.pauseText.text = "Resume"
                state = MowingState.Pause
            }
            else -> {

            }
        }
    }

    private fun checkInterruptionCode(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !interruptionIdxList.contains(idx)) {
                val message = Interruption.map[idx] ?: "unknown error"
                showInterruptionDialog(message, idx)
                interruptionIdxList.add(idx)
            }
        }
    }

    private fun checkRobotStatus(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !emergencyStopIdxList.contains(idx)) {
                val message = RobotStatus.map[idx] ?: "unknown error"
                showEmergencyStopDialog(message, idx)
                emergencyStopIdxList.add(idx)
            }
        }
    }

    private fun checkWorkingErrorCode(code: Int) {
        if (code > 0 && code != workingErrorCode) {
            Log.d("888", "errorCode: $code")
            val message = WorkingErrorCode.map[code] ?: "errorCode: $code"
            showWorkingErrorDialog(message)
            workingErrorCode = code
        }
    }

    private fun initSetupButtonListener() {
        binding.setupButton.setOnClickListener {
            findNavController().navigate(R.id.setupMapFragment)
        }
    }

    private fun initSettingButton() {
        binding.settingButton.setOnClickListener {
            // TODO
        }
    }

    private fun initScheduleButton() {
        binding.scheduleButton.setOnClickListener {
            // TODO
        }
    }

    private fun initStartButtonListener() {
        binding.startMowingBtn.setOnClickListener {
            Log.d("777", "state: $state")
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
//            when (state) {
//                MowingState.Stop -> {
                    Log.d("777", "[Enter] START_MOWING")
                    viewModel.startStop(START_MOWING)
//                }
//            }
        }
    }

    private fun initPauseButtonListener() {
        binding.pauseButton.setOnClickListener {
            Log.d("777", "state: $state")
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (state) {
                MowingState.Mowing -> {
                    Log.d("777", "[Enter] PAUSE_MOWING")
                    viewModel.startStop(PAUSE_MOWING)
                }
                MowingState.Pause -> {
                    Log.d("777", "[Enter] RESUME_MOWING")

                    viewModel.startStop(RESUME_MOWING)
                }
            }
        }
    }

    private fun initParkingButtonListener() {
        binding.parkingButton.setOnClickListener {
            Log.d("777", "state: $state")
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (state) {
                MowingState.Pause -> {
                    viewModel.startStop(BACK_CHARGING_STATION)
                    viewModel.startStop(RESUME_MOWING)

                    Log.d("777", "[Enter] BACK_CHARGING_STATION")
                    Log.d("777", "[Enter] RESUME_MOWING")

                }
                MowingState.Mowing -> {
                    viewModel.startStop(BACK_CHARGING_STATION)
                    Log.d("777", "[Enter] BACK_CHARGING_STATION")

                }
                MowingState.Stop -> {
                    // do nothing
                }
            }

        }
    }

    private fun showWorkingErrorDialog(message: String) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("ok") { it, _ ->
                it.dismiss()
                workingErrorCode = -1
            }
            .create()
        dialog.show()
    }

    private fun showEmergencyStopDialog(message: String, bitIndex: Int) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Reset") { it, _ ->
                viewModel.startStop(RESUME_EMERGENCY_STOP)
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
                viewModel.startStop(RESUME_FROM_INTERRUPT)
                interruptionIdxList.remove(bitIndex)
                it.dismiss()
            }
            .create()
        dialog.show()
    }

    //    private fun checkRobotPositionStatus(code: String) {
//        code.forEachIndexed { idx, value ->
//            when (idx) {
//                20 -> {    // 组合导航状态
//                    if (value.toInt() == 1) {
//
//                    } else {
//
//                    }
//                }
//                23 -> {    // 定位状态
//                    if (value.toInt() == 1) {
//
//                    } else {
//
//                    }
//                }
//            }
//        }
//    }

}