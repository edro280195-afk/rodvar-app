package com.rodvarled.admin.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.rodvarled.admin.core.util.BiometricHelper
import com.rodvarled.admin.ui.auth.BiometricLockOverlay
import com.rodvarled.admin.ui.auth.LoginScreen
import com.rodvarled.admin.ui.navigation.MainScaffold
import com.rodvarled.admin.ui.theme.RodvarTheme

@Composable
fun AppRoot(
    initialRoute: String? = null,
    viewModel: AppRootViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(isLoggedIn, isBiometricEnabled) {
        if (isLoggedIn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(activity)) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.onBiometricSuccess() },
                    onError = { err -> viewModel.onBiometricError(err) },
                    onCancel = { viewModel.onBiometricError("Autenticación cancelada. Toca para intentar de nuevo.") }
                )
            } else {
                viewModel.unlockWithoutBiometrics()
            }
        }
    }

    RodvarTheme {
        if (!isLoggedIn) {
            LoginScreen()
        } else if (!isUnlocked && isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(activity)) {
            BiometricLockOverlay(
                userName = viewModel.userName,
                errorMessage = errorMessage,
                onAuthenticateClick = {
                    BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = { viewModel.onBiometricSuccess() },
                        onError = { err -> viewModel.onBiometricError(err) },
                        onCancel = { viewModel.onBiometricError("Autenticación cancelada.") }
                    )
                },
                onLogoutClick = { viewModel.logout() }
            )
        } else {
            val navController = rememberNavController()

            LaunchedEffect(initialRoute) {
                if (!initialRoute.isNullOrBlank()) {
                    try {
                        navController.navigate(initialRoute)
                    } catch (e: Exception) {
                        // Si la ruta no coincide exactamente, ignorar
                    }
                }
            }

            MainScaffold(navController = navController)
        }
    }
}
