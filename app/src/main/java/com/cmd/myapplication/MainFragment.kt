package com.cmd.myapplication

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.cmd.myapplication.data.repositories.BusRoutePoints
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.cmd.myapplication.utils.BusInfo
import com.cmd.myapplication.utils.BusListAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * Fragment containing the map view, search bar and bus list
 */
class MainFragment : Fragment(R.layout.fragment_main) {
    companion object {
        // for debug purposes
        const val TAG = "MainFragment"
    }

    // provides the device's current location
    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // content layout contains search bar and bus list
        // separated from rest of layout so that window insets can be applied to it and not the map view
        val content: ConstraintLayout = view.findViewById(R.id.content_view)

        // app icon aligned with search bar
        val appIconView: ImageView = view.findViewById(R.id.app_icon_view)

        // apply window insets to content view
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            content.updateLayoutParams<ViewGroup.MarginLayoutParams> {
//                leftMargin = insets.left + 16.toDp(context)
                topMargin = insets.top
                bottomMargin = insets.bottom
//                rightMargin = insets.right + 16.toDp(context)
            }

            return@setOnApplyWindowInsetsListener WindowInsetsCompat.CONSUMED
        }

        val searchBar: TextInputLayout = view.findViewById(R.id.search_bar)

        val mapView = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?

        val bottomSheet: ConstraintLayout = view.findViewById(R.id.bottom_sheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.peekHeight = (256 + 16).toDp(context)

        val bottomSheetDragHandleView: BottomSheetDragHandleView =
            view.findViewById(R.id.drag_handle)

        val bottomSheetHeadingView: TextView = view.findViewById(R.id.bottom_sheet_heading_view)

        val bottomSheetContentView: ConstraintLayout =
            view.findViewById(R.id.bottom_sheet_content_view)

        val compactBusListView: RecyclerView = view.findViewById(R.id.compact_bus_list_view)
        val expandedBusListView: RecyclerView = view.findViewById(R.id.expanded_bus_list_view)

        // TODO for debugging purposes, remove later
        compactBusListView.adapter = BusListAdapter(
            arrayOf(
                BusInfo("Route 1", "Now"),
                BusInfo("Route 2", "Later"),
                BusInfo("Route 3", "Even later")
            )
        )
        expandedBusListView.adapter = BusListAdapter(
            arrayOf(
                BusInfo("Route 1", "Now"),
                BusInfo("Route 2", "Later"),
                BusInfo("Route 3", "Even later"),
                BusInfo("Route 4", "Even later"),
                BusInfo("Route 5", "Even later"),
                BusInfo("Route 6", "Even later"),
                BusInfo("Route 7", "Even later"),
                BusInfo("Route 8", "Even later"),
            ), true
        )
        // ----

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
            Log.e(
                TAG,
                "focus changed - hasFocus=${hasFocus} f1=${searchBar.hasFocus()} f2=${searchBar.editText?.hasFocus()}"
            )

            if (hasFocus) {
                backPressedCallback.isEnabled = true
                searchBar.isStartIconVisible = true
                appIconView.visibility = View.GONE

                if (searchBar.editText?.text?.isNotEmpty() == true) {
                    searchBar.isEndIconVisible = true
                }
            }
        }

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
            Log.e(TAG, "textChanged - text=${text} start=${start} before=${before} count=${count}")

            searchBar.isEndIconVisible = count > 0
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

                Log.e(TAG, "searchText - text=${text}")
            }

            return@setOnEditorActionListener true
        }

        deviceLocationViewModel.requestLocation {
            if (it != null) {
                val (
                    latitude,
                    longitude,
                ) = it

                mapView?.getMapAsync { map ->
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            10f
                        )
                    )
                }
            }
        }


        // ----- bottom sheet behaviour -----
        // shift content to accommodate for search bar
        // get offset on slide when search bar is guaranteed to be measured
        var startOffset: Int? = null
        var offset: Float? = null
        var bottom: Int? = null


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

                    compactBusListView.visibility = View.VISIBLE
                    expandedBusListView.visibility = View.INVISIBLE

                    bottomSheetBehavior.isDraggable = true
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    pState = newState
                    hasProcessedStateChange = false

                    compactBusListView.visibility = View.INVISIBLE
                    expandedBusListView.visibility = View.VISIBLE

                    expandedBusListView.isNestedScrollingEnabled = true

                    bottomSheetBehavior.isDraggable = false

                    backPressedCallback.isEnabled = true
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (offset == null) {
                    // + 16dp margin
                    startOffset = bottomSheetContentView.top
                    offset = searchBar.bottom.toFloat() + 16.toDp(context)
                    bottom = bottomSheetContentView.bottom
                    val lp = bottomSheetContentView.layoutParams as MarginLayoutParams
                    lp.height =
                        (bottom!! - offset!! + 16.toDp(context)).toInt() - bottomSheetHeadingView.height - bottomSheetDragHandleView.height
                    bottomSheetContentView.layoutParams = lp

                    Log.e("H", "start - ${lp.height}")
                }

                val sOffset = (slideOffset * offset!!).toInt()
                bottomSheetContentView.y = (startOffset!! + sOffset).toFloat()

//                val lp = bottomSheetContentView.layoutParams as MarginLayoutParams
//                lp.height = bottom!! - sOffset + 16.toDp(context)
//                bottomSheetContentView.layoutParams = lp

//                Log.e("H", "slide - ${lp.height}")

                bottomSheetDragHandleView.alpha = (1 - slideOffset)

                if (pState == BottomSheetBehavior.STATE_COLLAPSED && slideOffset > 0) {
                    // started to expand
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true
                        expandedBusListView.visibility = View.VISIBLE
                        expandedBusListView.alpha = 0f
                    }
                } else if (pState == BottomSheetBehavior.STATE_EXPANDED && slideOffset < 1) {
                    // started to collapse
                    if (!hasProcessedStateChange) {
                        hasProcessedStateChange = true
                        compactBusListView.visibility = View.VISIBLE
                        compactBusListView.alpha = 0f
                    }
                }

                val state = bottomSheetBehavior.state
                if (state == BottomSheetBehavior.STATE_DRAGGING || state == BottomSheetBehavior.STATE_SETTLING) {
                    compactBusListView.alpha = 1 - slideOffset
                    expandedBusListView.alpha = slideOffset
                }
            }

        })

        expandedBusListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val scrollPosition = recyclerView.computeVerticalScrollOffset()
                val wasScrolledToTop = scrollPosition == 0 && dy < 0

                expandedBusListView.isNestedScrollingEnabled = !wasScrolledToTop
                bottomSheetBehavior.isDraggable = wasScrolledToTop
            }
        })

        Log.e(TAG, "res - ${resources.getString(R.string.GOOGLE_MAPS_API_KEY)}")

        mapView?.getMapAsync {map ->
            map.setOnCameraIdleListener {
                val cameraBoundedArea = map.projection.visibleRegion.latLngBounds
                val swLatLng = cameraBoundedArea.southwest
                val neLatLng = cameraBoundedArea.northeast

                Log.e(TAG, "map - $swLatLng $neLatLng")
            }
        }

        // route points test
        val KEY = "AIzaSyBVnEB4WLSxuZ-DLPzB42OBz0heU5J7OHo"

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(
                        "https://roads.googleapis.com/v1/snapToRoads" +
                                "?interpolate=true" +
                                "&path=-35.27801%2C149.12958%7C-35.28032%2C149.12907%7C-35.28099%2C149.12929%7C-35.28144%2C149.12984%7C-35.28194%2C149.13003%7C-35.28282%2C149.12956%7C-35.28302%2C149.12881%7C-35.28473%2C149.12836" +
                                "&key=${KEY}"
                    )

                    val conn = url.openConnection() as HttpURLConnection
                    val inputStream = conn.inputStream

                    val reader = InputStreamReader(inputStream)

                    val s = reader.readText()

                    val json = Gson().fromJson(s, BusRoutePoints::class.java)

                    val latLngs = json.snappedPoints?.map {
                        LatLng(
                            it.location.latitude,
                            it.location.longitude
                        )
                    }

                    json.snappedPoints?.forEach { }

                    val options = PolylineOptions()
                        .clickable(true)

                    options.color(Color.valueOf(1f, 0f, 0f, 0.5f).toArgb())

                    latLngs?.forEach { options.add(it) }

                    Log.e(TAG, latLngs?.size.toString())

                    withContext(Dispatchers.Main) {
                        mapView?.getMapAsync { map ->
                            map.addPolyline(options)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "error")
                    e.printStackTrace()
                }
            }
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
}