package com.ulsee.mower.ui.settings.mower

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.databinding.FragmentSettingsMowerUpdatemowersoftwareBinding

private val TAG = MowerSettingsUpdateSoftwareFragment::class.java.simpleName

class MowerSettingsUpdateSoftwareFragment: Fragment() {

    private lateinit var binding: FragmentSettingsMowerUpdatemowersoftwareBinding
    private lateinit var viewModel: MowerSettingsUpdateSoftwareFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentSettingsMowerUpdatemowersoftwareBinding.inflate(inflater, container, false)

        initViewModel()

        initSettingsObserver()
        initLoadingStatusObserver()
        viewModel.getSettings()

        // init ui
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MowerSettingsUpdateSoftwareFragmentViewModelFactory(bleRepository)).get(MowerSettingsUpdateSoftwareFragmentViewModel::class.java)
    }

    private fun initSettingsObserver() {
        viewModel.settings.observe(viewLifecycleOwner) {
            // todo: update ui
        }
    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }
}

