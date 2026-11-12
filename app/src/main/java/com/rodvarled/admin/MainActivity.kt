package com.rodvarled.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.rodvarled.admin.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var pendingRoute: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingRoute = extractRouteFromIntent(intent)

        setContent {
            AppRoot(initialRoute = pendingRoute)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val route = extractRouteFromIntent(intent)
        if (route != null) {
            pendingRoute = route
            setContent {
                AppRoot(initialRoute = pendingRoute)
            }
        }
    }

    private fun extractRouteFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return intent.getStringExtra("route") ?: intent.data?.getQueryParameter("route")
    }
}
