package com.ulsee.mower.ui.schedule

import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.data.BLEBroadcastAction
import com.ulsee.mower.databinding.FragmentScheduleListBinding
import com.ulsee.mower.ui.map.SetupMapFragmentDirections

private val TAG = ScheduleListFragment::class.java.simpleName

class ScheduleListFragment : Fragment() {
    private lateinit var binding: FragmentScheduleListBinding
    private lateinit var viewModel: ScheduleListFragmentViewModel
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

        binding = FragmentScheduleListBinding.inflate(inflater, container, false)

        initViewModel()

        initScheduleObserver()
        initLoadingStatusObserver()
        initSubViewEntry()
//        initExpandableList()
        initFetchFailedObserver()
        initWeekdays()
        bindClick()
        viewModel.getSchedule()
        addOnBackPressedCallback()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, ScheduleListFragmentViewModelFactory(bleRepository)).get(ScheduleListFragmentViewModel::class.java)
    }

    private fun initScheduleObserver() {
        viewModel.schedules.observe(viewLifecycleOwner) {

            val weekDayDetailTextViews = arrayOf(
                binding.layoutDetail1.textView,
                binding.layoutDetail2.textView,
                binding.layoutDetail3.textView,
                binding.layoutDetail4.textView,
                binding.layoutDetail5.textView,
                binding.layoutDetail6.textView,
                binding.layoutDetail7.textView
            )
            val weekDayDetailLayouts = arrayOf(
                binding.layoutDetail1.layout,
                binding.layoutDetail2.layout,
                binding.layoutDetail3.layout,
                binding.layoutDetail4.layout,
                binding.layoutDetail5.layout,
                binding.layoutDetail6.layout,
                binding.layoutDetail7.layout
            )
            val checkboxes = arrayOf(
                binding.layoutWeekday1.checkbox,
                binding.layoutWeekday2.checkbox,
                binding.layoutWeekday3.checkbox,
                binding.layoutWeekday4.checkbox,
                binding.layoutWeekday5.checkbox,
                binding.layoutWeekday6.checkbox,
                binding.layoutWeekday7.checkbox
            )

            val calendar = it
            for(i in 0 until calendar.schedules.size) {
                val schedules = calendar.schedules[i]

                if (schedules.size > 0) {
                    checkboxes[i].isChecked = true
                    weekDayDetailLayouts[i].visibility = View.VISIBLE
                    weekDayDetailTextViews[i].text = ""
                    for(j in 0 until schedules.size) {
                        val schedule = schedules[j]
                        if (j > 0) weekDayDetailTextViews[i].append("\n")
                        var beginHour = schedule.beginAt / 2
                        if(beginHour > 12) beginHour = beginHour % 12
                        val beginMinute = if(schedule.beginAt %2 == 0) 0 else 30
                        val ampm = if(schedule.beginAt >= 24) "pm" else "am"
                        val dd : (Int)->String = {o -> if(o>=10)""+o else "0"+o}
                        weekDayDetailTextViews[i].append("${dd(beginHour)}:${dd(beginMinute)} $ampm")
                    }
                } else {
                    checkboxes[i].isChecked = false
                    weekDayDetailLayouts[i].visibility = View.GONE
                    weekDayDetailTextViews[i].text = ""
                }
            }
        }
    }

    fun initWeekdays() {
        binding.layoutWeekday1.textView.text = getString(R.string.monday)
        binding.layoutWeekday1.viewDivider.isVisible = false
        binding.layoutWeekday2.textView.text = getString(R.string.tuesday)
        binding.layoutWeekday3.textView.text = getString(R.string.wensday)
        binding.layoutWeekday4.textView.text = getString(R.string.thursday)
        binding.layoutWeekday5.textView.text = getString(R.string.friday)
        binding.layoutWeekday6.textView.text = getString(R.string.saturday)
        binding.layoutWeekday7.textView.text = getString(R.string.sunday)

        binding.layoutDetail1.layout.visibility = View.GONE
        binding.layoutDetail2.layout.visibility = View.GONE
        binding.layoutDetail3.layout.visibility = View.GONE
        binding.layoutDetail4.layout.visibility = View.GONE
        binding.layoutDetail5.layout.visibility = View.GONE
        binding.layoutDetail6.layout.visibility = View.GONE
        binding.layoutDetail7.layout.visibility = View.GONE
    }

    fun bindClick() {
        (binding.layoutWeekday1.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 1); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday2.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 2); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday3.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 3); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday4.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 4); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday5.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 5); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday6.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 6); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday7.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 7); putSerializable("calendar", viewModel.schedules.value) }) }

        (binding.layoutWeekday1.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 1); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday2.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 2); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday3.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 3); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday4.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 4); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday5.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 5); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday6.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 6); putSerializable("calendar", viewModel.schedules.value) }) }
        (binding.layoutWeekday7.checkbox).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment, Bundle().apply { putInt("week", 7); putSerializable("calendar", viewModel.schedules.value) }) }
    }

//    private fun initExpandableList() {
//        val weeks = arrayListOf(
//                getString(R.string.monday),
//                getString(R.string.tuesday),
//                getString(R.string.wensday),
//                getString(R.string.thursday),
//                getString(R.string.friday),
//                getString(R.string.saturday),
//                getString(R.string.sunday)
//        )
//        val details = arrayListOf(
//                "09:41 AM (Lawn 1)",
//                "",
//                "",
//                "",
//                "",
//                "",
//                "",
//        )
//        val adapter = ScheduleListExpandableAdapter(requireContext(), weeks, details)
//        binding.expandableListView.setAdapter(adapter)
//    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }

    private fun initSubViewEntry() {
        binding.buttonCalendar.setOnClickListener { findNavController().navigate(R.id.scheduleCalendarFragment) }
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

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val action = ScheduleListFragmentDirections.actionToStatusFragment()
                    findNavController().navigate(action)
                }
            })
    }

}