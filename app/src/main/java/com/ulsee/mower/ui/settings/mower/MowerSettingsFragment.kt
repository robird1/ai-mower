package com.ulsee.mower.ui.settings.mower

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.data.Status
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
        registerBLEReceiver()
        initFetchFailedObserver()
        viewModel.getSettings()
        initModeClick()
        initWorkOnRainDayClick()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MowerSettingsFragmentViewModelFactory(bleRepository)).get(MowerSettingsFragmentViewModel::class.java)
    }

    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(BLEBroadcastAction.ACTION_SETTINGS)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun initSettingsObserver() {
        viewModel.settings.observe(viewLifecycleOwner) {
            binding.switchWorkonrainydays.isChecked = it.isWorkingOnRainlyDay
            binding.radioButtonLearning.isChecked = it.workingMode == MowerWorkingMode.learning
            binding.radioButtonWorking.isChecked = it.workingMode == MowerWorkingMode.working
            binding.radioButtonLearnandwork.isChecked = it.workingMode == MowerWorkingMode.learnAndWork
            binding.radioButtonGradual.isChecked = it.workingMode == MowerWorkingMode.gradual
            binding.radioButtonExplosive.isChecked = it.workingMode == MowerWorkingMode.explosive
        }
    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }

    private fun initSubViewEntry() {
//        binding.layoutWorkonrainydays.setOnClickListener { findNavController().navigate(R.id.settingsWorkOnRainDaysFragment) }
//        binding.layoutUpdatemowersoftware.setOnClickListener { findNavController().navigate(R.id.settingsUpdateSoftwareFragment) }
//        binding.layoutGradual.setOnClickListener { findNavController().navigate(R.id.settingsGradualFragment) }
//        binding.layoutExplosive.setOnClickListener { findNavController().navigate(R.id.settingsExplosiveFragment) }
        binding.layoutBladeheight.setOnClickListener { findNavController().navigate(R.id.settingsBladeHeightFragment) }
    }

    private fun initFetchFailedObserver() {
        viewModel.fetchSettingsFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =================================================
    // ================== working mode ====================
    // =================================================
    private fun initModeClick() {
        val radioButtons = arrayOf(binding.radioButtonLearning,
            binding.radioButtonWorking,
            binding.radioButtonLearnandwork,
            binding.radioButtonGradual,
            binding.radioButtonExplosive)
        val onChangedListener =
            CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                if (!isChecked) return@OnCheckedChangeListener
                for (radioButton in radioButtons) {
                    if(radioButton.isChecked && radioButton != buttonView)radioButton.isChecked = false
                }
                viewModel.updateWorkingMode(getWorkingMode())
            }
        for (radioButton in radioButtons) {
            radioButton.setOnCheckedChangeListener(onChangedListener)
        }
    }

    private fun getWorkingMode() : MowerWorkingMode {
        if(binding.radioButtonLearning.isChecked) return MowerWorkingMode.learning
        if(binding.radioButtonWorking.isChecked) return MowerWorkingMode.working
        if(binding.radioButtonLearnandwork.isChecked) return MowerWorkingMode.learnAndWork
        if(binding.radioButtonGradual.isChecked) return MowerWorkingMode.gradual
        if(binding.radioButtonExplosive.isChecked) return MowerWorkingMode.explosive
        return MowerWorkingMode.learning
    }

    // =================================================
    // ================== rainly day ====================
    // =================================================
    private fun initWorkOnRainDayClick() {
        binding.switchWorkonrainydays.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.updateWorkingOnRainlyDay(isChecked)
        }
    }
}