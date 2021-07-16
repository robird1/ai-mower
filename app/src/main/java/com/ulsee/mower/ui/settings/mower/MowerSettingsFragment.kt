package com.ulsee.mower.ui.settings.mower

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.FragmentSettingsBinding
import com.ulsee.mower.databinding.FragmentSettingsMowerBinding

private val TAG = MowerSettingsFragment::class.java.simpleName

class MowerSettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsMowerBinding
    private lateinit var viewModel: MowerSettingsFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentSettingsMowerBinding.inflate(inflater, container, false)

        initViewModel()

        initSettingsObserver()
        initLoadingStatusObserver()
        initSubViewEntry()
        viewModel.getSettings()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MowerSettingsFragmentViewModelFactory(bleRepository)).get(MowerSettingsFragmentViewModel::class.java)
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

    private fun initSubViewEntry() {
        binding.layoutWorkonrainydays.setOnClickListener { findNavController().navigate(R.id.settingsWorkOnRainDaysFragment) }
        binding.layoutUpdatemowersoftware.setOnClickListener { findNavController().navigate(R.id.settingsUpdateSoftwareFragment) }
        binding.layoutGradual.setOnClickListener { findNavController().navigate(R.id.settingsGradualFragment) }
        binding.layoutExplosive.setOnClickListener { findNavController().navigate(R.id.settingsExplosiveFragment) }
        binding.layoutBladeheight.setOnClickListener { findNavController().navigate(R.id.settingsBladeHeightFragment) }
    }
}