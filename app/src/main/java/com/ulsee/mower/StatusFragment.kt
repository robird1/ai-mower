package com.ulsee.mower

import android.app.AlertDialog
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.data.RobotStatusState.Companion.ACTION_STATUS_RESPONSE
import com.ulsee.mower.databinding.ActivityStatusBinding

private val TAG = StatusFragment::class.java.simpleName

class StatusFragment: Fragment() {
    private lateinit var binding: ActivityStatusBinding
    private lateinit var viewModel: StatusFragmentViewModel
    lateinit var bluetoothService: BluetoothLeService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothService = (activity as MainActivity).bluetoothService!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ActivityStatusBinding.inflate(inflater, container, false)

        initViewModel()

        registerBLEReceiver()

        initPowerObserver()

        addOnBackPressedCallback()

        binding.setupButton.setOnClickListener {
            findNavController().navigate(R.id.mapFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        requireActivity().unregisterReceiver(viewModel.gattUpdateReceiver)
        super.onDestroyView()
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this, StatusFragmentFactory(
            BluetoothLeRepository(bluetoothService))).get(StatusFragmentViewModel::class.java)
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    val dialog = AlertDialog.Builder(context)
                    dialog.setMessage("Disconnect and return to home page ?")
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_confirm) { _, _ ->

                            viewModel.disconnectDevice()
                            findNavController().popBackStack()

                        }
                        .setNegativeButton("cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            })
    }

    private fun registerBLEReceiver() {
        val filter = IntentFilter()
        filter.addAction(ACTION_STATUS_RESPONSE)
        requireActivity().registerReceiver(viewModel.gattUpdateReceiver, filter)
    }

    private fun initPowerObserver() {
        viewModel.powerIndication.observe(viewLifecycleOwner) {
            binding.powerPercentage.text = it+"%"
        }
    }

}