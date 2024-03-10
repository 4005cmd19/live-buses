package com.cmd.myapplication

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cmd.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"

        const val PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // get view
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navController = NavHostFragment.findNavController(
            supportFragmentManager.findFragmentById(R.id.fragment_container)!!
        )

        if (hasPermissions()) {
            navigateToMainFragment()
        }

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (hasPermissions()) {
                        val currentFragmentId = navController.currentDestination?.id

                        if (currentFragmentId == R.id.mainFragment) {
                            content.viewTreeObserver.removeOnPreDrawListener(this)
                            return true
                        }
                    } else {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }

                    return false
                }
            }
        )
    }

    private fun hasPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.e(TAG, "permissions - $requestCode")

        permissions.forEachIndexed { i, it ->
            Log.e(TAG, "$it -> ${grantResults[i]}")
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PERMISSION_GRANTED }) {
                // has all permissions, swap fragments
                navigateToMainFragment()
            }
        }
    }

    private fun navigateToMainFragment() {
        if (navController.currentDestination?.id != R.id.mainFragment) {
            navController.navigate(NeedsPermissionsFragmentDirections.actionNeedsPermissionsFragmentToMainFragment())
        }
    }
}