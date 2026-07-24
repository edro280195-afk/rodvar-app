package com.rodvarled.admin.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rodvarled.admin.ui.appointments.AppointmentDetailScreen
import com.rodvarled.admin.ui.appointments.AppointmentFormScreen
import com.rodvarled.admin.ui.appointments.AppointmentsListScreen
import com.rodvarled.admin.ui.appointments.CompleteInstallationScreen
import com.rodvarled.admin.ui.compatibility.CompatibilityScreen
import com.rodvarled.admin.ui.customers.CustomerDetailScreen
import com.rodvarled.admin.ui.customers.CustomersListScreen
import com.rodvarled.admin.ui.dashboard.DashboardScreen
import com.rodvarled.admin.ui.gallery.GalleryScreen
import com.rodvarled.admin.ui.installations.DirectInstallationScreen
import com.rodvarled.admin.ui.inventory.InventoryListScreen
import com.rodvarled.admin.ui.inventory.ProductDetailScreen
import com.rodvarled.admin.ui.inventory.ProductFormScreen
import com.rodvarled.admin.ui.more.MoreScreen
import com.rodvarled.admin.ui.profile.ProfileScreen
import com.rodvarled.admin.ui.quotes.QuoteDetailScreen
import com.rodvarled.admin.ui.quotes.QuotesListScreen
import com.rodvarled.admin.ui.reviews.ReviewsScreen
import com.rodvarled.admin.ui.warranties.WarrantyDetailScreen
import com.rodvarled.admin.ui.warranties.WarrantiesListScreen

@Composable
fun RodvarNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) },
        exitTransition = { fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(180)) }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        composable(Screen.AppointmentsList.route) {
            AppointmentsListScreen(navController = navController)
        }
        composable(
            Screen.AppointmentDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            AppointmentDetailScreen(appointmentId = id, navController = navController)
        }
        composable(
            Screen.AppointmentForm.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id")?.takeIf { it > 0 }
            AppointmentFormScreen(appointmentId = id, navController = navController)
        }
        composable(
            Screen.CompleteInstallation.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            CompleteInstallationScreen(appointmentId = id, navController = navController)
        }
        composable(Screen.DirectInstallation.route) {
            DirectInstallationScreen(navController = navController)
        }

        composable(Screen.WarrantiesList.route) {
            WarrantiesListScreen(navController = navController)
        }
        composable(
            Screen.WarrantyDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            WarrantyDetailScreen(warrantyId = id, navController = navController)
        }

        composable(Screen.CustomersList.route) {
            CustomersListScreen(navController = navController)
        }
        composable(
            Screen.CustomerDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            CustomerDetailScreen(customerId = id, navController = navController)
        }

        composable(Screen.InventoryList.route) {
            InventoryListScreen(navController = navController)
        }
        composable(
            Screen.ProductDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            ProductDetailScreen(productId = id, navController = navController)
        }
        composable(
            Screen.ProductForm.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id")?.takeIf { it > 0 }
            ProductFormScreen(productId = id, navController = navController)
        }

        composable(Screen.QuotesList.route) {
            QuotesListScreen(navController = navController)
        }
        composable(
            Screen.QuoteDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            QuoteDetailScreen(quoteId = id, navController = navController)
        }

        composable(Screen.More.route) {
            MoreScreen(navController = navController)
        }
        composable(Screen.Compatibility.route) {
            CompatibilityScreen(navController = navController)
        }
        composable(Screen.Reviews.route) {
            ReviewsScreen(navController = navController)
        }
        composable(Screen.Gallery.route) {
            GalleryScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}
