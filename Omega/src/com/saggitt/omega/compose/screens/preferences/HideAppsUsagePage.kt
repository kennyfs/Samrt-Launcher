package com.saggitt.omega.compose.screens.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.saggitt.omega.util.App
import com.saggitt.omega.util.appComparator
import com.saggitt.omega.util.comparing

@Composable
fun HideAppsUsagePage() {
    val context = LocalContext.current
    val prefs = Utilities.getOmegaPrefs(context)
    val hiddenUsageApps by remember {
        mutableStateOf(prefs.hiddenUsageApps)
    }
    val title = if (hiddenUsageApps.isEmpty()) stringResource(id = R.string.title__hide_apps_usage)
    else stringResource(id = R.string.hide_usage_app_selected, hiddenUsageApps.size)

    AppSelectionPage(
        pageTitle = title,
        selectedApps = hiddenUsageApps,
        pluralTitleId = R.string.hide_usage_app_selected,
        mAppsComparator = getAppsComparator(hiddenUsageApps)
    ) { selectedApps ->
        prefs.hiddenUsageApps = selectedApps.map { it.split("/")[0] }.toSet()
    }
}