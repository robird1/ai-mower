package com.ulsee.mower.ui.schedule

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.databinding.FragmentScheduleCalendarBinding


private val TAG = ScheduleCalendarFragment::class.java.simpleName

class ScheduleCalendarFragment : Fragment() {
    private lateinit var binding: FragmentScheduleCalendarBinding
    private lateinit var viewModel: ScheduleCalendarFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!
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

        binding = FragmentScheduleCalendarBinding.inflate(inflater, container, false)

        initViewModel()

        initScheduleObserver()
        initLoadingStatusObserver()
        initFetchFailedObserver()
        viewModel.getSchedule()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, ScheduleCalendarFragmentViewModelFactory(bleRepository)).get(ScheduleCalendarFragmentViewModel::class.java)
    }

    private fun initScheduleObserver() {
        viewModel.schedules.observe(viewLifecycleOwner) {
            displaySchedules(it)
        }
    }

    private fun displaySchedules(calendar: Calendar) {
        for(i in 0 until calendar.schedules.size) {
            val week = i+1
            val schedules = calendar.schedules[i]
            for(schedule in schedules) {
                displaySchedule(week, schedule.beginAt, schedule.duration)
            }
        }
    }

    private fun displaySchedule(week: Int, beginHalfHour: Int, durationHalfHours: Int) {
        if(week < 1 || week > 7) throw IllegalArgumentException("displaySchedule: week should be in 1~7")
        if(beginHalfHour < 0 || beginHalfHour > 47) throw IllegalArgumentException("displaySchedule: beginHalfHour should be in 0~47")
        if(durationHalfHours < 1 || durationHalfHours > 48) throw IllegalArgumentException("displaySchedule: durationHalfHours should be in 1~48")

        val density = resources.displayMetrics.density

        val v: View = (context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.item_schedule_duration, null)

        val weekdayLayoutIDs = arrayListOf(R.id.layout_weekday_1, R.id.layout_weekday_2, R.id.layout_weekday_3, R.id.layout_weekday_4, R.id.layout_weekday_5, R.id.layout_weekday_6, R.id.layout_weekday_7)
        val layout = binding.root.findViewById<RelativeLayout>(weekdayLayoutIDs[week - 1])
        val contentHeight = layout.measuredHeight?.minus((32+44)*density) ?: 0f
        val height = contentHeight / 48f * durationHalfHours
        val marginTop = contentHeight / 48f * beginHalfHour + 32f * density
        val layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height.toInt())
        layoutParams.topMargin = marginTop.toInt()
        layoutParams.marginStart = (5f*density).toInt()
        layoutParams.marginEnd = (5f*density).toInt()
        layout?.addView(v, 0, layoutParams)
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

    // =================================================
    // ================== BLE ====================
    // =================================================
    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(BLEBroadcastAction.ACTION_SCHEDULING)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun unregisterBLEReceiver() {
        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
    }

}