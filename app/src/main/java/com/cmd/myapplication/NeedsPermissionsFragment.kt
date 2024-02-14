package com.cmd.myapplication

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * A simple [Fragment] subclass.
 * Use the [NeedsPermissionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NeedsPermissionsFragment : Fragment(R.layout.fragment_needs_permissions) {
    companion object {
        const val TAG = "NeedsPermissionsFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            return@setOnApplyWindowInsetsListener WindowInsetsCompat(windowInsets)
        }

        val requestButton = view.findViewById<Button>(R.id.grant_permissions_button)

        requestButton.setOnClickListener {
            requestPermissions()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity as FragmentActivity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            MainActivity.PERMISSION_REQUEST_CODE
        )
    }
}