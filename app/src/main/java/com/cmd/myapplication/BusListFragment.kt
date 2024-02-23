package com.cmd.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.BusData
import com.cmd.myapplication.utils.BusListAdapter

/**
 * A simple [Fragment] subclass.
 * Use the [BusListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BusListFragment : Fragment(R.layout.fragment_bus_list) {
    private val nearbyBusesViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }

    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by activityViewModels { BusRoutesViewModel.Factory }

    private lateinit var bottomSheetHeadingView: TextView
    private lateinit var compactBusListView: RecyclerView

    private val busListAdapter = BusListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bottomSheetHeadingView = view.findViewById(R.id.bottom_sheet_heading_view)
        compactBusListView = view.findViewById(R.id.compact_bus_list_view)

        compactBusListView.adapter = busListAdapter

        busListAdapter.busList.addAll(
            arrayOf(
                BusData("X2", "Stop 1", "London", "Now"),
                BusData("10", "Stop 2", "London", "Later"),
                BusData("2", "Stop 3", "London", "Later"),
                BusData("Y4", "Stop 4", "London", "Later"),
            )
        )

        nearbyBusesViewModel.nearbyBusStops.observe(viewLifecycleOwner) {
            busListAdapter.busList.clear()

            val data = it.map {
                val lineId = it.lines.first()

                val line = busLinesViewModel.busLines.value?.find { it.id == lineId }
                val routes = busRoutesViewModel.busRoutes.value?.find { it.lineId == line?.id }
                val route = routes?.routes?.first()

                BusData(line?.displayName ?: "", it.displayName, route?.destinationName ?: "", "Now")
            }

            busListAdapter.busList.clear()
            busListAdapter.busList.addAll(data)
            busListAdapter.notifyDataSetChanged()
        }
    }
}