package com.ulsee.mower.ui.connect

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ulsee.mower.App
import com.ulsee.mower.BuildConfig
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_CONNECT_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_DEVICE_NOT_FOUND
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_CONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_DISCONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_NOT_SUCCESS
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_SUCCESS
import com.ulsee.mower.data.DatabaseRepository
import com.ulsee.mower.databinding.FragmentRobotListBinding

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

private val TAG = RobotListFragment::class.java.simpleName

class RobotListFragment: Fragment() {
    private lateinit var binding: FragmentRobotListBinding
    private lateinit var progressBar: ConstraintLayout
    private lateinit var viewModel: RobotListFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository
    val isLocationPermissionGranted
        get() = requireActivity().hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private var inputSerialNumber: String? = null


    override fun onAttach(context: Context) {
        Log.d(TAG, "[Enter] onAttach")
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!

        initViewModel()

//        registerBLEReceiver()
//        registerGuideFinishReceiver()

//        (activity as MainActivity).registerServiceCallback(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "[Enter] onActivityCreated")
        initGuideFinishObserver()
    }

    override fun onStart() {
        Log.d(TAG, "[Enter] onStart")
        super.onStart()
    }

    override fun onPause() {
        Log.d(TAG, "[Enter] onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "[Enter] onStop")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "[Enter] onDestroyView")
        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
        requireActivity().unregisterReceiver(viewModel.guideFinishReceiver)
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "[Enter] onDestroy")
//        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
//        requireActivity().unregisterReceiver(viewModel.guideFinishReceiver)
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d(TAG, "[Enter] onDetach")
        super.onDetach()
    }

//    override fun onServiceConnected(service: BluetoothLeService) {
//        bluetoothService = service
//
//        bleRepository.setBleService(bluetoothService!!)
//
//        viewModel.startBLEScan(this@RobotListFragment)
//
//        if (!bluetoothService!!.bluetoothAdapter.isEnabled) {
//            promptEnableBluetooth()
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentRobotListBinding.inflate(inflater, container, false)

//        initViewModel()

        viewModel.startBLEScan(this)
        viewModel.getDeviceList()

        addOnBackPressedCallback()

        initProgressBar()
        initRecyclerView()
        initDeviceListObserver()
        initDeviceNotFoundObserver()
        initConnectFailedObserver()
        initGattStatusObserver()
        initGattNotSuccessObserver()
        initVerificationObserver()
        initInputSnObserver()
        configAddDeviceBtn()

        registerBLEReceiver()
        registerGuideFinishReceiver()

        Log.d(TAG, "isLocationPermissionGranted: $isLocationPermissionGranted")
//        if (!bluetoothService!!.bluetoothAdapter.isEnabled) {
//            promptEnableBluetooth()
//        }

        return binding.root
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.progressView2.isVisible) {
                        binding.progressView2.isVisible = false
                        bleRepository.disconnectDevice()
                    } else {
                        findNavController().popBackStack()
                    }
                }
            })
    }


    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_CONNECT_FAILED)
        filter.addAction(ACTION_DEVICE_NOT_FOUND)
        filter.addAction(ACTION_GATT_CONNECTED)
        filter.addAction(ACTION_GATT_DISCONNECTED)
        filter.addAction(ACTION_GATT_NOT_SUCCESS)
        filter.addAction(ACTION_VERIFICATION_SUCCESS)
        filter.addAction(ACTION_VERIFICATION_FAILED)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun registerGuideFinishReceiver() {
        val filter = IntentFilter()
        filter.addAction("FINISH_ADD_INSTRUCTION")
        requireActivity().registerReceiver(viewModel.guideFinishReceiver, filter)
    }

    override fun onResume() {
        Log.d(TAG, "[Enter] onResume")
        super.onResume()
//        if (!viewModel.bluetoothAdapter.isEnabled) {
//            promptEnableBluetooth()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray) {
        Log.d(TAG, "[Enter] onRequestPermissionsResult")

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // now, you have permission go ahead
            Log.d(TAG, "[Enter] grantResults[0] == PackageManager.PERMISSION_GRANTED")
            viewModel.startBLEScan(this)

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permissions[0]!!)) {
                Log.d(TAG, "[Enter] now, user has denied permission (but not permanently!)")
//                requestPermission(activity!!, permissions[0]!!)
                requestLocationPermission()
            } else {
                Log.d(TAG, "[Enter] now, user has denied permission permanently!")
                showPermissionIsNecessary(requireActivity())
            }
        }
        return
    }

    private fun initRecyclerView() {
        binding.recyclerview.adapter = RobotListAdapter(viewModel, progressBar)
        binding.recyclerview.layoutManager = LinearLayoutManager(context)
    }

    private fun configAddDeviceBtn() {
        binding.button.setOnClickListener {
//            val appPreference = AppPreference(PreferenceManager.getDefaultSharedPreferences(activity))
//            val isFirstAddDevice = appPreference.getFirstAddDevice()
//            if (isFirstAddDevice) {
                findNavController().navigate(R.id.addRobotInstructionFragment)
//            } else {
//                showAddDeviceDialog()
//            }
        }
    }

    private fun showAddDeviceDialog() {
        val input = EditText(context)
        input.setText("JCF20210302H0000001")
        val dialog = AlertDialog.Builder(activity)
        dialog.setTitle("Please enter serial number")
            .setView(input)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _, _ ->

                val textInput = input.text.toString()
                Log.d(TAG, "textInput: $textInput")

                inputSerialNumber = textInput

                viewModel.isInputDuplicated(textInput)

            }.show()
    }

    private fun initDeviceListObserver() {
        viewModel.deviceList.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.textNoRobotHere.isVisible = false
            }
            (binding.recyclerview.adapter as RobotListAdapter).setList(it)
        }
    }

    private fun initDeviceNotFoundObserver() {
        viewModel.isDeviceFound.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { isFound ->
                progressBar.isVisible = false
                if (!isFound) {
                    Toast.makeText(context, "error: Device not found", Toast.LENGTH_SHORT).show()

                    // restart the scanning if it has been stopped
                    viewModel.startBLEScan(this)
                }
            }
        }
    }

    private fun initConnectFailedObserver() {
        viewModel.connectFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { msg ->
                progressBar.isVisible = false
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initGattStatusObserver() {
//        progressBar.isVisible = false
        viewModel.gattStatusCode.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { statusCode ->
                when(statusCode) {
                    0 -> Toast.makeText(context, "BluetoothGatt.GATT_SUCCESS", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "status != BluetoothGatt.GATT_SUCCESS", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initGattNotSuccessObserver() {
        viewModel.gattNotSuccess.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initVerificationObserver() {
        viewModel.isVerificationSuccess.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { isSuccess ->
                if (isSuccess) {
                    progressBar.isVisible = false
                    Toast.makeText(context, "verification success", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.statusFragment)
                } else {
                    Toast.makeText(context, "verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initInputSnObserver() {
        viewModel.isInputDuplicated.observe(viewLifecycleOwner) { it ->
            it.getContentIfNotHandled()?.let { isDuplicated ->
                if (!isDuplicated) {
                    progressBar.isVisible = true
                    viewModel.connectBLEDevice(inputSerialNumber!!)

                } else {
                    AlertDialog.Builder(activity)
                        .setMessage("Device has already been added before!")
                        .setPositiveButton(android.R.string.ok) { it, _ ->
                            it.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    private fun initGuideFinishObserver() {
        viewModel.isGuideFinish.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { result ->
                if (result) {
                    showAddDeviceDialog()
                }
            }
        }
    }

    private fun initProgressBar() {
        progressBar = binding.progressView2
    }

    private fun showPermissionIsNecessary(activity: Activity) {
        val snackbar = Snackbar.make(
                activity.findViewById(android.R.id.content),
                """You have previously declined this permission. You must approve this permission in "Permissions" in the app settings on your device.""",
                Snackbar.LENGTH_LONG
        ).setAction("Settings") {
            activity.startActivity(
                    Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    )
            )
        }
        snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines =
                5 //Or as much as you need
        snackbar.show()
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothService.bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
//        activity?.runOnUiThread {
            val dialog = AlertDialog.Builder(activity)
            dialog.setTitle("Location permission required")
                .setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices.")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }.show()
//            alert {
//                title = "Location permission required"
//                message = "Starting from Android M (6.0), the system requires apps to be granted " +
//                        "location access in order to scan for BLE devices."
//                isCancelable = false
//                positiveButton(android.R.string.ok) {
//                    requestPermission(
//                            Manifest.permission.ACCESS_FINE_LOCATION,
//                            LOCATION_PERMISSION_REQUEST_CODE
//                    )
//                }
//            }.show()
//        }
    }

    fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, RobotListFactory(bleRepository, DatabaseRepository())).get(RobotListFragmentViewModel::class.java)
    }

}