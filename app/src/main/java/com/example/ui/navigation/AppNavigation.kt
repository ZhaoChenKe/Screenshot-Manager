package com.example.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.categories.CategoryDetailScreen
import com.example.ui.screens.detail.ScreenshotDetailScreen
import com.example.ui.screens.duplicates.DuplicatesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.timeline.TimelineScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodel.ScreenshotViewModel

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation(
    viewModel: ScreenshotViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem(Screen.Home.route, "首页", Icons.Outlined.Home),
        BottomNavItem(Screen.Categories.route, "分类", Icons.Outlined.Category),
        BottomNavItem(Screen.Timeline.route, "时间线", Icons.Outlined.CalendarMonth),
        BottomNavItem(Screen.Search.route, "搜索", Icons.Outlined.Search)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevelDestination = navItems.any { it.route == currentDestination?.route }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                ) {
                    navItems.forEach { item ->
                        val selected = currentDestination?.route == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDuplicates = { navController.navigate(Screen.Duplicates.route) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToCategory = { catId -> navController.navigate(Screen.CategoryDetail.createRoute(catId)) }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = viewModel,
                    onNavigateToCategoryDetail = { catId -> navController.navigate(Screen.CategoryDetail.createRoute(catId)) }
                )
            }

            composable(
                route = Screen.CategoryDetail.route,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "other"
                CategoryDetailScreen(
                    categoryId = categoryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("screenshotId") { type = NavType.LongType })
            ) { backStackEntry ->
                val screenshotId = backStackEntry.arguments?.getLong("screenshotId") ?: 0L
                ScreenshotDetailScreen(
                    screenshotId = screenshotId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Duplicates.route) {
                DuplicatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
