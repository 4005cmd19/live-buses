package com.cmd.myapplication.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cmd.myapplication.LineFragment

class LinePagerAdapter(
    private val lineIds: MutableList<String> = mutableListOf(), fragment: Fragment,
) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return lineIds.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = LineFragment()
        fragment.arguments = Bundle().apply {
            putString("lineId", lineIds[position])
        }

        return fragment
    }
}