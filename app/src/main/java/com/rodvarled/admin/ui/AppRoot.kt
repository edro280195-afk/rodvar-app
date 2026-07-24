package com.rodvarled.admin.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.rodvarled.admin.ui.auth.LoginScreen
import com.rodvarled.admin.ui.navigation.MainScaffold
import com.rodvarled.admin.ui.theme.RodvarTheme

@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    RodvarTheme {
        if (isLoggedIn) {
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // Si el token se limpia (401 sin refresh válido) mientras la app está en Main,
            // isLoggedIn cambia a false y este composable se recompone hacia LoginScreen.
            val navController = rememberNavController()
            MainScaffold(navController = navController)
        } else {
            LoginScreen()
        }
    }
}
