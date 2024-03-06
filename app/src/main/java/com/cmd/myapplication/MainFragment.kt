package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.transition.TransitionManager
import com.cmd.myapplication.data.BusLine
import com.cmd.myapplication.data.BusLineRoute
import com.cmd.myapplication.data.BusLineRoutes
import com.cmd.myapplication.data.BusStop
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.Locality
import com.cmd.myapplication.data.test.TestDataProvider
import com.cmd.myapplication.data.viewModels.BusLinesViewModel
import com.cmd.myapplication.data.viewModels.BusRoutesViewModel
import com.cmd.myapplication.data.viewModels.BusStopsViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.data.viewModels.SearchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialFade
import kotlin.random.Random

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

    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    private var shouldFollowDeviceLocation = true

    private lateinit var searchBarContainer: ConstraintLayout
    private lateinit var appIconView: ImageView
    private lateinit var expandButton: Button
    private lateinit var searchBar: TextInputLayout
    private var mapView: SupportMapFragment? = null

    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var bottomSheetDragHandleView: BottomSheetDragHandleView

    private lateinit var bottomSheetContentView: ConstraintLayout
    private lateinit var bottomSheetFragmentContainer: FragmentContainerView

    private lateinit var recenterButton: Button

    private var isBusListExpanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

//        provideTestData()

        val testDataProvider = TestDataProvider(
            busStopsViewModel,
            busLinesViewModel,
            busRoutesViewModel
        )

        val (stops, lines, routes) = testDataProvider.generateTestData()
        testDataProvider.publishTestData(stops, lines, routes)

        exitTransition = MaterialFade().apply {
            duration =
                5000//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
        }

        reenterTransition = MaterialFade().apply {
            duration =
                5000//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
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
        bottomSheetBehavior.isDraggable = true

        bottomSheetDragHandleView = view.findViewById(R.id.drag_handle)

        bottomSheetContentView = view.findViewById(R.id.bottom_sheet_content_view)
        bottomSheetFragmentContainer = view.findViewById(R.id.bottom_sheet_fragment_container)

        recenterButton = view.findViewById(R.id.recenter_button)
//        recenterButton.hide()
//        recenterButton.extend()

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

        bottomSheetFragmentContainer.findNavController().also { Log.e("NAV_CONTROLLER", "n - ${it.currentDestination?.displayName}") }

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

            Log.e(TAG, "count - $count")

            val textSize = text?.length ?: 0
            if (textSize > 0) {
                searchViewModel.search(
                    text.toString(),
                    deviceLocationViewModel.currentLocation.value!!,
                    Locality.COVENTRY.location,
                )
                navigateToStopFragment()
            }
            else {
                sharedViewModel.isSearchFragmentVisible.value = false
            }

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
            expandBusList()
        }

        // ----- bottom sheet behaviour -----
        // shift content to accommodate for search bar
        // get offset on slide when search bar is guaranteed to be measured
        var startOffset: Int? = null
        var offset: Float? = null
        var bottom: Int? = null

        bottomSheetFragmentContainer.setOnTouchListener { view, motionEvent -> false }

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            var pState = bottomSheetBehavior.state

            // use to avoid unnecessarily setting view visibility
            // if false change visibility
            var hasProcessedStateChange = false

            override fun onStateChanged(bottomSheet: View, newState: Int) {

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    pState = newState
                    hasProcessedStateChange = false

//                    compactBusListFragmentContainer.visibility = View.VISIBLE
//                    expandedBusListFragmentContainer.visibility = View.INVISIBLE

//                    bottomSheetBehavior.isDraggable = true
//                    sharedViewModel.isBottomSheetScrollable.value = false
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    pState = newState
                    hasProcessedStateChange = false

//                    bottomSheetBehavior.isDraggable = false
//                    sharedViewModel.isBottomSheetScrollable.value = true

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

                sharedViewModel.bottomSheetOffset.value = slideOffset

                val sOffset = (slideOffset * offset!!).toInt()
                bottomSheetContentView.y = (startOffset!! + sOffset).toFloat()

                bottomSheetDragHandleView.alpha = (1 - slideOffset)

                if (pState == BottomSheetBehavior.STATE_COLLAPSED && slideOffset > 0) {
                    // started to expand
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true

                        // TODO replaced with fragments
//                        expandedBusListFragmentContainer.visibility = VISIBLE
//                        expandedBusListFragmentContainer.alpha = 0f
                        // ----
                    }
                } else if (pState == BottomSheetBehavior.STATE_EXPANDED && slideOffset < 1) {
                    // started to collapse
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true

                        // TODO replaced with fragments
//                        compactBusListFragmentContainer.visibility = VISIBLE
//                        compactBusListFragmentContainer.alpha = 0f
                        // ----
                    }
                }

                val state = bottomSheetBehavior.state
                if (state == BottomSheetBehavior.STATE_DRAGGING || state == BottomSheetBehavior.STATE_SETTLING) {
                    // TODO replaced with fragments
//                    compactBusListFragmentContainer.alpha = 1 - slideOffset
//                    expandedBusListFragmentContainer.alpha = slideOffset
                    // ----
                }
            }

        })

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
                        expandBusList()
                    }

                    //
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            10f
                        )
                    )

                    map.setOnCameraMoveStartedListener {
                        collapseBusList()
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

                        if (deviceLocation != cameraPosition) {
//                            recenterButton.show()
                        }
                    }
                }
            }
        }

        sharedViewModel.bottomSheetState.observe(viewLifecycleOwner) {
            bottomSheetBehavior.state = it
        }

        sharedViewModel.isBottomSheetDraggable.observe(viewLifecycleOwner) {
            bottomSheetBehavior.isDraggable = it
            Log.e(TAG, "isDraggable - $it")
        }
    }

    private fun navigateToStopFragment () {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
            sharedViewModel.bottomSheetOffset.observe(viewLifecycleOwner, object : Observer<Float> {
                override fun onChanged(value: Float) {
                    if (value == 1f) {
                        sharedViewModel.bottomSheetOffset.removeObserver(this)
                        sharedViewModel.isSearchFragmentVisible.value = true
                    }
                }
            })
        }
        else {
            sharedViewModel.isSearchFragmentVisible.value = true
        }
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

    private var hasTestData = false

    private fun provideTestData() {
        if (hasTestData) return
        hasTestData = true

        val stops = setOf(
            BusStop(
                "stopId0",
                "stopCode0",
                "Stop 1",
                randomLocation(),
                setOf("lineId2", "lineId5")
            ),
            BusStop(
                "stopId1",
                "stopCode1",
                "Stop 2",
                randomLocation(),
                setOf("lineId0", "lineId4", "lineId7")
            ),
            BusStop(
                "stopId2",
                "stopCode2",
                "Stop 3",
                randomLocation(),
                setOf("lineId1", "lineId6")
            ),
            BusStop(
                "stopId3",
                "stopCode3",
                "Stop 4",
                randomLocation(),
                setOf("lineId0", "lineId6")
            ),
            BusStop(
                "stopId4",
                "stopCode4",
                "Stop 5",
                randomLocation(),
                setOf("lineId8", "lineId1")
            ),
            BusStop(
                "stopId5",
                "stopCode5",
                "Stop 6",
                randomLocation(),
                setOf("lineId11", "lineId9")
            ),
            BusStop(
                "stopId6",
                "stopCode6",
                "Stop 7",
                randomLocation(),
                setOf("lineId3", "lineId10")
            ),
            BusStop(
                "stopId7",
                "stopCode7",
                "Stop 8",
                randomLocation(),
                setOf("lineId7", "lineId3")
            ),
            BusStop(
                "stopId8",
                "stopCode8",
                "Stop 9",
                randomLocation(),
                setOf("lineId9", "lineId11")
            ),
        )

        val lines = setOf(
            BusLine("lineId0", "X10", setOf(), setOf("stopId1", "stopId3"), setOf()),
            BusLine("lineId1", "2A", setOf(), setOf("stopId2", "stopId4"), setOf()),
            BusLine("lineId2", "2", setOf(), setOf("stopId0"), setOf()),
            BusLine("lineId3", "9X", setOf(), setOf("stopId6", "stopId7"), setOf()),
            BusLine("lineId4", "4W", setOf(), setOf("stopId1"), setOf()),
            BusLine("lineId5", "Y2", setOf(), setOf("stopId5"), setOf()),
            BusLine("lineId6", "91", setOf(), setOf("stopId2", "stopId3"), setOf()),
            BusLine("lineId7", "134", setOf(), setOf("stopId1", "stopId7"), setOf()),
            BusLine("lineId8", "43", setOf(), setOf("stopId4"), setOf()),
            BusLine("lineId9", "W10", setOf(), setOf("stopId5", "stopId8"), setOf()),
            BusLine("lineId10", "69", setOf(), setOf("stopId6"), setOf()),
            BusLine("lineId11", "261", setOf(), setOf("stopId5", "stopId8"), setOf()),
        )

        val routes =
            lines.map {
                val lineRoutes = BusLineRoutes(
                    it.id, setOf(
                        BusLineRoute(
                            "routeId0",
                            "Muswell Hill to Archway",
                            "0",
                            "Muswell Hill Broadway",
                            "0",
                            "Archway Station",
                            BusLineRoute.Direction.OUTBOUND,
                            arrayOf()
                        ),
                        BusLineRoute(
                            "routeId1",
                            "Archway to Muswell Hill",
                            "0",
                            "Archway Station",
                            "0",
                            "Muswell Hill Broadway",
                            BusLineRoute.Direction.INBOUND,
                            arrayOf()
                        ),
                        BusLineRoute(
                            "routeId2",
                            "Muswell Hill to North Finchley",
                            "0",
                            "Muswell Hill Broadway",
                            "0",
                            "Woodhouse College",
                            BusLineRoute.Direction.OUTBOUND,
                            arrayOf()
                        ),
                        BusLineRoute(
                            "routeId3",
                            "North Finchely to Muswell Hill",
                            "0",
                            "Woodhouse College",
                            "0",
                            "Muswell Hill Broadway",
                            BusLineRoute.Direction.INBOUND,
                            arrayOf()
                        ),
                    )
                )

                val routeIds = lineRoutes.routes.map { it.id }.toSet()

                it.routes = routeIds

                lineRoutes
            }.toSet()

        busStopsViewModel.debugSet(stops)
        busLinesViewModel.debugSet(lines)
        busRoutesViewModel.debugSet(routes)
    }

    private fun randomLocation(): LatLngPoint {
        val bounds = Locality.COVENTRY.location

        val rLat = Random.nextDouble(bounds.southwest.lat, bounds.northeast.lat)
        val rLng = Random.nextDouble(bounds.southwest.lng, bounds.northeast.lng)

        return LatLngPoint(rLat, rLng)
    }
}