package com.cmd.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.BusLineRouteSearchResult
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.viewModels.BusDataViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.SearchViewModel
import com.cmd.myapplication.utils.adapters.BusRouteData
import com.cmd.myapplication.utils.adapters.BusStopData
import com.cmd.myapplication.utils.adapters.PlaceData
import com.cmd.myapplication.utils.adapters.SearchListAdapter
import com.google.android.material.transition.MaterialFade

/**
 * A simple [Fragment] subclass.
 * Use the [SearchFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SearchFragment : Fragment(R.layout.fragment_search) {
    private val busDataViewModel: BusDataViewModel by activityViewModels { BusDataViewModel.Factory }
    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }

    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    private lateinit var searchList: RecyclerView
    private val searchListAdapter = SearchListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFade().apply {
            duration = 400
        }

        exitTransition = MaterialFade().apply {
            duration = 400
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchList = view.findViewById(R.id.search_list)
        searchList.adapter = searchListAdapter

        deviceLocationViewModel.currentLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                searchViewModel.supplyLocation(it)
            }
        }

        busDataViewModel.data.observe(viewLifecycleOwner) {
            val (busStops, busLines, busLineRoutes) = it

            searchViewModel.supplyBusData(busStops, busLines, busLineRoutes)
        }

        searchViewModel.searchText.observe(viewLifecycleOwner) {
            searchViewModel.search(it)
        }

        searchViewModel.searchResults.observe(viewLifecycleOwner) {
            val adapterData = it.map { result ->
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

                    is BusLineRouteSearchResult -> {
                        val line = result.line
                        val route = result.route

                        BusRouteData(
                            line.id,
                            route.id,
                            line.displayName,
                            route.name,
                            line.operators.first().name
                        )
                    }

                    else -> {
                        val r = result as PlaceSearchResult

                        PlaceData(
                            r.id,
                            r.name,
                            r.address
                        )
                    }
                }
            }

            searchListAdapter.searchResults.apply {
                clear()
                addAll(adapterData)
            }

            searchListAdapter.notifyDataSetChanged()
        }


    }
}