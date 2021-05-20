package com.ulsee.mower

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.ActivityStatusBinding

private val TAG = StatusFragment::class.java.simpleName

class StatusFragment: Fragment() {
    private lateinit var binding: ActivityStatusBinding
    private lateinit var viewModel: StatusFragmentViewModel
//    private val viewModel: RobotListFragmentViewModel by activityViewModels()
var bluetoothService: BluetoothLeService? = null

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            Log.d(TAG, "[Enter] onServiceConnected")

            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()

        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "[Enter] onServiceDisconnected")

            bluetoothService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gattServiceIntent = Intent(context, BluetoothLeService::class.java)
        requireActivity().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ActivityStatusBinding.inflate(inflater, container, false)
        super.onCreate(savedInstanceState)

        viewModel = initViewModel()

        addOnBackPressedCallback()

        binding.button2.setOnClickListener {


        }

        return binding.root
    }

    // TODO
    private fun initViewModel(): StatusFragmentViewModel {
        return ViewModelProvider.AndroidViewModelFactory(requireActivity().application).create(StatusFragmentViewModel::class.java)
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

                            bluetoothService!!.disconnectDevice()
                            findNavController().popBackStack()

                        }.show()
                }
            })
    }

}