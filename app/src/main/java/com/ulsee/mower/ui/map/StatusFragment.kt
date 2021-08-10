package com.ulsee.mower.ui.map

import android.app.AlertDialog
import android.content.Intent
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
import com.ulsee.mower.data.Status.WorkingMode.Companion.MANUAL_MODE
import com.ulsee.mower.data.Status.WorkingMode.Companion.SUSPEND_WORKING_MODE
import com.ulsee.mower.data.Status.WorkingMode.Companion.TESTING_BOUNDARY_MODE
import com.ulsee.mower.data.Status.WorkingMode.Companion.WORKING_MODE
import com.ulsee.mower.databinding.ActivityStatusBinding

private val TAG = StatusFragment::class.java.simpleName

class StatusFragment: Fragment() {
    private lateinit var binding: ActivityStatusBinding
    lateinit var viewModel: StatusFragmentViewModel
    lateinit var bluetoothService: BluetoothLeService
    private var isReceiverRegistered = false
    private var signalQuality = -1
    private var isMowingStatus = false
    private var workingMode = MANUAL_MODE

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
        initStatusObserver()
        initStartStopObserver()
        initMapDataObserver()
        initGlobalParameterObserver()
        initMowingDataObserver()
        initGattConnectedStatus()
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
        viewModel = ViewModelProvider(requireActivity(), StatusFragmentFactory(
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
            filter.addAction(BLEBroadcastAction.ACTION_GATT_CONNECTED)
            filter.addAction(BLEBroadcastAction.ACTION_GATT_NOT_SUCCESS)
            requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)

            isReceiverRegistered = true
        }
    }

    private fun initMapDataObserver() {
        viewModel.hasMapData.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { result ->
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
    }

    private fun initGlobalParameterObserver() {
        viewModel.requestMapFinished.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { isFinished ->
                binding.progressView.isVisible = false
                if (isFinished) {
                    binding.statusView.initData()
                }
            }
        }
    }

    private fun initMowingDataObserver() {
        viewModel.mowingDataList.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { list ->
//                Log.d("666", "[Enter] updateMowingArea() size: ${it.size} isMowingStatus: $isMowingStatus")

                // 刀盤啟動中或作業暫停中
                if (isMowingStatus || workingMode == SUSPEND_WORKING_MODE) {
                    binding.statusView.updateMowingArea(list)
                }
            }
        }
    }

    private fun initGattConnectedStatus() {
        viewModel.gattConnected.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { isConnected ->
                if (isConnected) {
                    binding.connectedView.isVisible = true
                    binding.disconnectedView.isVisible = false
                    binding.connectStausText.text = "Connected"
                } else {
                    binding.connectedView.isVisible = false
                    binding.disconnectedView.isVisible = true
                    binding.connectStausText.text = "Disconnected"
                }
            }
        }
    }

    private fun initStartStopObserver() {
        viewModel.startStopResult.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { pair ->
                val isSuccess = pair.first
                val command = pair.second
                if (isSuccess) {
                    if (command == "0") {    // 開始作業
                        Log.d("222", "workingErrorCodeList.clear()")
                        viewModel.workingErrorCodeList.clear()
                    }
                }
            }
        }
    }

    private fun initStatusObserver() {
        viewModel.statusIntent.observe(viewLifecycleOwner) {
            Log.d("222", "workingErrorCodeList.size: ${viewModel.workingErrorCodeList.size}")
            it.getContentIfNotHandled()?.let { intent ->
                val x = intent.getIntExtra("x", 0)
                val y = intent.getIntExtra("y", 0)
                val angle = intent.getFloatExtra("angle", 0F)
                val power = intent.getIntExtra("power", -1)
                workingMode = intent.getIntExtra("working_mode", -1)
                val errorCode = intent.getIntExtra("working_error_code", -1)
                val robotStatus = intent.getStringExtra("robot_status") ?: ""
                val isCharging = intent.getBooleanExtra("charging_status", false)
                isMowingStatus = intent.getBooleanExtra("mowing_status", false)
                val interruptionCode = intent.getStringExtra("interruption_code") ?: ""
                signalQuality = intent.getIntExtra("signal_quality", -1)

                setWorkingAreaText(intent)
                setWorkingTime(intent)
                setPowerPercentage(power)
                setChargingText(isCharging)
                checkSatelliteSignal()
                binding.statusView.notifyRobotCoordinate(x, y, angle)
                checkWorkingErrorCode(errorCode)
                checkRobotStatus(robotStatus)
                checkInterruptionCode(interruptionCode)
                checkWorkingMode(workingMode)
            }
        }
    }

    private fun setPowerPercentage(power: Int) {
        binding.powerPercentage.text = "$power %"
    }

    private fun setWorkingTime(intent: Intent) {
        val estimatedTime = intent.getStringExtra("estimated_time")
        val elapsedTime = intent.getStringExtra("elapsed_time")
        binding.workingTimeText.text = "$elapsedTime / $estimatedTime"
    }

    private fun setWorkingAreaText(intent: Intent) {
        val totalArea = intent.getShortExtra("total_area", -1)
        val finishedArea = intent.getShortExtra("finished_area", -1)
        binding.workingAreaText.text = "$finishedArea㎡ / $totalArea㎡"
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
        when (workingMode) {
            WORKING_MODE, LEARNING_MODE -> {
                Log.d("777", "[Enter] WORKING_MODE, LEARNING_MODE")
                binding.startMowingBtn.isVisible = false
                binding.startText.isVisible = false
                binding.pauseButton.isVisible = true
                binding.pauseText.isVisible = true
                binding.pauseText.text = "Pause"
                state = MowingState.Mowing

                viewModel.getMowingData(0x00)

            }
            SUSPEND_WORKING_MODE -> {
                Log.d("777", "[Enter] SUSPEND_WORKING_MODE")

                binding.startMowingBtn.isVisible = false
                binding.startText.isVisible = false
                binding.pauseButton.isVisible = true
                binding.pauseText.isVisible = true
                binding.pauseText.text = "Resume"
                state = MowingState.Pause

//                viewModel.cancelGetMowingData()
                viewModel.getMowingData(0x00)

            }
            MANUAL_MODE -> {
                Log.d("777", "[Enter] MANUAL_MODE")

                binding.startMowingBtn.isVisible = true
                binding.startText.isVisible = true
                binding.pauseButton.isVisible = false
                binding.pauseText.isVisible = false
                state = MowingState.Stop

                viewModel.cancelGetMowingData()
            }
            TESTING_BOUNDARY_MODE -> {
                Log.d("777", "[Enter] TESTING_BOUNDARY_MODE")

            }
        }
    }

    private fun checkInterruptionCode(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !viewModel.interruptionIdxList.contains(idx)) {
//                val message = Interruption.map[idx] ?: "unknown error"
                val message = Interruption.map[idx]
                if (message != null) {
                    showInterruptionDialog(message, idx)
                    viewModel.interruptionIdxList.add(idx)
                }
            }
        }
    }

    private fun checkRobotStatus(code: String) {
        code.forEachIndexed { idx, value ->
            if (value == '1' && !viewModel.emergencyStopIdxList.contains(idx)) {
                val message = RobotStatus.map[idx] ?: "unknown error"
                showEmergencyStopDialog(message, idx)
                viewModel.emergencyStopIdxList.add(idx)
            }
        }
    }

    private fun checkWorkingErrorCode(code: Int) {
        if (code > 0 && !viewModel.workingErrorCodeList.contains(code)) {
            val message = WorkingErrorCode.map[code] ?: "errorCode: $code"
            showWorkingErrorDialog(message)
            viewModel.workingErrorCodeList.add(code)
        }
    }

    private fun initSetupButtonListener() {
        binding.setupButton.setOnClickListener {
            findNavController().navigate(R.id.instruction0Fragment)
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
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startStop(START_MOWING)
        }
    }

    private fun initPauseButtonListener() {
        binding.pauseButton.setOnClickListener {
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (state) {
                MowingState.Mowing -> {
                    viewModel.startStop(PAUSE_MOWING)
                }
                MowingState.Pause -> {
                    viewModel.startStop(RESUME_MOWING)
                }
            }
        }
    }

    private fun initParkingButtonListener() {
        binding.parkingButton.setOnClickListener {
            if (signalQuality == 0) {
                Toast.makeText(context, "Weak satellite signal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (state) {
                MowingState.Pause -> {
                    viewModel.startStop(BACK_CHARGING_STATION)
                    viewModel.startStop(RESUME_MOWING)
                }
                MowingState.Mowing -> {
                    viewModel.startStop(BACK_CHARGING_STATION)
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
                viewModel.emergencyStopIdxList.remove(bitIndex)
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
                viewModel.interruptionIdxList.remove(bitIndex)
                it.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun setChargingText(isCharging: Boolean) {
        binding.chargingTxt.isVisible = isCharging
    }

}