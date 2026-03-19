package com.violet.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.violet.app.ui.screens.*

object Routes {
    const val HOME       = "home"
    const val VERTIMERGE = "vertimerge"
    const val ZIP_MAKER  = "zip_maker"
    const val WATERMARK  = "watermark"
    const val FORMAT     = "format"
}

@Composable
fun VioletNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME)       { HomeScreen(nav) }
        composable(Routes.VERTIMERGE) { VertimergeScreen { nav.popBackStack() } }
        composable(Routes.ZIP_MAKER)  { ZipMakerScreen  { nav.popBackStack() } }
        composable(Routes.WATERMARK)  { WatermarkScreen { nav.popBackStack() } }
        composable(Routes.FORMAT)     { FormatChangerScreen { nav.popBackStack() } }
    }
}
