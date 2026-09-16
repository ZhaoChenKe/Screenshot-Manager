package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Categories : Screen("categories")
    object CategoryDetail : Screen("category_detail/{categoryId}") {
        fun createRoute(categoryId: String) = "category_detail/$categoryId"
    }
    object Timeline : Screen("timeline")
    object Search : Screen("search")
    object Detail : Screen("detail/{screenshotId}") {
        fun createRoute(screenshotId: Long) = "detail/$screenshotId"
    }
    object Duplicates : Screen("duplicates")
    object Settings : Screen("settings")
}
