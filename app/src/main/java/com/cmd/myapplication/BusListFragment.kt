package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.BusDataViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.ListViewLayoutManager
import com.cmd.myapplication.utils.ScrollableHost
import com.cmd.myapplication.utils.adapters.BusData
import com.cmd.myapplication.utils.adapters.BusListAdapter
import com.cmd.myapplication.utils.adapters.BusStopData
import com.cmd.myapplication.utils.adapters.BusStopListAdapter
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
    private val busDataViewModel: BusDataViewModel by activityViewModels { BusDataViewModel.Factory }

    private lateinit var container: ScrollableHost
    private lateinit var bottomSheetHeadingView: TextView
    private lateinit var compactBusListView: RecyclerView
    private lateinit var busStopsListView: RecyclerView

    private val busListAdapter = BusListAdapter()
    private val busStopListAdapter = BusStopListAdapter()

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

        container = view.findViewById(R.id.container)
        bottomSheetHeadingView = view.findViewById(R.id.bottom_sheet_heading_view)
        compactBusListView = view.findViewById(R.id.compact_bus_list_view)
        busStopsListView = view.findViewById(R.id.bus_stops_list_view)

        compactBusListView.adapter = busListAdapter
        busStopsListView.adapter = busStopListAdapter

        val layoutManager = busStopsListView.layoutManager as ListViewLayoutManager
        layoutManager.canScrollVertically = false

        handleCloseGesture()

        nearbyBusesViewModel.nearbyBusStops.observe(viewLifecycleOwner) {
            val busData = it.map {
                val lineId = it.lines.first()

                val line = busDataViewModel.busLines.value?.find { it.id == lineId }
                val routes = busDataViewModel.busLineRoutes.value?.find { it.lineId == line?.id }
                val route = routes?.routes?.first()

                BusData(
                    it.id,
                    line?.id ?: "",
                    line?.displayName ?: "",
                    it.displayName,
                    route?.destinationName ?: "",
                    route?.destinationName ?: "",
                    "Now"
                )
            }

            val busStopsData = it.map {
                val linesIds = it.lines.toList()
                val lines = busDataViewModel.busLines.value?.filter { linesIds.contains(it.id) }
                    ?.map { it.displayName }
                    ?.toList()

                Log.e("NBVM", it.displayName)

                BusStopData(it.id, it.displayName, lines ?: emptyList())
            }

            busListAdapter.busList.clear()
            busListAdapter.busList.addAll(busData)
            busListAdapter.notifyDataSetChanged()

            Log.e(TAG, busStopsData.joinToString())

            busStopListAdapter.busStops.clear()
            busStopListAdapter.busStops.addAll(busStopsData)
            busStopListAdapter.notifyDataSetChanged()
        }

        busListAdapter.setOnExpandListener { itemView, _, data ->
            navigateToStopFragmentAnimated(itemView, data.stopId, data.lineId)
        }

        busStopListAdapter.setOnExpandListener { itemView, _, data ->
            navigateToStopFragmentAnimated(itemView, data.id, null)
        }

        sharedViewModel.isBottomSheetDraggable.value = true
        sharedViewModel.isMainBackPressedCallbackEnabled.value = true
        sharedViewModel.isInSearchMode.value = false

        sharedViewModel.bottomSheetState.observe(viewLifecycleOwner) {
            if (it != BottomSheetBehavior.STATE_EXPANDED) {
                container.isScrollable = false
                container.scrollY = 0
            } else {
                Log.e(TAG, "isScrollable - true")
                container.isScrollable = true
            }
        }
    }

    private fun navigateToStopFragmentAnimated(view: View, stopId: String, lineId: String?) {
        sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
        sharedViewModel.isBottomSheetDraggable.value = false

        if (sharedViewModel.bottomSheetOffset.value != 1f) {
            sharedViewModel.bottomSheetOffset.observe(viewLifecycleOwner, object : Observer<Float> {
                override fun onChanged(value: Float) {
                    if (value == 1f) {
                        sharedViewModel.bottomSheetOffset.removeObserver(this)
                        navigateToStopFragment(view, stopId, lineId)
                    }
                }
            })
        } else {
            navigateToStopFragment(view, stopId, lineId)
        }
    }

    private fun navigateToStopFragment(view: View, stopId: String, lineId: String?) {
        findNavController().navigate(
            BusListFragmentDirections.actionBusListFragmentToStopFragment(
                stopId,
                lineId
            ),
            FragmentNavigatorExtras(view to "expand_stop_transition"),
        )
    }

    private fun handleCloseGesture() {
        var overscrollCount = 0
        var shouldShowToast = true

        container.setOnOverScrolledListener { scrollX, scrollY, clampedX, clampedY ->
            if (clampedY && scrollY == 0) {
                overscrollCount++

                if (overscrollCount == 2 && shouldShowToast) {
                    Toast.makeText(context,
                        getString(R.string.close_bottom_sheet_hint), Toast.LENGTH_SHORT).apply {
                        addCallback(object : Toast.Callback() {
                            override fun onToastHidden() {
                                super.onToastHidden()

                                shouldShowToast = true
                                overscrollCount = 0
                            }
                        })
                    }.show()

                    shouldShowToast = false
                }
            } else if (scrollY > 0) {
                overscrollCount = 0
            }
        }
    }

    companion object {
        const val TAG = "BusListFragment"
    }
}