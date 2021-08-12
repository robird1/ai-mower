package com.ulsee.mower.ui.map

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.R
import com.ulsee.mower.databinding.FragmentInstruction0Binding
import com.ulsee.mower.databinding.FragmentInstruction1Binding
import com.ulsee.mower.databinding.FragmentInstruction2Binding

class Instruction0Fragment: Fragment() {
    lateinit var binding: FragmentInstruction0Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInstruction0Binding.inflate(inflater, container, false)

        binding.startBtn.setOnClickListener {

            findNavController().navigate(R.id.instruction1Fragment)
        }

        return binding.root
    }
}


class Instruction1Fragment: Fragment() {
    lateinit var binding: FragmentInstruction1Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        binding = FragmentInstruction1Binding.inflate(inflater, container, false)

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        binding.instructionNextBtn.setOnClickListener {
            findNavController().navigate(R.id.instruction2Fragment)
        }

        addOnBackPressedCallback()

        return binding.root
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // disable user pressing back button
                }
            })
    }

}


class Instruction2Fragment: Fragment() {
    lateinit var binding: FragmentInstruction2Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInstruction2Binding.inflate(inflater, container, false)

        binding.instruction2NextBtn.setOnClickListener {
            findNavController().navigate(R.id.setupMapFragment)
        }

        addOnBackPressedCallback()

        return binding.root
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // disable user pressing back button
                }
            })
    }

}