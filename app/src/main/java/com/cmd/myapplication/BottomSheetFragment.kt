package com.cmd.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import com.cmd.myapplication.databinding.FragmentBottomSheetBinding
import com.google.android.material.transition.MaterialFade

/**
 * A simple [Fragment] subclass.
 * Use the [BottomSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BottomSheetFragment : Fragment(R.layout.fragment_bottom_sheet) {
    private val sharedViewModel: SharedViewModel by activityViewModels { SharedViewModel.Factory }

    private lateinit var binding: FragmentBottomSheetBinding
    private lateinit var busListFragmentContainer: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFade().apply {
            duration = 400
        }

        reenterTransition = MaterialFade().apply {
            duration = 400
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentBottomSheetBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        busListFragmentContainer = binding.busListFragmentContainer

        busListFragmentContainer.setOnTouchListener { _, _ -> false }
        view.setOnTouchListener { _, _ -> false }
    }

    private var children = 0

    companion object {
        const val TAG = "BottomSheetFragment"
    }
}