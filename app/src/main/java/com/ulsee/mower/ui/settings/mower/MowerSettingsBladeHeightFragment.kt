package com.ulsee.mower.ui.settings.mower

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.FragmentSettingsMowerBladeheightBinding


private val TAG = MowerSettingsBladeHeightFragment::class.java.simpleName

class MowerSettingsBladeHeightFragment: Fragment() {

    private lateinit var binding: FragmentSettingsMowerBladeheightBinding
    private lateinit var viewModel: MowerSettingsBladeHeightFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentSettingsMowerBladeheightBinding.inflate(inflater, container, false)

        initViewModel()

        initSettingsObserver()
        initLoadingStatusObserver()
        viewModel.getSettings()

        // init ui
        val height = resources.displayMetrics.heightPixels - 64
        val width = resources.displayMetrics.widthPixels

        val maxViewLayoutParams = binding.textViewMax.layoutParams as ConstraintLayout.LayoutParams
        maxViewLayoutParams.topMargin = (height - width) / 2

        val minViewLayoutParams = binding.textViewMin.layoutParams as ConstraintLayout.LayoutParams
        minViewLayoutParams.bottomMargin = (height - width) / 2 + (width * 20 / 125)

        val valueViewLayoutParams = binding.textViewValue.layoutParams as ConstraintLayout.LayoutParams
        valueViewLayoutParams.topMargin = (height - width) / 2
        // seekbar mini 20
        binding.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress < 20) {
                    binding.seekbar.progress = 20
                }
                binding.textViewValue.text = "${binding.seekbar.progress}mm"
                valueViewLayoutParams.topMargin = (height - width) / 2 + (width * (125.0 - binding.seekbar.progress) / 125.0 * 0.93).toInt() // 乘上0.93偏差才不會太多，懶得計算了
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MowerSettingsBladeHeightFragmentViewModelFactory(bleRepository)).get(MowerSettingsBladeHeightFragmentViewModel::class.java)
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

