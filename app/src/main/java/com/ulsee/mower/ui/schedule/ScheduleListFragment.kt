package com.ulsee.mower.ui.schedule

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
import com.ulsee.mower.ble.BluetoothLeRepository
import com.ulsee.mower.ble.BluetoothLeService
import com.ulsee.mower.databinding.FragmentScheduleListBinding

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentScheduleListBinding.inflate(inflater, container, false)

        initViewModel()

        initScheduleObserver()
        initLoadingStatusObserver()
        initSubViewEntry()
//        initExpandableList()
        initWeekdays()
        bindClick()
        viewModel.getSchedule()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, ScheduleListFragmentViewModelFactory(bleRepository)).get(ScheduleListFragmentViewModel::class.java)
    }

    private fun initScheduleObserver() {
        viewModel.schedules.observe(viewLifecycleOwner) {
            // todo: update ui
            binding.layoutDetail1.textView.text = "09:41 AM (Lawn 1)"
            binding.layoutDetail1.layout.visibility = View.VISIBLE
            binding.layoutDetail2.textView.text = ""
            binding.layoutDetail3.textView.text = ""
            binding.layoutDetail4.textView.text = ""
            binding.layoutDetail5.textView.text = ""
            binding.layoutDetail6.textView.text = ""
            binding.layoutDetail7.textView.text = ""
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
        // todo: 1. checkbox, 2. 帶參數
        (binding.layoutWeekday1.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday2.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday3.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday4.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday5.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday6.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
        (binding.layoutWeekday7.container).setOnClickListener { findNavController().navigate(R.id.scheduleEditorFragment) }
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
}