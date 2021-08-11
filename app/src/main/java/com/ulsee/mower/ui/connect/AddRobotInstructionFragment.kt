package com.ulsee.mower.ui.connect

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.ulsee.mower.R
import com.ulsee.mower.data.model.AppPreference
import com.ulsee.mower.databinding.ActivityAddRobotInstructionBinding

private const val NUM_PAGES = 2

class AddRobotInstructionFragment: Fragment() {
    private lateinit var binding: ActivityAddRobotInstructionBinding
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ActivityAddRobotInstructionBinding.inflate(inflater, container, false)

        viewPager = binding.viewPager

        // The pager adapter, which provides the pages to the view pager widget.
        viewPager.adapter = activity?.let { ScreenSlidePagerAdapter(it, getFragmentList()) }

        TabLayoutMediator(binding.tabLayout, viewPager) { tab, position ->     }.attach()

        initNextBtnListener()

        addOnBackPressedCallback()

        return binding.root
    }

    private fun getFragmentList() = arrayListOf(InstructionPage1(), InstructionPage2())

    private fun initNextBtnListener() {
        binding.nextBtn.setOnClickListener {
            val next = viewPager.currentItem + 1
            if (next < NUM_PAGES) {
                // move to next screen
                viewPager.currentItem = next
            } else {
                val appPreference = AppPreference(PreferenceManager.getDefaultSharedPreferences(activity))
                appPreference.setFirstAddDevice()

                val action = AddRobotInstructionFragmentDirections.actionBackToRobotList(true)
                findNavController().navigate(action)
            }
        }
    }

    private fun addOnBackPressedCallback() {
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    if (viewPager.currentItem == 0) {
                        // If the user is currently looking at the first step, allow the system to handle the
                        // Back button. This calls finish() on this activity and pops the back stack.
//                        findNavController().popBackStack()
                        val action = AddRobotInstructionFragmentDirections.actionBackToRobotList(false)
                        findNavController().navigate(action)
                    } else {
                        // Otherwise, select the previous step.
                        viewPager.currentItem = viewPager.currentItem - 1
                    }
                }
            })
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity, private val fragments: ArrayList<Fragment>) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment = fragments[position]
    }

}


class InstructionPage1: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_robot_instruction1, container, false)

}


class InstructionPage2: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_robot_instruction2, container, false)

}
