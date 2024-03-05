package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.BusStopData
import com.cmd.myapplication.utils.BusStopListAdapter

/**
 * A simple [Fragment] subclass.
 * Use the [ExpandedBusListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExpandedBusListFragment : Fragment(R.layout.fragment_expanded_bus_list) {
    private val nearbyBusesViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by activityViewModels { BusRoutesViewModel.Factory }


    private lateinit var bottomSheetHeadingView: TextView
    private lateinit var expandedBusListView: RecyclerView

    private val busListAdapter = BusStopListAdapter()

    private var expandedView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bottomSheetHeadingView = view.findViewById(R.id.bottom_sheet_heading_view)
        expandedBusListView = view.findViewById(R.id.expanded_bus_list_view)

        expandedBusListView.isNestedScrollingEnabled = true

        expandedBusListView.adapter = busListAdapter

        busListAdapter.busStops.addAll(
            arrayOf(
                BusStopData(
                    "id0",
                    "Stop 1",
                    listOf("X2", "10", "2", "Y4", "A", "B", "C", "D", "E", "F")
                ),
                BusStopData("id1", "Stop 2", listOf("X2", "10", "2", "Y4")),
                BusStopData("id2", "Stop 3", listOf("X2", "10", "2", "Y4")),
                BusStopData("id3", "Stop 4", listOf("X2", "10", "2", "Y4")),
                BusStopData("id4", "Stop 5", listOf("X2", "10", "2", "Y4")),
                BusStopData("id5", "Stop 6", listOf("X2", "10", "2", "Y4")),
                BusStopData("id6", "Stop 7", listOf("X2", "10", "2", "Y4")),
                BusStopData("id7", "Stop 8", listOf("X2", "10", "2", "Y4")),
                BusStopData("id8", "Stop 9", listOf("X2", "10", "2", "Y4")),
            )
        )

        nearbyBusesViewModel.nearbyBusStops.observe(viewLifecycleOwner) {
            val data = it.map {
                val linesIds = it.lines.toList()
                val lines = busLinesViewModel.busLines.value?.filter { linesIds.contains(it.id) }
                    ?.map { it.displayName }
                    ?.toList()

                Log.e("NBVM", it.displayName)

                BusStopData(it.id, it.displayName, lines ?: emptyList())
            }

            busListAdapter.busStops.clear()
            busListAdapter.busStops.addAll(data)
            busListAdapter.notifyDataSetChanged()
        }

//        busListAdapter.onExpand { view, stopId ->
//            expandedView?.transitionName = null
//            expandedView?.findViewById<TextView>(R.id.stop_name_view)?.transitionName = null
//
//            view.transitionName = "expand_stop_transition"
//            view.findViewById<TextView>(R.id.stop_name_view).transitionName =
//                "translate_stop_name_view"
//            expandedView = view
//
//            nearbyBusesViewModel.selectedBusStop.value = stopId to view
//        }
    }
}