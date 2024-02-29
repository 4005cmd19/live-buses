package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.INVISIBLE
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup.VISIBLE
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialFade

private const val DISPLAY_MAX_STOPS = 10

/**
 * Fragment containing the map view, search bar and bus list
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    companion object {
        // for debug purposes
        const val TAG = "MainFragment"
    }

    private val sharedViewModel: SharedViewModel by activityViewModels { SharedViewModel.Factory }

    // provides the device's current location
    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }
    private val busLinesViewModel: BusLinesViewModel by activityViewModels { BusLinesViewModel.Factory }
    private val busStopsViewModel: BusStopsViewModel by activityViewModels { BusStopsViewModel.Factory }
    private val busRoutesViewModel: BusRoutesViewModel by activityViewModels { BusRoutesViewModel.Factory }

    private val nearbyBusStopsViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }

    private var shouldFollowDeviceLocation = true

    private lateinit var searchBarContainer: ConstraintLayout
    private lateinit var appIconView: ImageView
    private lateinit var expandButton: Button
    private lateinit var searchBar: TextInputLayout
    private var mapView: SupportMapFragment? = null

    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetCallback: BottomSheetCallback

    private lateinit var bottomSheetDragHandleView: BottomSheetDragHandleView

    private lateinit var bottomSheetContentView: ConstraintLayout
    private lateinit var compactBusListFragmentContainer: FragmentContainerView
    private lateinit var expandedBusListFragmentContainer: FragmentContainerView

    private lateinit var recenterButton: Button

    private var isBusListExpanded = true

    private var selectedStopView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        exitTransition = MaterialFade().apply {
            duration =
                500//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
        }

        reenterTransition = MaterialFade().apply {
            duration =
                500//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
        }

        postponeEnterTransition()
        view.doOnPreDraw {
            Log.w("STATE_VM", "bss - ${bottomSheetBehavior.state}")
            startPostponedEnterTransition()
        }

        // content layout contains search bar and bus list
        // separated from rest of layout so that window insets can be applied to it and not the map view

        // val
        searchBarContainer = view.findViewById(R.id.content_view)

        // app icon aligned with search bar
        appIconView = view.findViewById(R.id.app_icon_view)

        expandButton = view.findViewById(R.id.expand_bottom_sheet_button)

        // apply window insets to content view
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            searchBarContainer.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }

            expandButton.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = insets.bottom
            }

            return@setOnApplyWindowInsetsListener WindowInsetsCompat(windowInsets)
        }

        searchBar = view.findViewById(R.id.search_bar)

        mapView = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?

        bottomSheet = view.findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = (256 + 16).toDp(context)

        bottomSheetDragHandleView = view.findViewById(R.id.drag_handle)

        bottomSheetContentView = view.findViewById(R.id.bottom_sheet_content_view)
        compactBusListFragmentContainer = view.findViewById(R.id.compact_bus_list_fragment)
        expandedBusListFragmentContainer = view.findViewById(R.id.expanded_bus_list_fragment)

        recenterButton = view.findViewById(R.id.recenter_button)

        searchBar.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                searchBar.isStartIconVisible = false
                searchBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        searchBar.isEndIconVisible = false

        val backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            searchBar.isEndIconVisible = false

            if (searchBar.hasFocus()) {
                hideKeyboard()
                searchBar.clearFocus()

                val hasSearchText = searchBar.editText?.text?.isNotEmpty() ?: false

                if (!hasSearchText) {
                    searchBar.editText?.text = null
                    searchBar.isStartIconVisible = false
                    appIconView.visibility = View.VISIBLE
                }
            } else {
                searchBar.editText?.text = null
                searchBar.isStartIconVisible = false
                appIconView.visibility = View.VISIBLE
                this.isEnabled = false
            }

            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                Log.w("STATE_VM", "collapsing bs")
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            return@addCallback
        }.apply { isEnabled = false }

        searchBar.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                backPressedCallback.isEnabled = true
                searchBar.isStartIconVisible = true
                appIconView.visibility = View.GONE

                if (searchBar.editText?.text?.isNotEmpty() == true) {
                    searchBar.isEndIconVisible = true
                }

                expandSearchBar()
            }
        }

        searchBar.editText?.setOnClickListener { expandSearchBar() }

        searchBar.setStartIconOnClickListener {
            searchBar.isEndIconVisible = false

            if (searchBar.hasFocus()) {
                hideKeyboard()
                searchBar.clearFocus()

                val hasSearchText = searchBar.editText?.text?.isNotEmpty() ?: false

                if (!hasSearchText) {
                    searchBar.editText?.text = null
                    searchBar.isStartIconVisible = false
                    appIconView.visibility = View.VISIBLE
                }
            } else {
                // TODO set to change to app icon later
                searchBar.editText?.text = null
                searchBar.isStartIconVisible = false
                appIconView.visibility = View.VISIBLE
            }
        }

        searchBar.editText?.doOnTextChanged { text, start, before, count ->
            searchBar.isEndIconVisible = count > 0

            expandSearchBar()
        }

        searchBar.editText?.setOnEditorActionListener { _, actionId, keyEvent ->
            if (keyEvent == null && actionId == EditorInfo.IME_ACTION_DONE) {
                // enter key
                hideKeyboard()
                searchBar.clearFocus()
                searchBar.isEndIconVisible = false

                val text = searchBar.editText?.text

                if (text?.isEmpty() == true) {
                    searchBar.isStartIconVisible = false
                    appIconView.visibility = View.VISIBLE
                }
            }

            return@setOnEditorActionListener true
        }

        expandButton.setOnClickListener {
            hideExpandBusListButton()
//            expandBusList() // TODO aaaa
        }

        // ----- bottom sheet behaviour -----
        // shift content to accommodate for search bar
        // get offset on slide when search bar is guaranteed to be measured
        var startOffset: Int? = null
        var offset: Float? = null
        var bottom: Int? = null


        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            var pState = bottomSheetBehavior.state

            // use to avoid unnecessarily setting view visibility
            // if false change visibility
            var hasProcessedStateChange = false

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    pState = newState
                    hasProcessedStateChange = false

                    compactBusListFragmentContainer.visibility = View.VISIBLE
                    expandedBusListFragmentContainer.visibility = View.INVISIBLE

                    bottomSheetBehavior.isDraggable = true
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    pState = newState
                    hasProcessedStateChange = false

                    Log.e("STATE_VM", "state changed - expanded")

                    compactBusListFragmentContainer.visibility = INVISIBLE
                    expandedBusListFragmentContainer.visibility = VISIBLE

                    // ----

                    bottomSheetBehavior.isDraggable = false

                    backPressedCallback.isEnabled = true
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    isBusListExpanded = false
                    showExpandBusListButton()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (offset == null) {
                    // + 16dp margin
                    startOffset = bottomSheetContentView.top
                    offset = searchBar.bottom.toFloat() + 16.toDp(context)
                    bottom = bottomSheetContentView.bottom
                    val lp = bottomSheetContentView.layoutParams as MarginLayoutParams

                    Log.e(TAG, "y - ${bottomSheetContentView.y}")

                    // TODO replaced with fragments
//                    lp.height =
//                        (bottom!! - offset!! + 16.toDp(context)).toInt() - bottomSheetHeadingView.height - bottomSheetDragHandleView.height

                    lp.height =
                        (bottom!! - offset!!).toInt() - bottomSheetDragHandleView.height
                    // ----
                    bottomSheetContentView.layoutParams = lp
                }

                val sOffset = (slideOffset * offset!!).toInt()
                bottomSheetContentView.y = (startOffset!! + sOffset).toFloat()

                bottomSheetDragHandleView.alpha = (1 - slideOffset)

                if (pState == BottomSheetBehavior.STATE_COLLAPSED && slideOffset > 0) {
                    // started to expand
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true

                        // TODO replaced with fragments
                        expandedBusListFragmentContainer.visibility = VISIBLE
                        expandedBusListFragmentContainer.alpha = 0f
                        // ----
                    }
                } else if (pState == BottomSheetBehavior.STATE_EXPANDED && slideOffset < 1) {
                    // started to collapse
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true

                        // TODO replaced with fragments
                        compactBusListFragmentContainer.visibility = VISIBLE
                        compactBusListFragmentContainer.alpha = 0f
                        // ----
                    }
                }

                val state = bottomSheetBehavior.state
                if (state == BottomSheetBehavior.STATE_DRAGGING || state == BottomSheetBehavior.STATE_SETTLING) {
                    // TODO replaced with fragments
                    compactBusListFragmentContainer.alpha = 1 - slideOffset
                    expandedBusListFragmentContainer.alpha = slideOffset
                    // ----
                }
            }
        }.also { bottomSheetCallback = it })

        restoreState()

        deviceLocationViewModel.currentLocation.observe(viewLifecycleOwner) { deviceLocation ->
            if (deviceLocation != null) {
                val (
                    latitude,
                    longitude,
                ) = deviceLocation

                mapView?.getMapAsync { map ->

                    // recenter map
                    recenterButton.setOnClickListener {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    deviceLocation.lat,
                                    deviceLocation.lng,
                                ),
                                10f
                            )
                        )

//                        recenterButton.hide()
                        //expandBusList() TODO aaaa
                    }

                    //
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            10f
                        )
                    )

                    map.setOnCameraMoveStartedListener {
                        Log.w("STATE_VM", "moved")
//                        collapseBusList() // TODO aaaaaaa
                    }

                    map.setOnCameraIdleListener {
                        val cameraPosition = LatLngPoint(
                            map.cameraPosition.target.latitude,
                            map.cameraPosition.target.longitude
                        )

                        val cameraBoundedArea = map.projection.visibleRegion.latLngBounds
                        val swLatLng = cameraBoundedArea.southwest
                        val neLatLng = cameraBoundedArea.northeast

                        val area = LatLngRect(
                            LatLngPoint(swLatLng.latitude, swLatLng.longitude),
                            LatLngPoint(neLatLng.latitude, neLatLng.longitude)
                        )

                        shouldFollowDeviceLocation =
                            shouldFollowDeviceLocation && deviceLocation in area


                        val isCentered =
                            deviceLocation == cameraPosition //deviceLocation.approxEquals(cameraPosition)
                        Log.e(
                            TAG,
                            "dl - $deviceLocation $cameraPosition eq=${
                                deviceLocation.approxEquals(cameraPosition)
                            }"
                        )

                        if (!isCentered) {//deviceLocation != cameraPosition) {

                            val transition = MaterialFade().apply {
                                duration = 200
                            }

                            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
                            recenterButton.visibility = View.VISIBLE
                        } else {
                            val transition = MaterialFade().apply {
                                duration = 500
                            }

                            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
                            recenterButton.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        }

        nearbyBusStopsViewModel.selectedBusStop.observe(viewLifecycleOwner) {
            if (it == null) {
                if (selectedStopView != null) {
                    selectedStopView = null
                }
            } else {
                val (id, stopView) = it

                selectedStopView = stopView

                val transitionName = "expand_stop_transition"

                val extras = FragmentNavigatorExtras(
                    stopView to transitionName,
                )

                sharedViewModel.saveState(this@MainFragment, Bundle().apply {
                    putInt("bottomSheetState", bottomSheetBehavior.state)
                })

                findNavController().navigate(
                    MainFragmentDirections.actionMainFragmentToStopFragment(id),
                    extras
                )
            }
        }
    }

    private fun restoreState() {
        val state = sharedViewModel.restoreState(this)

        Log.w("STATE_VM", "state - $state")

        state?.let {
            it.apply {
                val bottomSheetState = getInt("bottomSheetState")

                Log.w("STATE_VM", "applying bottom sheet state - $bottomSheetState")

                bottomSheetCallback.onSlide(bottomSheet, 1f)
                bottomSheetBehavior.state = bottomSheetState
//                bottomSheetCallback.onStateChanged(bottomSheet, bottomSheetBehavior.state)
            }
        }
    }

    private fun applyExpandedOffset () {
        val startOffset = bottomSheetContentView.top
        val offset = searchBar.bottom.toFloat() + 16.toDp(context)
        val bottom = bottomSheetContentView.bottom
        val lp = bottomSheetContentView.layoutParams as MarginLayoutParams

        Log.e(TAG, "y - ${bottomSheetContentView.y}")

        // TODO replaced with fragments
//                    lp.height =
//                        (bottom!! - offset!! + 16.toDp(context)).toInt() - bottomSheetHeadingView.height - bottomSheetDragHandleView.height

        lp.height =
            (bottom - offset).toInt() - bottomSheetDragHandleView.height
        // ----
        bottomSheetContentView.layoutParams = lp

        bottomSheetContentView.y = offset
    }

    private fun collapseBusList() {
        if (isBusListExpanded) {
            isBusListExpanded = false
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun expandBusList() {
        if (!isBusListExpanded) {
            isBusListExpanded = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun showExpandBusListButton() {
        val fade = MaterialFade().apply {
            duration = 500
        }

        TransitionManager.beginDelayedTransition(view as ViewGroup, fade)
        expandButton.visibility = View.VISIBLE
    }

    private fun hideExpandBusListButton() {
        val fade = MaterialFade().apply {
            duration = 200
        }

        TransitionManager.beginDelayedTransition(view as ViewGroup, fade)
        expandButton.visibility = View.INVISIBLE
    }

    private fun expandSearchBar() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun collapseSearchBar() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

//    private fun showStopFragment(view: View) {
//        val transform = MaterialContainerTransform().apply {
//            startView = view
//            endView = stopFragmentContainer
//            scrimColor = Color.TRANSPARENT
//            duration = 500
//            addTarget(stopFragmentContainer)
//
//            interpolator = MotionUtils.resolveThemeInterpolator(
//                requireContext(),
//                com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
//                FastOutSlowInInterpolator()
//            )
//        }
//
//        transform.addListener(object : TransitionListener {
//            override fun onTransitionStart(transition: Transition) {
//                stopFragmentContainer.visibility = View.VISIBLE
//            }
//
//            override fun onTransitionEnd(transition: Transition) {
//                view.visibility = View.INVISIBLE
//            }
//
//            override fun onTransitionCancel(transition: Transition) {
//
//            }
//
//            override fun onTransitionPause(transition: Transition) {
//
//            }
//
//            override fun onTransitionResume(transition: Transition) {
//
//            }
//
//        })
//
//        TransitionManager.beginDelayedTransition(view as ViewGroup, transform)
//    }
//
//    private fun hideStopFragment(view: View) {
//        val transform = MaterialContainerTransform().apply {
//            startView = stopFragmentContainer
//            endView = view
//            scrimColor = Color.TRANSPARENT
//            duration = 200
//
//            addTarget(view)
//
//            interpolator = MotionUtils.resolveThemeInterpolator(
//                requireContext(),
//                com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
//                FastOutSlowInInterpolator()
//            )
//        }
//
//        transform.addListener(object : TransitionListener {
//            override fun onTransitionStart(transition: Transition) {
//                view.visibility = View.VISIBLE
//            }
//
//            override fun onTransitionEnd(transition: Transition) {
//                stopFragmentContainer.visibility = View.INVISIBLE
//            }
//
//            override fun onTransitionCancel(transition: Transition) {
//
//            }
//
//            override fun onTransitionPause(transition: Transition) {
//
//            }
//
//            override fun onTransitionResume(transition: Transition) {
//
//            }
//
//        })
//
//        TransitionManager.beginDelayedTransition(view as ViewGroup, transform)
//    }

    private fun showKeyboard() {
        val view = requireView()

        val keyboardManager = requireActivity().getSystemService(InputMethodManager::class.java)
        keyboardManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val view = requireView()

        val keyboardManager = requireActivity().getSystemService(InputMethodManager::class.java)
        keyboardManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}