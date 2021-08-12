package com.ulsee.mower.ui.schedule

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.databinding.FragmentScheduleEditorBinding

private val TAG = ScheduleEditorFragment::class.java.simpleName

class ScheduleEditorFragment : Fragment() {
    private lateinit var binding: FragmentScheduleEditorBinding
    private lateinit var viewModel: ScheduleEditorFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository
    private lateinit var datasAdapter: ScheduleEditorAdapter

    var week : Int = 1 // [1,7]
    lateinit var calendar: Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!

        week = arguments?.getInt("week") ?: 1
        calendar = arguments?.getSerializable("calendar") as Calendar
    }

    override fun onStart() {
        super.onStart()
        registerBLEReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterBLEReceiver()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentScheduleEditorBinding.inflate(inflater, container, false)
        val titleRes = arrayOf(
            R.string.monday,
            R.string.tuesday,
            R.string.wensday,
            R.string.thursday,
            R.string.friday,
            R.string.saturday,
            R.string.sunday
        )[week-1]
        binding.textViewToolbarTitle.text = getString(titleRes)

        initViewModel()

//        initScheduleObserver()
        initFetchFailedObserver()
        initFetchSettingsFailedObserver()
        initLoadingStatusObserver()
        initRecyclerView()
        initOnSave()
//        viewModel.getSchedule()
        viewModel.getSettings()

        datasAdapter.submitList(calendar.schedules.get(week-1))
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, ScheduleEditorFragmentViewModelFactory(bleRepository)).get(ScheduleEditorFragmentViewModel::class.java)
    }

//    private fun initScheduleObserver() {
//        viewModel.schedules.observe(viewLifecycleOwner) {
//            // todo: update ui
//            datasAdapter.submitList(arrayListOf(SingleSchedule(19, 1), SingleSchedule(33, 1)))
//        }
//    }

    private fun initRecyclerView() {
        datasAdapter = ScheduleEditorAdapter(childFragmentManager, { data -> }, {
            val arr = ArrayList(datasAdapter.currentList)
            arr.add(ScheduleEvent(0, 1))
            datasAdapter.submitList(arr)
            if (arr.size >= 5) datasAdapter.notifyDataSetChanged()
        })
        binding.recyclerView.adapter = datasAdapter
    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }
    private fun initFetchFailedObserver() {
        viewModel.fetchScheduleFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun initFetchSettingsFailedObserver() {
        viewModel.fetchSettingsFailedLog.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun initOnSave() {
        binding.buttonSave.setOnClickListener {
            // 複寫本日的schedules
            calendar.schedules[week-1] = ArrayList(datasAdapter.currentList)
            viewModel.save(calendar)
        }
    }

    // =================================================
    // ================== BLE ====================
    // =================================================

    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(BLEBroadcastAction.ACTION_SCHEDULING)
        filter.addAction(BLEBroadcastAction.ACTION_SETTINGS)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun unregisterBLEReceiver() {
        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
    }
}