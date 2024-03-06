package com.cmd.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import com.cmd.myapplication.utils.adapters.BusData
import com.cmd.myapplication.utils.adapters.BusListAdapter
import com.cmd.myapplication.utils.adapters.BusStopData
import com.cmd.myapplication.utils.adapters.BusStopListAdapter
import com.cmd.myapplication.utils.ListViewLayoutManager
import com.cmd.myapplication.utils.ScrollableHost
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialFade
import kotlin.math.abs
import kotlin.math.sign

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
//        container.isNestedScrollingEnabled = false

//        handleScroll()

        sharedViewModel.isBottomSheetDraggable.value = true
        sharedViewModel.isBottomSheetScrollable.observe(viewLifecycleOwner) {
            Log.e(TAG, "isScrollable - $it")
            container.isScrollable = true//it
        }

        nearbyBusesViewModel.nearbyBusStops.observe(viewLifecycleOwner) {
            val busData = it.map {
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

            val busStopsData = it.map {
                val linesIds = it.lines.toList()
                val lines = busLinesViewModel.busLines.value?.filter { linesIds.contains(it.id) }
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
    }

    private fun navigateToStopFragmentAnimated(view: View, stopId: String, lineId: String?) {
        sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
        sharedViewModel.isBottomSheetDraggable.value = false

        if (sharedViewModel.bottomSheetOffset.value != 1f) {
            sharedViewModel.bottomSheetOffset.observe(viewLifecycleOwner, object : Observer<Float> {
                override fun onChanged(value: Float) {
                    Log.e("NAV", "value - $value")
                    if (value == 1f) {
                        sharedViewModel.bottomSheetOffset.removeObserver(this)
                        Log.e("NAV", "postponed")
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

    private fun disableTouch() {
//        container.requestDisallowInterceptTouchEvent(true)
//        container.isScrollable = false
    }

    private fun enableTouch() {
//        container.requestDisallowInterceptTouchEvent(false)
//        container.isScrollable = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleScroll() {
//        requireView().setOnTouchListener { view, motionEvent -> false }
//        busStopsListView.setOnTouchListener { _, _ -> false }
//        busStopsListView.requestDisallowInterceptTouchEvent(true)
//        compactBusListView.setOnTouchListener { _, _ -> false }
//        compactBusListView.requestDisallowInterceptTouchEvent(true)

        container.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY == 0 && container.isNestedScrollingEnabled) {
                container.isNestedScrollingEnabled = false
                sharedViewModel.isBottomSheetDraggable.value = true
                disableTouch()
            }
        }

        var oldY: Float? = null

        // min dY for it to be considered a scroll attempt
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        // always return false as to not consume the event
        container.setOnTouchListener { view, touchEvent ->
            Log.e(TAG, "onTouch")
            if (touchEvent.action == MotionEvent.ACTION_UP) {
                oldY = null
            }

            if (touchEvent.action == MotionEvent.ACTION_MOVE) {
                // on first touch
                if (oldY == null) {
                    oldY = touchEvent.y

                    return@setOnTouchListener false
                }

                val dY = touchEvent.y - oldY!!

                if (abs(dY) >= touchSlop) {
                    val scrollDirection = -sign(dY)

                    if (scrollDirection > 0 && !container.isNestedScrollingEnabled) {
                        Log.e(TAG, "unlocked - dY=$dY sD=$scrollDirection")
                        sharedViewModel.isBottomSheetDraggable.value = false
                        container.isNestedScrollingEnabled = true
                        enableTouch()
                    }

                    oldY = touchEvent.y
                }
            }

            return@setOnTouchListener false
        }
    }

    companion object {
        const val TAG = "BusListFragment"
    }
}