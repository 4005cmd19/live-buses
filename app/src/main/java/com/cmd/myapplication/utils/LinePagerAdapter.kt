package com.cmd.myapplication.utils

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cmd.myapplication.LineFragment

class LinePagerAdapter(
    val lineIds: MutableList<String> = mutableListOf(), fragment: Fragment,
) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return lineIds.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.e("StopFragment", "adapter - create")

        val fragment = LineFragment()
        fragment.arguments = Bundle().apply {
            putString("lineId", lineIds[position])
        }

        return fragment
    }
}