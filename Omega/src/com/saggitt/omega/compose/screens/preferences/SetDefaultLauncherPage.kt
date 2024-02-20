package com.saggitt.omega.compose.screens.preferences

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SetDefaultLauncherPage() {
    val context = LocalContext.current
    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(intent)
}