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
import androidx.recyclerview.widget.RecyclerView
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.FragmentScheduleEditorBinding

private val TAG = ScheduleEditorFragment::class.java.simpleName

class ScheduleEditorFragment : Fragment() {
    private lateinit var binding: FragmentScheduleEditorBinding
    private lateinit var viewModel: ScheduleEditorFragmentViewModel
    private lateinit var bluetoothService: BluetoothLeService
    private lateinit var bleRepository: BluetoothLeRepository
    private lateinit var datasAdapter: ScheduleEditorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
        bluetoothService = (requireActivity().application as App).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentScheduleEditorBinding.inflate(inflater, container, false)

        initViewModel()

        initScheduleObserver()
        initLoadingStatusObserver()
        initRecyclerView()
        viewModel.getSchedule()
        return binding.root
    }

    fun initViewModel() {
        bleRepository = BluetoothLeRepository(bluetoothService)
        viewModel = ViewModelProvider(this, ScheduleEditorFragmentViewModelFactory(bleRepository)).get(ScheduleEditorFragmentViewModel::class.java)
    }

    private fun initScheduleObserver() {
        viewModel.schedules.observe(viewLifecycleOwner) {
            // todo: update ui
            datasAdapter.submitList(arrayListOf(SingleSchedule(19, 1), SingleSchedule(33, 1)))
        }
    }

    private fun initRecyclerView() {
        datasAdapter = ScheduleEditorAdapter(childFragmentManager, { data -> }, {
            val arr = ArrayList(datasAdapter.currentList)
            arr.add(SingleSchedule(0, 1))
            datasAdapter.submitList(arr)
        })
        binding.recyclerView.adapter = datasAdapter
    }

    private fun initLoadingStatusObserver() {
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressView.isVisible = it
        }
    }
}