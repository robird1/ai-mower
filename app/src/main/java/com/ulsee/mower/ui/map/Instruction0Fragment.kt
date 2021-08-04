package com.ulsee.mower.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.R
import com.ulsee.mower.databinding.FragmentInstruction0Binding

class Instruction0Fragment: Fragment() {
    lateinit var binding: FragmentInstruction0Binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInstruction0Binding.inflate(inflater, container, false)

        binding.startBtn.setOnClickListener {
            findNavController().navigate(R.id.setupMapFragment)
        }

        return binding.root
    }
}