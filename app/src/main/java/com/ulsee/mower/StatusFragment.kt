package com.ulsee.mower

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.databinding.ActivityStatusBinding

class StatusFragment: Fragment() {
    private lateinit var binding: ActivityStatusBinding
    private lateinit var viewModel: StatusFragmentViewModel
//    private val viewModel: RobotListFragmentViewModel by activityViewModels()

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

                            findNavController().popBackStack()

                        }.show()
                }
            })
    }

}