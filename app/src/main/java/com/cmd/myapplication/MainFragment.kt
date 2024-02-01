package com.cmd.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cmd.myapplication.data.viewModels.DeviceLocationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputLayout


class MainFragment : Fragment(R.layout.fragment_main) {
    companion object {
        const val TAG = "MainFragment"
    }

    private val deviceLocationViewModel: DeviceLocationViewModel by activityViewModels { DeviceLocationViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val content: ConstraintLayout = view.findViewById(R.id.content_view)

        val appIconView: ImageView = view.findViewById(R.id.app_icon_view)

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

        searchBar.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                searchBar.isStartIconVisible = false
                searchBar.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        searchBar.isEndIconVisible = false

//        mapView?.getMapAsync {
//            it.setOnMapLoadedCallback {
//                it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(51.0, 0.1), 10f))
//            }
//        }

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

        Log.e(TAG, "dlvm - ${deviceLocationViewModel}")

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

        //val searchBar: SearchBar = view.findViewById(R.id.search_bar)
//        val searchView: MaterialCardView = view.findViewById(R.id.search_view)
//        val searchField: TextInputLayout = view.findViewById(R.id.search_field)

//        val backButton: Button = view.findViewById(R.id.back_button)

//        searchField.viewTreeObserver.addOnGlobalLayoutListener {
//            val radius = searchField.height / 2f
//
//            searchField.setBoxCornerRadii(radius, radius, radius, radius)
//        }
//
//        // search view transitions
//        val searchViewShowDuration = resources.getInteger(m3_sys_motion_duration_long2).toLong()
//        val searchViewHideDuration = resources.getInteger(m3_sys_motion_duration_short4).toLong()
//
//        val searchViewShowInterpolator = PathInterpolatorCompat.create(0.05f, 0.7f, 0.1f, 1f)
//        val searchViewHideInterpolator = PathInterpolatorCompat.create(0.3f, 0f, 0.8f, 0.15f)

//        val showSearchViewTransition = MaterialContainerTransform().apply {
//            startView = searchBar
//            endView = searchField//searchView
//            scrimColor = Color.TRANSPARENT
//            duration = searchViewShowDuration
//            startElevation = 0f
//            endElevation = 0f
//            interpolator = searchViewShowInterpolator
//
//            addTarget(searchField) // searchView
//            addListener(
//                object : Transition.TransitionListener {
//                    override fun onTransitionStart(transition: Transition) {
//
//                    }
//
//                    override fun onTransitionEnd(transition: Transition) {
//                        searchBar.visibility = View.INVISIBLE
//                        searchView.visibility = View.VISIBLE
//                    }
//
//                    override fun onTransitionCancel(transition: Transition) {
//                        this.onTransitionEnd(transition)
//                    }
//
//                    override fun onTransitionPause(transition: Transition) {
//
//                    }
//
//                    override fun onTransitionResume(transition: Transition) {
//
//                    }
//
//                }
//            )
//        }
//        val hideSearchViewTransition = MaterialContainerTransform().apply {
//            startView = searchField
//            endView = searchBar
//            scrimColor = Color.TRANSPARENT
//            duration = searchViewHideDuration
//            startElevation = 0f
//            endElevation = 0f
//            interpolator = searchViewHideInterpolator
//
//            addTarget(searchBar)
//            addListener(
//                object : Transition.TransitionListener {
//                    override fun onTransitionStart(transition: Transition) {
//                    }
//
//                    override fun onTransitionEnd(transition: Transition) {
//                        searchView.visibility = View.GONE
//                        searchBar.visibility = View.VISIBLE
//                    }
//
//                    override fun onTransitionCancel(transition: Transition) {
//                        this.onTransitionEnd(transition)
//                    }
//
//                    override fun onTransitionPause(transition: Transition) {
//
//                    }
//
//                    override fun onTransitionResume(transition: Transition) {
//
//                    }
//
//                }
//            )
//        }
//
//        searchBar.setOnClickListener {
//            searchBar.isClickable = false
//            searchView.visibility = View.VISIBLE
//            searchView.alpha = 0f
//            searchView.animate().alpha(1f).start()
//
//            ValueAnimator.ofInt(searchView.height, 500)
//                .apply {
//                    addUpdateListener {
//                        val height = it.animatedValue as Int
//
//                        searchView.layoutParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
//                            setMargins(this.leftMargin)
//                        }
//
//                    }
//                    start()
//                }
//
//            TransitionManager.beginDelayedTransition(view as ViewGroup, showSearchViewTransition)
//        }
//
//        searchField.setStartIconOnClickListener {
//            searchBar.isClickable = true
//            searchBar.visibility = View.VISIBLE
//            searchView.animate().alpha(0f).start()
//
//            TransitionManager.beginDelayedTransition(view as ViewGroup, hideSearchViewTransition)
//        }
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