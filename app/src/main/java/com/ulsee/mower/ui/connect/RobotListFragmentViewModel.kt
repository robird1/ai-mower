package com.ulsee.mower.ui.connect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_CONNECT_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_DEVICE_NOT_FOUND
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_CONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_DISCONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_NOT_SUCCESS
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_SUCCESS
import com.ulsee.mower.data.DatabaseRepository
import com.ulsee.mower.data.AccountRepository
import com.ulsee.mower.data.model.Device
import com.ulsee.mower.utils.Event
import com.ulsee.mower.utils.MD5
import com.ulsee.mower.utils.Utils
import kotlinx.coroutines.launch


private val TAG = RobotListFragmentViewModel::class.java.simpleName


class RobotListFragmentViewModel(private val bleRepository: BluetoothLeRepository, private val dbRepository: DatabaseRepository, private val accountRepository: AccountRepository) : ViewModel() {

    private var _isDeviceFound : MutableLiveData<Event<Boolean>> = MutableLiveData()
    val isDeviceFound : LiveData<Event<Boolean>>
        get() = _isDeviceFound
    private var _connectFailedLog : MutableLiveData<Event<String>> = MutableLiveData()
    val connectFailedLog : LiveData<Event<String>>
        get() = _connectFailedLog
    private var _isVerificationSuccess : MutableLiveData<Event<Boolean>> = MutableLiveData()
    val isVerificationSuccess : LiveData<Event<Boolean>>
        get() = _isVerificationSuccess
    private var _isInputDuplicated = MutableLiveData<Event<Boolean>>()
    val isInputDuplicated : LiveData<Event<Boolean>>
        get() = _isInputDuplicated

    private var _isScanning = MutableLiveData<Boolean>()
    val isScanning : LiveData<Boolean>
        get() = _isScanning
    private var _gattStatusCode = MutableLiveData<Event<Int>>()
    val gattStatusCode : LiveData<Event<Int>>
        get() = _gattStatusCode
    private var _gattNotSuccess = MutableLiveData<Event<String>>()
    val gattNotSuccess : LiveData<Event<String>>
        get() = _gattNotSuccess

    private var _deviceList = MutableLiveData<List<Device>>()
    val deviceList : LiveData<List<Device>>
        get() = _deviceList

    private var deviceSerialNumber: String? = null

    private var _bindFailedLog : MutableLiveData<Event<Exception>> = MutableLiveData()
    val bindFailedLog : LiveData<Event<Exception>>
        get() = _bindFailedLog
    private var _reloadCloudDeviceFailedLog : MutableLiveData<Event<Exception>> = MutableLiveData()
    val reloadCloudDeviceFailedLog : LiveData<Event<Exception>>
        get() = _reloadCloudDeviceFailedLog

    val isLoading : MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    val gattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val message = intent.getStringExtra("message")
            Log.d(TAG, "action: $action message: $message")
            when (action){
                ACTION_CONNECT_FAILED -> {
                    _connectFailedLog.value = Event(message!!)
                }
                ACTION_DEVICE_NOT_FOUND -> {
                    _isDeviceFound.value = Event(false)
                }
                ACTION_GATT_CONNECTED -> {
                    _gattStatusCode.value = Event(message!!.toInt())
                }
                ACTION_GATT_DISCONNECTED -> {
//                    _gattStatusCode.value = Event(message!!.toInt())
                    // TODO
                }
                ACTION_GATT_NOT_SUCCESS -> {
                    _gattNotSuccess.value = Event(message!!)
                }
                ACTION_VERIFICATION_SUCCESS -> {
                    saveDeviceIfNotExisting()
                    _isVerificationSuccess.value = Event(true)
                }
                ACTION_VERIFICATION_FAILED -> {
                    _isVerificationSuccess.value = Event(false)
                }
            }
        }
    }

    fun startBLEScan(fragment: RobotListFragment) {
        Utils.checkLocationSetting(fragment.requireActivity())

        if (!fragment.isLocationPermissionGranted) {
            fragment.requestLocationPermission()
        }
        else {
//        val filters = arrayListOf(filter)
            if (_isScanning.value == true)
                stopBleScan()

            bleRepository.startBLEScan()
            _isScanning.value = true
        }
    }

    private fun stopBleScan() {
        Log.d(TAG, "[Enter] stopBleScan()")
        bleRepository.stopScan()
        _isScanning.value = false
    }

    fun connectBLEDevice(serialNumber: String) {
        deviceSerialNumber = serialNumber
        bleRepository.connectDevice(serialNumber)
    }

    fun disconnectDevice() {
        bleRepository.disconnectDevice()
    }

    fun saveDevice(serialNumber: String) {

        isLoading.value = true
        viewModelScope.launch {
            // save to api
            val result = accountRepository.bind(serialNumber)
            if (result is com.ulsee.mower.data.Result.Success) {
            } else {
                _bindFailedLog.value = Event((result as com.ulsee.mower.data.Result.Error).exception)
            }
            isLoading.value = false
        }
    }

    private fun saveDeviceIfNotExisting() {
        viewModelScope.launch {
            val isDeviceExisting =_deviceList.value?.firstOrNull {
                it.getSerialNumber() == deviceSerialNumber!!
            } != null
            if (!isDeviceExisting) {
                saveDevice(deviceSerialNumber!!)
            }
        }
    }

    fun isInputDuplicated(sn: String) {
        viewModelScope.launch {
            val isDeviceExisting =_deviceList.value?.firstOrNull { it.getSerialNumber() == sn } != null
            _isInputDuplicated.value = Event(isDeviceExisting)
        }
    }

    fun getDeviceList() {
    isLoading.value = true
        viewModelScope.launch {
            val result = accountRepository.getMe()
            if (result is com.ulsee.mower.data.Result.Success) {
                val devices = result.data.history
                _deviceList.value = devices.map {
                    val device = Device()
                    device.setSerialNumber(it.sn)
                    device.setSnMD5(MD5.convertMD5(it.sn))
                    device
                }
            } else {
                _reloadCloudDeviceFailedLog.value = Event((result as com.ulsee.mower.data.Result.Error).exception)
            }
            isLoading.value = false
        }
    }

}


class RobotListFactory(private val bleRepository: BluetoothLeRepository, private val dbRepository: DatabaseRepository, private val accountRepository: AccountRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RobotListFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RobotListFragmentViewModel(bleRepository, dbRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}