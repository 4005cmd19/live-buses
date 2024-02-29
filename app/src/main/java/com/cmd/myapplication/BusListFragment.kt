package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.BusData
import com.cmd.myapplication.utils.BusListAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialFade

/**
 * A simple [Fragment] subclass.
 * Use the [BusListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BusListFragment : Fragment(R.layout.fragment_bus_list) {
    private val sharedViewModel: SharedViewModel by activityViewModels { SharedViewModel.Factory }

    private val nearbyBusesViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }

    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by activityViewModels { BusRoutesViewModel.Factory }

    private lateinit var bottomSheetHeadingView: TextView
    private lateinit var compactBusListView: RecyclerView

    private val busListAdapter = BusListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exitDuration = MotionUtils.resolveThemeDuration(
            requireContext(),
            com.google.android.material.R.attr.motionDurationShort4,
            200
        ).toLong()

        val enterDuration = MotionUtils.resolveThemeDuration(
            requireContext(),
            com.google.android.material.R.attr.motionDurationMedium4,
            400
        ).toLong()

        exitTransition = MaterialFade().apply {
            duration = exitDuration
        }

        reenterTransition = MaterialFade().apply {
            duration = enterDuration
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        bottomSheetHeadingView = view.findViewById(R.id.bottom_sheet_heading_view)
        compactBusListView = view.findViewById(R.id.compact_bus_list_view)

        compactBusListView.adapter = busListAdapter

        sharedViewModel.isBottomSheetDraggable.value = true

        nearbyBusesViewModel.nearbyBusStops.observe(viewLifecycleOwner) {
            busListAdapter.busList.clear()

            val data = it.map {
                val lineId = it.lines.first()

                val line = busLinesViewModel.busLines.value?.find { it.id == lineId }
                val routes = busRoutesViewModel.busRoutes.value?.find { it.lineId == line?.id }
                val route = routes?.routes?.first()

                BusData(
                    it.id,
                    line?.id ?: "",
                    line?.displayName ?: "",
                    it.displayName,
                    route?.destinationName ?: "",
                    "Now"
                )
            }

            busListAdapter.busList.clear()
            busListAdapter.busList.addAll(data)
            busListAdapter.notifyDataSetChanged()
        }

        busListAdapter.setOnExpandListener { itemView, _, data ->
//            itemView.transitionName = "expand_stop_transition_${Random.nextInt()}"
//            sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED

//            findNavController().navigate(
//                BusListFragmentDirections.actionBusListFragmentToStopFragment(
//                    data.stopId,
//                    data.lineId
//                ),
//                FragmentNavigatorExtras(itemView to "expand_stop_transition"),
//            )

            navigateToStopFragmentAnimated(itemView, data)
        }
    }

    private fun navigateToStopFragmentAnimated (view: View, data: BusData) {
        sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
        sharedViewModel.isBottomSheetDraggable.value = false

        if (sharedViewModel.bottomSheetOffset.value != 1f) {
            sharedViewModel.bottomSheetOffset.observe(viewLifecycleOwner, object : Observer<Float> {
                override fun onChanged(value: Float) {
                    Log.e("NAV", "value - $value")
                    if (value == 1f) {
                        sharedViewModel.bottomSheetOffset.removeObserver(this)
                        Log.e("NAV", "postponed")
                        navigateToStopFragment(view, data)
                    }
                }
            })
        }
        else {
            navigateToStopFragment(view, data)
        }
    }

    private fun navigateToStopFragment(view: View, data: BusData) {
        findNavController().navigate(
            BusListFragmentDirections.actionBusListFragmentToStopFragment(
                data.stopId,
                data.lineId
            ),
            FragmentNavigatorExtras(view to "expand_stop_transition"),
        )
    }
}