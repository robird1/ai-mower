package com.ulsee.mower

import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.data.*
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_CONNECT_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_DEVICE_NOT_FOUND
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_CONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_DISCONNECTED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_GATT_NOT_SUCCESS
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_FAILED
import com.ulsee.mower.data.BLEBroadcastAction.Companion.ACTION_VERIFICATION_SUCCESS
import com.ulsee.mower.data.model.Device
import com.ulsee.mower.utils.Event
import com.ulsee.mower.utils.MD5
import com.ulsee.mower.utils.Utils
import kotlinx.coroutines.launch


private val TAG = RobotListFragmentViewModel::class.java.simpleName


class RobotListFragmentViewModel(private val bleRepository: BluetoothLeRepository, private val dbRepository: DatabaseRepository) : ViewModel() {

//    val filter = ScanFilter.Builder().setManufacturerData(741, null).build()
    private var scanResults: ArrayList<ScanResult> = ArrayList()
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

    private var _deviceList = MutableLiveData<List<Device>>()
    val deviceList : LiveData<List<Device>>
        get() = _deviceList

    private var deviceSerialNumber: String? = null


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
                    _gattStatusCode.value = Event(message!!.toInt())
                }
                ACTION_GATT_NOT_SUCCESS -> {
                    _connectFailedLog.value = Event(message!!)
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
        viewModelScope.launch {
            val md5 = MD5.convertMD5(serialNumber)
            dbRepository.saveDevice(serialNumber, md5)
        }
    }

    private fun saveDeviceIfNotExisting() {
        viewModelScope.launch {
            val isDeviceExisting = dbRepository.isSerialNumberDuplicated(deviceSerialNumber!!)
            if (!isDeviceExisting) {
                saveDevice(deviceSerialNumber!!)
            }
        }
    }

    fun isInputDuplicated(sn: String) {
        viewModelScope.launch {
            _isInputDuplicated.value = Event(dbRepository.isSerialNumberDuplicated(sn))
        }
    }

    fun getDeviceList() {
        viewModelScope.launch {
            _deviceList.value = dbRepository.getDevices()
        }
    }

}


class RobotListFactory(private val bleRepository: BluetoothLeRepository, private val dbRepository: DatabaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RobotListFragmentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RobotListFragmentViewModel(bleRepository, dbRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}