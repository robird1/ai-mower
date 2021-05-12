package com.ulsee.mower

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


private const val NUM_PAGES = 2

class AddRobotInstructionActivity: AppCompatActivity() {
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_robot_instruction)

        viewPager = findViewById(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        // The pager adapter, which provides the pages to the view pager widget.
        viewPager.adapter = ScreenSlidePagerAdapter(this, getFragmentList())

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->     }.attach()

        initNextBtnListener()

    }

    private fun getFragmentList() = arrayListOf(
        AddRobotInstruction1Fragment(), AddRobotInstruction2Fragment())

    private fun initNextBtnListener() {
        val btnNext = findViewById<Button>(R.id.nextBtn)
        btnNext.setOnClickListener { v ->
            val next = viewPager.currentItem + 1
            if (next < NUM_PAGES) {
                // move to next screen
                viewPager.currentItem = next
            } else {
                val intent = Intent(this@AddRobotInstructionActivity, RobotPairingListActivity::class.java)
                intent.putExtra("mode", "fake_list")
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager.currentItem = viewPager.currentItem - 1
        }
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