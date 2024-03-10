package com.cmd.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
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
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.cmd.myapplication.data.LatLngPoint
import com.cmd.myapplication.data.LatLngRect
import com.cmd.myapplication.data.Orientation
import com.cmd.myapplication.data.viewModels.BusDataViewModel
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.data.viewModels.NearbyBusesViewModel
import com.cmd.myapplication.data.viewModels.SearchViewModel
import com.cmd.myapplication.utils.MapUtils
import com.cmd.myapplication.utils.MapUtils.toLatLng
import com.cmd.myapplication.utils.Transitions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialElevationScale
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

    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }
    private val busDataViewModel: BusDataViewModel by activityViewModels { BusDataViewModel.Factory }
    private val nearbyBusStopsViewModel: NearbyBusesViewModel by activityViewModels { NearbyBusesViewModel.Factory }
    private val searchViewModel: SearchViewModel by activityViewModels { SearchViewModel.Factory }

    // map vars
    private var isMapInitialized = false
    private var shouldFollowDeviceLocation = false
    private var shouldFollowDeviceOrientation = false
    private lateinit var mapState: MapState
    private var mapMode = MapMode.DEFAULT

    private var deviceLocationMarker: Marker? = null
    private lateinit var deviceLocationIcon: Bitmap

    // views
    private lateinit var searchBarContainer: ConstraintLayout
    private lateinit var appIconView: ImageView
    private lateinit var expandButton: Button
    private lateinit var searchBar: TextInputLayout
    private lateinit var searchHintView: TextView

    private lateinit var mapView: SupportMapFragment

    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var bottomSheetDragHandleView: BottomSheetDragHandleView

    private lateinit var bottomSheetContentView: ConstraintLayout
    private lateinit var bottomSheetFragmentContainer: FragmentContainerView

    private lateinit var recenterButton: Button

    // state
    private var isBusListPeeking = true
    private lateinit var backPressedCallback: OnBackPressedCallback

    // transitions
    private lateinit var showSearchBarTransition: Transition
    private lateinit var hideSearchBarTransition: Transition

    private lateinit var showSearchHintTransition: Transition
    private lateinit var hideSearchHintTransition: Transition

    private lateinit var showRecenterButtonTransition: Transition
    private lateinit var hideRecenterButtonTransition: Transition

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialFade().apply {
            duration =
                5000//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
        }

        enterTransition = MaterialFade().apply {
            duration =
                5000//resources.getInteger(com.google.android.material.R.integer.m3_sys_motion_duration_long2).toLong()
            secondaryAnimatorProvider = null
        }

        reenterTransition = enterTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        requestData()

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
        searchHintView = view.findViewById(R.id.search_hint)

        mapView = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment

        bottomSheet = view.findViewById(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.peekHeight = (256 + 16).toDp(context)
        bottomSheetBehavior.isDraggable = true

        bottomSheetDragHandleView = view.findViewById(R.id.drag_handle)

        bottomSheetContentView = view.findViewById(R.id.bottom_sheet_content_view)
        bottomSheetFragmentContainer = view.findViewById(R.id.bottom_sheet_fragment_container)

        recenterButton = view.findViewById(R.id.recenter_button)

        searchBar.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                searchBar.isStartIconVisible = false
                searchBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        val deviceLocationIconDrawable = ResourcesCompat.getDrawable(
            resources, R.drawable.ic_device_location, requireContext().theme
        )!!.apply {
            bounds = Rect(
                0, 0,
                intrinsicWidth, intrinsicHeight
            )
        }

        deviceLocationIcon = Bitmap.createBitmap(
            deviceLocationIconDrawable.intrinsicWidth,
            deviceLocationIconDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        with(Canvas(deviceLocationIcon)) {
            deviceLocationIconDrawable.draw(this)
        }

        defineTransitions()

        searchBar.isEndIconVisible = false

        backPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            searchBar.isEndIconVisible = false

            if (isSearchFragmentOpen() && !searchBar.hasFocus()) {
                searchBar.editText?.editableText?.clear()
                searchBar.isStartIconVisible = false
                appIconView.visibility = View.VISIBLE
                closeSearchFragmentAnimated()
            }

            if (searchBar.hasFocus()) {
                hideKeyboard()
                searchBar.clearFocus()

                if (!isSearchFragmentOpen()) {
                    searchBar.editText?.editableText?.clear()
                    searchBar.isStartIconVisible = false
                    appIconView.visibility = View.VISIBLE
                }
            } else if (
                bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
                && !isSearchFragmentOpen()
            ) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                this.isEnabled = false
            }

            return@addCallback
        }.apply { isEnabled = false }

        searchBar.editText?.setOnFocusChangeListener { _, hasFocus ->
            sharedViewModel.searchBarHasFocus.value = hasFocus

            if (hasFocus) {
                backPressedCallback.isEnabled = true
                searchBar.isStartIconVisible = true
                appIconView.visibility = View.GONE

                if (searchBar.editText?.text?.isNotEmpty() == true) {
                    searchBar.isEndIconVisible = true
                }

                expandBusList()
            }
        }

        searchBar.editText?.setOnClickListener { expandBusList() }

        searchBar.setStartIconOnClickListener {
            searchBar.isEndIconVisible = false

            closeSearchFragmentAnimated()

            if (searchBar.hasFocus()) {
                hideKeyboard()
                searchBar.clearFocus()

                searchBar.editText?.editableText?.clear()
                searchBar.isStartIconVisible = false
                appIconView.visibility = View.VISIBLE
            }
        }

        searchBar.editText?.doOnTextChanged { text, start, before, count ->
            val textSize = text?.length ?: 0
            searchBar.isEndIconVisible = textSize > 0

            if (textSize > 0) {
                navigateToSearchFragmentAnimated()
            }

            if (text != null) {
                searchViewModel.searchText.value = text.toString()
            }

            expandBusList()
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
                } else if (text != null) {
                    searchViewModel.search(text.toString(), false)
                }
            }

            return@setOnEditorActionListener true
        }

        expandButton.setOnClickListener {
            hidePeekBusListButton()
            peekBusList()
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

                    sharedViewModel.bottomSheetState.value = newState
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    pState = newState
                    hasProcessedStateChange = false
                    backPressedCallback.isEnabled = true

                    sharedViewModel.bottomSheetState.value = newState
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    isBusListPeeking = false
                    showPeekBusListButton()

                    sharedViewModel.bottomSheetState.value = newState
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (offset == null) {
                    startOffset = bottomSheetContentView.top
                    // + 16dp margin
                    offset = searchBar.bottom.toFloat() + 16.toDp(context)
                    bottom = bottomSheetContentView.bottom
                    val lp = bottomSheetContentView.layoutParams as MarginLayoutParams

                    lp.height =
                        (bottom!! - offset!!).toInt() - bottomSheetDragHandleView.height

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
                    }
                } else if (pState == BottomSheetBehavior.STATE_EXPANDED && slideOffset < 1) {
                    // started to collapse
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true
                    }
                }

                val state = bottomSheetBehavior.state
                if (state == BottomSheetBehavior.STATE_DRAGGING || state == BottomSheetBehavior.STATE_SETTLING) {

                }
            }

        })

        mapView.getMapAsync { map ->
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isMapToolbarEnabled = false

            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            recenterButton.setOnClickListener {
                val cameraPosition =
                    map.cameraPosition.target.let { LatLngPoint(it.latitude, it.longitude) }

                val cameraBearing = map.cameraPosition.bearing

                val deviceLocation = deviceLocationViewModel.currentLocation.value
                val deviceOrientation = deviceLocationViewModel.currentOrientation.value

                if (deviceLocation != null && deviceOrientation != null) {
                    val isCentered = deviceLocation.approxEquals(cameraPosition)

                    if (isCentered) {
                        if (cameraBearing != 0f) {
                            Log.e(TAG, "map - setting north")

                            animateMap(
                                orientation = Orientation(0f, 0f, 0f),
                            )

                            mapState = mapState.copy(bearing = 0f)
                        } else if (mapMode == MapMode.DEFAULT) {
                            Log.e(TAG, "map - setting focused")

                            setMapModeFocused()
                        } else {
                            Log.e(TAG, "map - setting default")

                            setMapModeDefault()
                        }
                    } else {
                        Log.e(TAG, "map - centering")
                        animateMap(deviceLocation)
                    }
                }

                peekBusList()
            }
        }

        deviceLocationViewModel.currentLocation.observe(viewLifecycleOwner) { deviceLocation ->
            if (deviceLocation == null) {
                return@observe
            }

            val (
                latitude,
                longitude,
            ) = deviceLocation

            mapView.getMapAsync { map ->
                if (!isMapInitialized) {
                    isMapInitialized = true

                    mapState = MapState(
                        deviceLocation,
                        map.cameraPosition.bearing,
                        0f,
                        MapUtils.ZoomLevel.DEFAULT
                    )

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            MapUtils.ZoomLevel.DEFAULT
                        )
                    )
                }

                if (deviceLocationMarker == null) {
                    deviceLocationMarker = map.addMarker(MarkerOptions().apply {
                        position(
                            LatLng(
                                deviceLocation.lat,
                                deviceLocation.lng
                            ),
                        )
                        icon(BitmapDescriptorFactory.fromBitmap(deviceLocationIcon))
                        anchor(0.5f, 0.5f)
                        flat(true)
                    })
                }

                if (shouldFollowDeviceLocation) {
                    animateMap(deviceLocation)
//                    map.animateCamera(CameraUpdateFactory.newLatLng(deviceLocation.toLatLng()))
                }

                map.setOnCameraMoveStartedListener {
                    shouldFollowDeviceLocation = false
                    shouldFollowDeviceOrientation = false
                    hideBusList()
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

                    val isCentered = deviceLocation.approxEquals(cameraPosition)

                    shouldFollowDeviceLocation = isCentered && shouldFollowDeviceLocation
                    shouldFollowDeviceOrientation = isCentered && mapMode == MapMode.FOCUSED

                    if (!isCentered) {
                        showRecenterButton()
                    } else {
                        hideRecenterButton()
                    }
                }
            }
        }

        deviceLocationViewModel.currentOrientation.observe(viewLifecycleOwner) { orientation ->
            val deviceLocation = deviceLocationViewModel.currentLocation.value

            if (deviceLocation != null) {
                mapView.getMapAsync { map ->
                    if (deviceLocationMarker == null) {
                        return@getMapAsync
                    }

                    val marker = deviceLocationMarker!!
                    val rotation = MapUtils.calculateBearing(orientation)

                    marker.rotation = rotation

                    if (shouldFollowDeviceOrientation) {
                        Log.e(TAG, "animating map to device orientation - ${MapUtils.calculateBearing(orientation)}")
                        animateMap(orientation = orientation)
                    }
                }
            }
        }

        sharedViewModel.isBottomSheetDraggable.observe(viewLifecycleOwner) { isDraggable ->
            bottomSheetBehavior.isDraggable = isDraggable
        }

        sharedViewModel.bottomSheetState.observe(viewLifecycleOwner) {
            bottomSheetBehavior.state = it
        }

        sharedViewModel.isMainBackPressedCallbackEnabled.observe(viewLifecycleOwner) { isEnabled ->
            backPressedCallback.isEnabled = isEnabled
        }

        sharedViewModel.searchBarHasFocus.observe(viewLifecycleOwner) { hasFocus ->
            if (hasFocus) {
                searchBar.editText?.requestFocus()
            } else {
                searchBar.editText?.clearFocus()
            }
        }

        sharedViewModel.isInSearchMode.observe(viewLifecycleOwner) { inSearchMode ->
            setInSearchMode(inSearchMode)
        }
    }

    private fun setMapModeDefault() {
        val location = deviceLocationViewModel.currentLocation.value
        val orientation = deviceLocationViewModel.currentOrientation.value

        if (location != null && orientation != null) {
            mapMode = MapMode.DEFAULT

            shouldFollowDeviceOrientation = false
            animateMap(tilt = 0f, zoom = MapUtils.ZoomLevel.DEFAULT)
        }
    }

    private fun setMapModeFocused() {
        val location = deviceLocationViewModel.currentLocation.value
        val orientation = deviceLocationViewModel.currentOrientation.value

        if (location != null && orientation != null) {
            mapMode = MapMode.FOCUSED

            shouldFollowDeviceOrientation = true
            animateMap(tilt = 30f, zoom = MapUtils.ZoomLevel.FOCUSED)
        }
    }

    private fun animateMap(
        location: LatLngPoint? = null,
        orientation: Orientation? = null,
        tilt: Float? = null,
        zoom: Float? = null,
    ) {
        if (!::mapState.isInitialized) {
            return
        }

        mapView.getMapAsync { map ->
            val newLocation = location ?: mapState.location

            val newBearing = if (orientation != null) {
                MapUtils.calculateBearing(orientation)
            } else {
                mapState.bearing
            }

            val newTilt = tilt ?: mapState.tilt
            val newZoom = zoom ?: mapState.zoom

            mapState = MapState(
                newLocation,
                newBearing,
                newTilt,
                newZoom
            )

            val newCameraPosition = CameraPosition.Builder(map.cameraPosition)
                .target(newLocation.toLatLng())
                .bearing(newBearing)
                .tilt(newTilt)

            map.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition.build()))
        }
    }

    // search fragment navigation
    private fun isSearchFragmentOpen() =
        bottomSheetFragmentContainer.findNavController().currentDestination?.id != R.id.bottomSheetFragment

    private fun navigateToSearchFragmentAnimated() {
        if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            sharedViewModel.bottomSheetState.value = BottomSheetBehavior.STATE_EXPANDED
            sharedViewModel.bottomSheetOffset.observe(viewLifecycleOwner, object : Observer<Float> {
                override fun onChanged(value: Float) {
                    if (value == 1f) {
                        sharedViewModel.bottomSheetOffset.removeObserver(this)
                        navigateToSearchFragment()
                    }
                }
            })
        } else {
            navigateToSearchFragment()
        }
    }

    private fun navigateToSearchFragment() {
        if (bottomSheetFragmentContainer.findNavController().currentDestination?.id != R.id.searchFragment) {
            bottomSheetFragmentContainer.findNavController()
                .navigate(BottomSheetFragmentDirections.actionBottomSheetFragmentToSearchFragment())
        }
    }

    private fun closeSearchFragment() {
        if (isSearchFragmentOpen()) {
            bottomSheetFragmentContainer.findNavController()
                .navigateUp()
        }
    }

    private fun closeSearchFragmentAnimated() = Handler(Looper.getMainLooper()).postDelayed(
        {
            closeSearchFragment()
        }, 200
    )

    private fun hideBusList() {
        if (isBusListPeeking) {
            isBusListPeeking = false
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun peekBusList() {
        if (!isBusListPeeking) {
            isBusListPeeking = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun expandBusList() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showPeekBusListButton() {
        val fade = MaterialFade().apply {
            duration = 500
        }

        TransitionManager.beginDelayedTransition(view as ViewGroup, fade)
        expandButton.visibility = View.VISIBLE
    }

    private fun hidePeekBusListButton() {
        val fade = MaterialFade().apply {
            duration = 200
        }

        TransitionManager.beginDelayedTransition(view as ViewGroup, fade)
        expandButton.visibility = View.INVISIBLE
    }

    private fun setInSearchMode(inSearchMode: Boolean) {
        if (inSearchMode) {
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                hideSearchBarTransition
            )
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                showSearchHintTransition
            )

            searchBar.changeVisibilityForTransition(View.INVISIBLE)
            appIconView.changeVisibilityForTransition(View.INVISIBLE)
            searchHintView.changeVisibilityForTransition(View.VISIBLE)
        } else {
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                showSearchBarTransition
            )
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                hideSearchHintTransition
            )

            searchBar.changeVisibilityForTransition(View.VISIBLE)
            appIconView.changeVisibilityForTransition(View.VISIBLE)
            searchHintView.changeVisibilityForTransition(View.INVISIBLE)
        }
    }

    private fun showRecenterButton() {
        return

        if (recenterButton.visibility != View.VISIBLE) {
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                showRecenterButtonTransition
            )
            recenterButton.visibility = View.VISIBLE
        }
    }

    private fun hideRecenterButton() {
        return

        if (recenterButton.visibility != View.INVISIBLE) {
            TransitionManager.beginDelayedTransition(
                requireView() as ViewGroup,
                hideRecenterButtonTransition
            )
            recenterButton.visibility = View.INVISIBLE
        }
    }

    private fun defineTransitions() {
        showSearchBarTransition = MaterialFade().apply {
            addTarget(searchBar)
            addTarget(appIconView)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.ENTER)
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.ENTER)
        }

        hideSearchBarTransition = MaterialFade().apply {
            addTarget(searchBar)
            addTarget(appIconView)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.EXIT)
                .also { Log.e(TAG, "duration - $it") }
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.EXIT)
        }

        showSearchHintTransition = MaterialFade().apply {
            addTarget(searchHintView)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.ENTER)
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.ENTER)
        }

        hideSearchHintTransition = MaterialFade().apply {
            addTarget(searchHintView)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.EXIT)
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.EXIT)
        }

        showRecenterButtonTransition = MaterialElevationScale(true).apply {
            addTarget(recenterButton)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.ENTER)
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.ENTER)
        }

        hideRecenterButtonTransition = MaterialElevationScale(false).apply {
            addTarget(recenterButton)
            duration = Transitions.getDuration(requireContext(), Transitions.Type.EXIT)
            interpolator = Transitions.getInterpolator(requireContext(), Transitions.Type.EXIT)
        }
    }

    private fun View.changeVisibilityForTransition(visibility: Int) {
        if (this.visibility != visibility) {
            this.visibility = visibility
        }
    }

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

    private fun requestData() {
        deviceLocationViewModel.requestUpdates()
        busDataViewModel.requestData()

        deviceLocationViewModel.currentLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                nearbyBusStopsViewModel.location = it
            }
        }

        // observe all data instead of just bus stops to make sure that lines and routes are loaded
        busDataViewModel.data.observe(viewLifecycleOwner) {
            val (stops, lines, routes) = it

            nearbyBusStopsViewModel.busStops = stops
            searchViewModel.supplyBusData(stops, lines, routes)
        }
    }

    private data class MapState(
        val location: LatLngPoint,
        val bearing: Float,
        val tilt: Float,
        val zoom: Float,
    )

    private enum class MapMode {
        DEFAULT,
        FOCUSED,
    }
}