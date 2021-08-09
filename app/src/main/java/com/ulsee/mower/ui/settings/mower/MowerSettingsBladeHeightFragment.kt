package com.ulsee.mower.ui.settings.mower

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.databinding.FragmentSettingsMowerBladeheightBinding
import java.util.*


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
        registerBLEReceiver()

        initSettingsObserver()
        initLoadingStatusObserver()
        initFetchFailedObserver()
        viewModel.getSettings()

        // init ui
        val height = resources.displayMetrics.heightPixels - 64
        val width = resources.displayMetrics.widthPixels

        val maxViewLayoutParams = binding.textViewMax.layoutParams as ConstraintLayout.LayoutParams
        maxViewLayoutParams.topMargin = (height - width) / 2

        val minViewLayoutParams = binding.textViewMin.layoutParams as ConstraintLayout.LayoutParams
        minViewLayoutParams.bottomMargin = (height - width) / 2 + (width * 20 / 70)

        val valueViewLayoutParams = binding.textViewValue.layoutParams as ConstraintLayout.LayoutParams
        valueViewLayoutParams.topMargin = (height - width) / 2
        // seekbar mini 20
        binding.seekbar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            // user change or program change
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                Log.i(TAG, "onProgressChanged ${binding.seekbar.progress}")
                if (progress < 20) {
                    binding.seekbar.progress = 20
                }
                binding.textViewValue.text = "${binding.seekbar.progress}mm"
                valueViewLayoutParams.topMargin = (height - width) / 2 + (width * (70.0 - binding.seekbar.progress) / 70.0 * 0.93).toInt() // 乘上0.93偏差才不會太多，懶得計算了
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            // user change
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                Log.i(TAG, "onStopTrackingTouch ${binding.seekbar.progress}")
                val progress = binding.seekbar.progress
                if (progress < 20) {
                    binding.seekbar.progress = 20
                }
                binding.textViewValue.text = "${binding.seekbar.progress}mm"
                valueViewLayoutParams.topMargin = (height - width) / 2 + (width * (70.0 - binding.seekbar.progress) / 70.0 * 0.93).toInt() // 乘上0.93偏差才不會太多，懶得計算了
                delayUpdateSettings()
            }
        })

        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, MowerSettingsBladeHeightFragmentViewModelFactory(bleRepository)).get(MowerSettingsBladeHeightFragmentViewModel::class.java)
    }


    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(BLEBroadcastAction.ACTION_SETTINGS)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun initSettingsObserver() {
        viewModel.settings.observe(viewLifecycleOwner) {
            binding.seekbar.progress = it.knifeHeight
            // copy from  onCreateView.setOnSeekBarChangeListener
            val height = resources.displayMetrics.heightPixels - 64
            val width = resources.displayMetrics.widthPixels
            binding.textViewValue.text = "${binding.seekbar.progress}mm"
            val valueViewLayoutParams = binding.textViewValue.layoutParams as ConstraintLayout.LayoutParams
            valueViewLayoutParams.topMargin = (height - width) / 2 + (width * (70.0 - binding.seekbar.progress) / 70.0 * 0.93).toInt()
        }
    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }

    var timer: Timer? = null
    var lastUpdatedAt = 0L
    val minUpdateInterval = 1000L
    private fun delayUpdateSettings() {
        if (timer != null) return

        val now = System.currentTimeMillis()
        val diff = now - lastUpdatedAt
        if (diff < minUpdateInterval) {
            timer = Timer()
            timer?.schedule(object: TimerTask(){
                override fun run() {
                    activity?.runOnUiThread {
                        viewModel.updateKnifeHeihgt(binding.seekbar.progress)
                    }
                    timer?.cancel()
                    timer = null
                }
            },minUpdateInterval - diff)
            return
        }
        lastUpdatedAt = now
        viewModel.updateKnifeHeihgt(binding.seekbar.progress)
    }
    private fun initFetchFailedObserver() {
        viewModel.fetchSettingsFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

