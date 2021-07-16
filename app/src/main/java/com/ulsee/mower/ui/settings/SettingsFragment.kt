package com.ulsee.mower.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.ulsee.mower.App
import com.ulsee.mower.R
import com.ulsee.mower.data.BluetoothLeRepository
import com.ulsee.mower.data.BluetoothLeService
import com.ulsee.mower.databinding.FragmentSettingsBinding
private val TAG = SettingsFragment::class.java.simpleName

class SettingsFragment: Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: SettingsFragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "[Enter] onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "[Enter] onCreateView")

        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        initViewModel()

        // init ui
        initSubViewEntry()
        return binding.root
    }

    fun initViewModel() {
        viewModel = ViewModelProvider(this, SettingsFragmentViewModelFactory()).get(SettingsFragmentViewModel::class.java)
    }

    private fun initSubViewEntry() {
        binding.layoutMower.setOnClickListener { findNavController().navigate(R.id.mowerSettingsFragment) }
    }
}

