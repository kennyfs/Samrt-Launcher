package com.saggitt.omega.compose.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.saggitt.omega.compose.screens.editIconGraph
import com.saggitt.omega.compose.screens.iconPickerGraph
import com.saggitt.omega.compose.screens.preferences.mainPrefsGraph
import soup.compose.material.motion.materialSharedAxisX

object Routes {
    const val PREFS_MAIN = "prefs_main"
    const val PREFS_PROFILE = "prefs_profile"
    const val PREFS_DESKTOP = "prefs_desktop"
    const val PREFS_DOCK = "prefs_dock"
    const val PREFS_DRAWER = "prefs_drawer"
    const val PREFS_WIDGETS = "prefs_widgets"
    const val PREFS_SEARCH = "prefs_search"
    const val PREFS_GESTURES = "prefs_gestures"

    const val PREFS_HIDE_APPS_USAGE = "prefs_hide_apps_usage"
    const val PREFS_BACKUPS = "prefs_backups"
    const val PREFS_DM = "prefs_desktop_mode"
    const val PREFS_DEV = "prefs_developer"
    const val SHOW_NN_RESULTS ="prefs_show_nn_results"

    const val PREFS_SET_LAUNCHER = "prefs_set_default_launcher"
    const val ABOUT = "about"
    const val TRANSLATORS = "translators"
    const val LICENSE = "license"
    const val CHANGELOG = "changelog"
    const val EDIT_ICON = "edit_icon"
    const val ICON_PICKER = "icon_picker"
    const val GESTURE_SELECTOR = "gesture_selector"
    const val HIDDEN_APPS = "hidden_apps"
    const val PROTECTED_APPS = "protected_apps"
    const val CATEGORIZE_APPS = "categorize_apps"
    const val EDIT_DASH = "edit_dash"
    const val ICON_SHAPE = "icon_shape"
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("CompositionLocal LocalNavController not present")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DefaultComposeView(navController: NavHostController) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motionSpec = materialSharedAxisX()
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalNavController provides navController
    ) {
        AnimatedNavHost(
            navController = navController,
            startDestination = "/",
            enterTransition = { motionSpec.enter.transition(!isRtl, density) },
            exitTransition = { motionSpec.exit.transition(!isRtl, density) },
            popEnterTransition = { motionSpec.enter.transition(isRtl, density) },
            popExitTransition = { motionSpec.exit.transition(isRtl, density) },
        ) {
            preferenceGraph(route = "/", { BlankScreen() }) { subRoute ->
                editIconGraph(route = subRoute(Routes.EDIT_ICON))
                iconPickerGraph(route = subRoute(Routes.ICON_PICKER))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PrefsComposeView(navController: NavHostController) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motionSpec = materialSharedAxisX()
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalNavController provides navController
    ) {
        AnimatedNavHost(
            navController = navController,
            startDestination = "/",
            enterTransition = { motionSpec.enter.transition(!isRtl, density) },
            exitTransition = { motionSpec.exit.transition(!isRtl, density) },
            popEnterTransition = { motionSpec.enter.transition(isRtl, density) },
            popExitTransition = { motionSpec.exit.transition(isRtl, density) },
        ) {
            mainPrefsGraph(route = "/")
        }
    }
}

@Composable
fun BlankScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
    ) {
    }
}