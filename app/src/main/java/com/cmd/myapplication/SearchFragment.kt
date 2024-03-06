package com.cmd.myapplication

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.PlaceSearchResult
import com.cmd.myapplication.data.RouteSearchResult
import com.cmd.myapplication.data.StopSearchResult
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
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
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by activityViewModels { BusRoutesViewModel.Factory }

    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    private lateinit var searchList: RecyclerView
    private val searchListAdapter = SearchListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialFade().apply {
            duration = 400
        }

        exitTransition = MaterialFade().apply {
            duration = 200
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchList = view.findViewById(R.id.search_list)
        searchList.adapter = searchListAdapter

        searchViewModel.searchResults.observe(viewLifecycleOwner) {
            val adapterData = it.map { result ->
                if (result is StopSearchResult) {
                    val busStop = busStopsViewModel.busStops.value?.find { it.id == result.stopId }
                    val lines =
                        busLinesViewModel.busLines.value?.filter { result.lineIds.contains(it.id) }
                            ?: emptyList()

                    if (busStop != null) BusStopData(
                        busStop.id,
                        busStop.displayName,
                        lines.map { it.displayName }
                    ) else null
                } else if (result is RouteSearchResult) {
                    val line = busLinesViewModel.busLines.value?.find { it.id == result.lineId }
                    val route =
                        busRoutesViewModel.busRoutes.value?.find { it.lineId == result.lineId }
                            ?.routes
                            ?.find { it.id == result.routeId }

                    if (line != null && route != null) BusRouteData(
                        line.id,
                        route.id,
                        line.displayName,
                        route.name,
                        line.operators.first().name
                    ) else null
                } else {
                    val r = result as PlaceSearchResult

                    PlaceData(
                        r.id,
                        r.shortName,
                        r.name
                    )
                }
            }.filterNotNull()

            searchListAdapter.searchResults.apply {
                clear()
                addAll(adapterData)
            }

            searchListAdapter.notifyDataSetChanged()
        }
    }
}