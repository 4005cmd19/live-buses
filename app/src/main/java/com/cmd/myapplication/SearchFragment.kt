package com.cmd.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.cmd.myapplication.data.BusLineRouteSearchResult
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.viewModels.BusDataViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.SearchViewModel
import com.cmd.myapplication.utils.ListViewLayoutManager
import com.cmd.myapplication.utils.adapters.BusRouteData
import com.cmd.myapplication.utils.adapters.BusStopData
import com.cmd.myapplication.utils.adapters.PlaceData
import com.cmd.myapplication.utils.adapters.PlacesListAdapter
import com.cmd.myapplication.utils.adapters.RoutesListAdapter
import com.cmd.myapplication.utils.adapters.SearchListAdapter
import com.cmd.myapplication.utils.adapters.SearchResultData
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFade

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment(R.layout.fragment_search) {
    private val sharedViewModel: SharedViewModel by activityViewModels { SharedViewModel.Factory }

    private val busDataViewModel: BusDataViewModel by activityViewModels { BusDataViewModel.Factory }
    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }

    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    private lateinit var searchHintViewGroup: Group
    private lateinit var hintTextView: TextView
    private lateinit var hintIconView: ImageView
    private lateinit var container: NestedScrollView
    private lateinit var placesList: RecyclerView
    private lateinit var routesList: RecyclerView

    private val placesListAdapter = PlacesListAdapter()
    private val routesListAdapter = RoutesListAdapter()

    private lateinit var hintShowTransition: Transition
    private lateinit var hintHideTransition: Transition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFade().apply {
            duration = 400
        }

        exitTransition = MaterialFade().apply {
            duration = 200
        }

        returnTransition = exitTransition
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // get views
        searchHintViewGroup = view.findViewById(R.id.search_hint_view_group)
        hintTextView = view.findViewById(R.id.search_hint_text_view)
        hintIconView = view.findViewById(R.id.search_hint_icon_view)
        container = view.findViewById(R.id.container)
        routesList = view.findViewById(R.id.routes_list)
        placesList = view.findViewById(R.id.places_list)

        // lock list - scroll handled by nested scroll view
        placesList.layoutManager.let { it as ListViewLayoutManager }.canScrollVertically = false

        // set adapters
        routesList.adapter = routesListAdapter
        placesList.adapter = placesListAdapter

        // define animations
        hintShowTransition = MaterialElevationScale(true).apply {
            addTarget(hintTextView)
            addTarget(hintIconView)

            duration = MotionUtils.resolveThemeDuration(
                requireContext(),
                com.google.android.material.R.attr.motionDurationShort4,
                400
            ).toLong()
        }

        hintHideTransition = MaterialElevationScale(false).apply {
            addTarget(hintTextView)
            addTarget(hintIconView)
            duration = MotionUtils.resolveThemeDuration(
                requireContext(),
                com.google.android.material.R.attr.motionDurationMedium4,
                200
            ).toLong()
        }

        deviceLocationViewModel.currentLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                searchViewModel.setLocationBias(it)
            }
        }

        busDataViewModel.data.observe(viewLifecycleOwner) {
            val (busStops, busLines, busLineRoutes) = it

            searchViewModel.supplyBusData(busStops, busLines, busLineRoutes)
        }

        searchViewModel.searchText.observe(viewLifecycleOwner) {
            if (it != null) {
                // search
                searchViewModel.search(it)
            }
        }

        searchViewModel.searchResults.observe(viewLifecycleOwner) {
            // get bus data from search results
            val busData = it.filterNot { it is PlaceSearchResult }.map { result ->
                when (result) {
                    is BusStop -> {
                        val lines =
                            busDataViewModel.busLines.value?.filter { result.lines.contains(it.id) }
                                ?: emptyList()

                        BusStopData(
                            result.id,
                            result.displayName,
                            lines.map { it.displayName }
                        )
                    }

                    else -> {
                        val r = result as BusLineRouteSearchResult

                        val line = r.line
                        val route = r.route

                        BusRouteData(
                            line.id,
                            route.id,
                            line.displayName,
                            route.name,
                            route.destinationName,
                            line.operators.firstOrNull()?.name ?: "Tfl"
                        )
                    }
                }
            }

            // get place data
            val placeData = it.filterIsInstance<PlaceSearchResult>().sortedBy { it.distance }.map {
                PlaceData(
                    it.id,
                    it.name,
                    it.address
                )
            }.toTypedArray()

            // separate and reorganize
            val stopData = busData.filterIsInstance<BusStopData>().toTypedArray()
            val routesData = busData.filterIsInstance<BusRouteData>()

            val placesListAdapterData = listOf(*placeData, *stopData)

            // update lists
            updateAdapterItems(placesListAdapter, placesListAdapterData)
            updateAdapterItems(routesListAdapter, routesData)
        }

        sharedViewModel.isBottomSheetDraggable.value = false
        sharedViewModel.isMainBackPressedCallbackEnabled.value = true
    }

    private fun updateAdapterItems(
        adapter: SearchListAdapter<*>,
        newItems: List<SearchResultData>,
    ) {
        val oldCount = adapter.itemCount
        val newCount = newItems.size

        // show/hide hint
        val newVisibility = if (newCount == 0) {
            TransitionManager.beginDelayedTransition(requireView() as ViewGroup, hintShowTransition)
            View.VISIBLE
        } else {
            if (oldCount > 0) {
                TransitionManager.beginDelayedTransition(
                    requireView() as ViewGroup,
                    hintHideTransition
                )
            }
            View.INVISIBLE
        }

        // avoid double animations
        if (newVisibility != searchHintViewGroup.visibility) {
            searchHintViewGroup.visibility = newVisibility
        }

        // update list
        adapter.searchResults.apply {
            clear()
            addAll(newItems)
        }

        // animate updates
        if (newCount < oldCount) {
            adapter.notifyItemRangeChanged(0, newCount)
            adapter.notifyItemRangeRemoved(newCount, oldCount)
        } else if (newCount > oldCount) {
            adapter.notifyItemRangeChanged(0, oldCount)
            adapter.notifyItemRangeInserted(oldCount, newCount)
        } else {
            adapter.notifyItemRangeChanged(0, newCount)
        }
    }

    companion object {
        const val TAG = "SearchFragment"
    }
}