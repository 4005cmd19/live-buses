package com.cmd.myapplication

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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