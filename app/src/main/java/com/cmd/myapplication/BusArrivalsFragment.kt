package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.adapters.ArrivalData
import com.cmd.myapplication.utils.adapters.ArrivalsListAdapter

/**
 * A simple [Fragment] subclass.
 * Use the [BusArrivalsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BusArrivalsFragment : Fragment(R.layout.fragment_bus_arrivals) {
    private val nearbyBusesViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }

    private lateinit var arrivalsListView: RecyclerView
    private val arrivalsListAdapter = ArrivalsListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arrivalsListView = view.findViewById(R.id.arrivals_list_view)
        arrivalsListView.adapter = arrivalsListAdapter

        Log.e("StopFragment", "view created")

        val lineId = arguments?.takeIf { it.containsKey("lineId") }?.getString("lineId")

        arrivalsListAdapter.arrivalsList.addAll(
            arrayOf(
                ArrivalData("Muswell Hill to Archway", "Archway", "Now"),
                ArrivalData("Archway to Muswell Hill", "Muswell Hill", "3 mins"),
                ArrivalData("Muswell Hill to North Finchley", "Finchley", "5 mins"),
                ArrivalData("North Finchley to Muswell Hill", "Muswell Hill", "17:37"),
            )
        )
    }
}