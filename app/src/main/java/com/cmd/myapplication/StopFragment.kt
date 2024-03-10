package com.cmd.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.cmd.myapplication.data.viewModels.BusDataViewModel
import com.cmd.myapplication.utils.adapters.LinePagerAdapter
import com.google.android.material.motion.MotionUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFade

class StopFragment : Fragment(R.layout.fragment_stop) {
    private val args: StopFragmentArgs by navArgs()
    private val sharedViewModel: SharedViewModel by activityViewModels { SharedViewModel.Factory }

    private val busDataViewModel: BusDataViewModel by activityViewModels { BusDataViewModel.Factory }

    private lateinit var stopNameView: TextView
    private lateinit var closeButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var lineViewPager: ViewPager2

    private lateinit var lineViewPagerAdapter: LinePagerAdapter

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transformDuration = MotionUtils.resolveThemeDuration(
            requireContext(),
            com.google.android.material.R.attr.motionDurationLong2,
            500
        ).toLong()
        //resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2)

        val transformInterpolator = MotionUtils.resolveThemeInterpolator(
            requireContext(),
            com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
            FastOutSlowInInterpolator()
        )

        val enterDuration = MotionUtils.resolveThemeDuration(
            requireContext(),
            com.google.android.material.R.attr.motionDurationMedium4,
            400
        ).toLong()

        val exitDuration = MotionUtils.resolveThemeDuration(
            requireContext(),
            com.google.android.material.R.attr.motionDurationShort4,
            200
        ).toLong()

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.bus_list_fragment_container

            duration = transformDuration
            interpolator = transformInterpolator

            scrimColor = Color.TRANSPARENT
            containerColor = Color.TRANSPARENT
            startContainerColor = Color.TRANSPARENT
            endContainerColor = Color.TRANSPARENT
        }

        enterTransition = MaterialFade().apply {
            duration = enterDuration
        }

        exitTransition = MaterialFade().apply {
            duration = exitDuration
        }

        reenterTransition = enterTransition
        returnTransition = exitTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        ViewCompat.setOnApplyWindowInsetsListener(view,
            OnApplyWindowInsetsListener { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                view.updateLayoutParams<MarginLayoutParams> {
//                    topMargin = insets.top + 32.toDp(context)
//                    bottomMargin = insets.bottom + 32.toDp(context)
                }

                return@OnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
            })

        stopNameView = view.findViewById(R.id.stop_name_view)
        closeButton = view.findViewById(R.id.close_button)
        tabLayout = view.findViewById(R.id.tab_layout)
        lineViewPager = view.findViewById(R.id.line_view_pager)

        lineViewPagerAdapter = LinePagerAdapter(fragment = this)

        lineViewPager.offscreenPageLimit = 100
        lineViewPager.adapter = lineViewPagerAdapter

        val stop = busDataViewModel.busStops.value?.find { it.id == args.stopId }
        stopNameView.text = stop?.displayName

        val lines = busDataViewModel.busLines.value?.filter { it.stops.contains(stop?.id) }
        val lineNames = lines?.map { it.displayName }

        lineViewPagerAdapter.lineIds.addAll(lines?.map { it.id } ?: emptyList())
        lineViewPagerAdapter.notifyItemRangeInserted(0, lines?.size ?: 0)

        TabLayoutMediator(tabLayout, lineViewPager) { tab, position ->
            tab.text = lineNames?.get(position)
        }.attach()

        // apply margins
        tabLayout.getChildAt(0).let { it as ViewGroup }
            .let {
                it.getChildAt(0).apply {
                    updateLayoutParams<MarginLayoutParams> {
                        leftMargin = 16.toDp(context)
                    }
                }

                it.getChildAt(it.childCount - 1).apply {
                    updateLayoutParams<MarginLayoutParams> {
                        rightMargin = 16.toDp(context)
                    }
                }
            }

        if (args.lineId != null) {
            tabLayout.getTabAt(lines?.indexOfFirst { it.id == args.lineId } ?: 0)?.select()
        }

        closeButton.setOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.isBottomSheetDraggable.value = false
        sharedViewModel.isMainBackPressedCallbackEnabled.value = false
        sharedViewModel.isInSearchMode.value = true

        onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().navigateUp()
        }
        onBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        onBackPressedCallback.isEnabled = false

        super.onDestroyView()
    }

    companion object {
        const val TAG = "StopFragment"
    }
}