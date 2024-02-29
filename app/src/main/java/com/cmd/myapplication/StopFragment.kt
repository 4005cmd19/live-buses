package com.cmd.myapplication

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.utils.LinePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialContainerTransform

/**
 * A simple [Fragment] subclass.
 * Use the [StopFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StopFragment : Fragment(R.layout.fragment_stop) {
    private val args: StopFragmentArgs by navArgs()

    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val nearbyBusesViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }

    private lateinit var stopNameView: TextView
    private lateinit var closeButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var lineViewPager: ViewPager2

    private lateinit var lineViewPagerAdapter: LinePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigateUp()
        }

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 500
            startDelay = 500
            scrimColor = Color.TRANSPARENT
            containerColor = Color.TRANSPARENT
            startContainerColor = Color.TRANSPARENT
            endContainerColor = Color.TRANSPARENT
        }
    }

    override fun onResume() {
        super.onResume()

        Log.e("STOP", "resume")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        ViewCompat.setOnApplyWindowInsetsListener(view,
            OnApplyWindowInsetsListener { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                view.updateLayoutParams<MarginLayoutParams> {
                    topMargin = insets.top + 32.toDp(context)
                    bottomMargin = insets.bottom + 32.toDp(context)
                }

                return@OnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
            })

        stopNameView = view.findViewById(R.id.stop_name_view)
        closeButton = view.findViewById(R.id.close_button)
        tabLayout = view.findViewById(R.id.tab_layout)
        lineViewPager = view.findViewById(R.id.line_view_pager)

        lineViewPagerAdapter = LinePagerAdapter(fragment = this)
        lineViewPager.adapter = lineViewPagerAdapter

        val stop = busStopsViewModel.busStops.value?.find { it.id == args.stopId }
        stopNameView.text = stop?.displayName

        val lines = busLinesViewModel.busLines.value?.filter { it.stops.contains(stop?.id) }
        val lineNames = lines?.map { it.displayName }

        TabLayoutMediator(tabLayout, lineViewPager) { tab, position ->
            tab.text = lineNames?.get(position)
        }.attach()

        closeButton.setOnClickListener {
            navigateUp()
        }
    }

    private fun navigateUp() {
        nearbyBusesViewModel.selectedBusStop.value = null
        findNavController().navigateUp()
    }
}