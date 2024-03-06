package com.cmd.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.cmd.myapplication.databinding.FragmentBottomSheetBinding

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

        sharedViewModel.isSearchFragmentVisible.observe(viewLifecycleOwner) {
            val navController =
                busListFragmentContainer.getFragment<NavHostFragment>().findNavController()

            val currentFragmentId = navController.currentDestination?.id

            if (it) {
                if (currentFragmentId == R.id.busListFragment) {
                    navController.navigate(BusListFragmentDirections.actionBusListFragmentToSearchFragment())
                } else if (currentFragmentId == R.id.stopFragment) {
                    navController.navigate(SearchFragmentDirections.actionSearchFragmentToBusListFragment())
                }
            } else {
                if (currentFragmentId == R.id.searchFragment) {
                    navController.navigateUp()
                }
            }
        }
    }

    private var children = 0

    companion object {
        const val TAG = "BottomSheetFragment"
    }
}