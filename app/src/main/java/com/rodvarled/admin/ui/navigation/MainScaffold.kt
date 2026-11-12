package com.rodvarled.admin.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rodvarled.admin.ui.theme.RodvarOutline
import com.rodvarled.admin.ui.theme.RodvarPrimary
import com.rodvarled.admin.ui.theme.RodvarPrimaryContainer
import com.rodvarled.admin.ui.theme.RodvarSurface
import com.rodvarled.admin.ui.theme.RodvarTextSecondary

private data class BottomTab(
    val screen: Screen,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Screen.Dashboard, "Inicio", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomTab(Screen.AppointmentsList, "Citas", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomTab(Screen.CustomersList, "Clientes", Icons.Filled.People, Icons.Outlined.People),
    BottomTab(Screen.InventoryList, "Inventario", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    BottomTab(Screen.More, "Más", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@Composable
fun MainScaffold(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(RodvarOutline))
                NavigationBar(
                    containerColor = RodvarSurface,
                    tonalElevation = 8.dp
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.filledIcon else tab.outlinedIcon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RodvarPrimary,
                                selectedTextColor = RodvarPrimary,
                                unselectedIconColor = RodvarTextSecondary,
                                unselectedTextColor = RodvarTextSecondary,
                                indicatorColor = RodvarPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        RodvarNavHost(navController = navController, modifier = Modifier.padding(padding))
    }
}
