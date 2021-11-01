package com.ulsee.mower.ui.connect

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.ulsee.mower.App
import com.ulsee.mower.BuildConfig
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.AccountDataSource
import com.ulsee.mower.data.AccountRepository
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_CONNECT_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_DEVICE_NOT_FOUND
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_CONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_DISCONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_NOT_SUCCESS
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_SUCCESS
import com.ulsee.mower.data.DatabaseRepository
import com.ulsee.mower.databinding.FragmentRobotListBinding
import com.ulsee.mower.ui.login.LoginActivity

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
    private var isReceiverRegistered = false

    private var editTextInput: EditText? = null
//    private val args: RobotListFragmentArgs by navArgs()

    var isConnecting = false
    var connectingBeginAt = 0L

    override fun onAttach(context: Context) {
        Log.d(TAG, "[Enter] onAttach")
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        checkService()
        initViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "[Enter] onCreateView")
        binding = FragmentRobotListBinding.inflate(inflater, container, false)
        viewModel.startBLEScan(this)
        viewModel.getDeviceList()
        addOnBackPressedCallback()
        initProgressBar()
        initRecyclerView()
        configAddDeviceBtn()
        registerBLEReceiver()

        val args = RobotListFragmentArgs.fromBundle(requireArguments())
        if (args.isGuideFinished) {
            showAddDeviceDialog()
        }

        return binding.root
    }

    private fun checkService() {
        (requireActivity().application as App).bluetoothService?.let {
            bluetoothService = it

        } ?: run {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onViewCreated")
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "[Enter] onActivityCreated")
        initDeviceListObserver()
        initDeviceNotFoundObserver()
        initConnectFailedObserver()
        initBindFailedObserver()
        initReloadCloudDeivceFailedObserver()
        initRecyclerView()
        initGattStatusObserver()
        initGattNotSuccessObserver()
        initVerificationObserver()
        initInputSnObserver()
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
        if (isReceiverRegistered) {
            requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
            isReceiverRegistered = false
        }

        requireArguments().clear()

        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "[Enter] onDestroy")
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d(TAG, "[Enter] onDetach")
        super.onDetach()
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
                        if (!findNavController().popBackStack()) {
                            requireActivity().finish()
                        }
                    }
                }
            })
    }

    private fun registerBLEReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter()
            filter.addAction(ACTION_CONNECT_FAILED)
            filter.addAction(ACTION_DEVICE_NOT_FOUND)
            filter.addAction(ACTION_GATT_CONNECTED)
            filter.addAction(ACTION_GATT_DISCONNECTED)
            filter.addAction(ACTION_GATT_NOT_SUCCESS)
            filter.addAction(ACTION_VERIFICATION_SUCCESS)
            filter.addAction(ACTION_VERIFICATION_FAILED)
            requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
            isReceiverRegistered = true
        }
    }

    override fun onResume() {
        Log.d(TAG, "[Enter] onResume")
        super.onResume()
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
            findNavController().navigate(R.id.addRobotInstructionFragment)
        }
    }

    private fun showAddDeviceDialog() {
        val view = layoutInflater.inflate(R.layout.input_serial_number_view, null, false)
        editTextInput = view.findViewById(R.id.inputText)
//        editTextInput!!.setText("JCF20210302H0000001")
        editTextInput!!.setText("JCF20210630H0000015")
        val qrCodeIcon = view.findViewById<ImageView>(R.id.qrCodeIcon)
        qrCodeIcon.setOnClickListener {
            initZxingScanner()
        }

        val dialog = AlertDialog.Builder(activity)
        dialog.setTitle("Please enter serial number")
            .setView(view)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val textInput = editTextInput!!.text.toString()
                Log.d(TAG, "textInput: $textInput")
                inputSerialNumber = textInput
                viewModel.isInputDuplicated(textInput)
            }.show()
    }

    private fun initZxingScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan device QRCode")
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    checkQRCode(result.contents)
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkQRCode(qrCode: String) {
        val isValidQRCode = qrCode.startsWith("JCF")
        if(!isValidQRCode) {
            Toast.makeText(context, "QRCode is invalid", Toast.LENGTH_SHORT).show()
            initZxingScanner()
            return
        }
        editTextInput!!.setText(qrCode)
        inputSerialNumber = qrCode
        viewModel.isInputDuplicated(qrCode)
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
            it.getContentIfNotHandled()?.let { msg ->
                val isFound = msg == "true"
//                progressBar.isVisible = false
                if (!isFound) {
                    if (isConnecting) {
                        val now = System.currentTimeMillis()
                        // 10秒內都再次嘗試
                        if (inputSerialNumber != null && now - connectingBeginAt < 10000) {
//                            progressBar.isVisible = true
                            Handler().postDelayed({
                                viewModel.connectBLEDevice(inputSerialNumber!!)
                            }, 1000)
                            return@let
                        } else {

                            AlertDialog.Builder(activity)
                                .setMessage(msg)
                                .setPositiveButton(android.R.string.ok) { it, _ ->
                                    it.dismiss()
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                    progressBar.isVisible = false
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

    private fun initBindFailedObserver() {
        viewModel.bindFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { e ->
                progressBar.isVisible = false
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun initReloadCloudDeivceFailedObserver() {
        viewModel.reloadCloudDeviceFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { e ->
                progressBar.isVisible = false
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initGattStatusObserver() {
//        progressBar.isVisible = false
        viewModel.gattStatusCode.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { statusCode ->
                when(statusCode) {
                    0 -> Toast.makeText(context, "connect successfully", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "connect failed", Toast.LENGTH_SHORT).show()
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
                progressBar.isVisible = false
                if (isSuccess) {
//                    progressBar.isVisible = false
                    Toast.makeText(context, "verification success", Toast.LENGTH_SHORT).show()
                    Log.d("888", "[Enter] verification success")
//                    val action = RobotListFragmentDirections.actionRobotListFragmentToStatusFragment()
//                    findNavController().navigate(action)
                    val bundle = Bundle()
                    bundle.putBoolean("refreshMap", true)
                    findNavController().navigate(R.id.statusFragment, bundle)
                } else {
                    Log.d("888", "[Enter] verification failed")
                    Toast.makeText(context, "verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initInputSnObserver() {
        viewModel.isInputDuplicated.observe(viewLifecycleOwner) { it ->
            it.getContentIfNotHandled()?.let { isDuplicated ->
                if (!isDuplicated) {
                    connectingBeginAt = System.currentTimeMillis()
                    isConnecting = true
                    progressBar.isVisible = true
                    viewModel.connectBLEDevice(inputSerialNumber!!)
                    Log.i(TAG, "!isDuplicated, connectBLEDevice")
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
        val prefs = requireContext().getSharedPreferences("account", Context.MODE_PRIVATE)
        val accountRepository = AccountRepository(AccountDataSource("https://fr.ulsee.club/api/", prefs), prefs)
        viewModel = ViewModelProvider(this, RobotListFactory(bleRepository, DatabaseRepository(), accountRepository)).get(RobotListFragmentViewModel::class.java)
    }

}