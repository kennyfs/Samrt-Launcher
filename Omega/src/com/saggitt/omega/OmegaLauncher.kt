/*
 *  This file is part of Omega Launcher
 *  Copyright (c) 2021   Omega Launcher Team
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.saggitt.omega

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.launcher3.AppFilter
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherRootView
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.launcher3.util.ComponentKey
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import com.android.launcher3.util.Themes.KEY_THEMED_ICONS
import com.android.launcher3.views.OptionsPopupView
import com.android.launcher3.widget.RoundedCornerEnforcement
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.shared.system.QuickStepContract
import com.farmerbb.taskbar.lib.Taskbar
import com.google.systemui.smartspace.SmartSpaceView
import com.saggitt.omega.data.PeopleRepository
import com.saggitt.omega.gestures.GestureController
import com.saggitt.omega.popup.OmegaShortcuts
import com.saggitt.omega.preferences.OmegaPreferences
import com.saggitt.omega.preferences.OmegaPreferencesChangeCallback
import com.saggitt.omega.search.PeopleItems
import com.saggitt.omega.theme.ThemeManager
import com.saggitt.omega.theme.ThemeOverride
import com.saggitt.omega.util.Config
import com.saggitt.omega.util.isPackageInstalled
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.stream.Stream

class OmegaLauncher : QuickstepLauncher(), LifecycleOwner, SavedStateRegistryOwner,
    ActivityResultRegistryOwner, ThemeManager.ThemeableActivity,
    OmegaPreferences.OnPreferenceChangeListener {
    val gestureController by lazy { GestureController(this) }
    val dummyView by lazy { findViewById<View>(R.id.dummy_view)!! }
    val optionsView by lazy { findViewById<OptionsPopupView>(R.id.options_view)!! }
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override var currentTheme = 0
    override var currentAccent = 0
    private lateinit var themeOverride: ThemeOverride
    private val themeSet: ThemeOverride.ThemeSet get() = ThemeOverride.Settings()

    private var paused = false
    private var sRestart = false
    private val prefCallback = OmegaPreferencesChangeCallback(this)
    val prefs: OmegaPreferences by lazy { Utilities.getOmegaPrefs(this) }

    val hiddenApps = ArrayList<AppInfo>()
    val allApps = ArrayList<AppInfo>()

    private val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            val activity = this@OmegaLauncher

            // Immediate result path
            val synchronousResult = contract.getSynchronousResult(activity, input)
            if (synchronousResult != null) {
                Handler(Looper.getMainLooper()).post {
                    dispatchResult(
                        requestCode,
                        synchronousResult.value
                    )
                }
                return
            }

            // Start activity path
            val intent = contract.createIntent(activity, input)
            var optionsBundle: Bundle? = null
            // If there are any extras, we should defensively set the classLoader
            if (intent.extras != null && intent.extras!!.classLoader == null) {
                intent.setExtrasClassLoader(activity.classLoader)
            }
            if (intent.hasExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)) {
                optionsBundle =
                    intent.getBundleExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)
                intent.removeExtra(StartActivityForResult.EXTRA_ACTIVITY_OPTIONS_BUNDLE)
            } else if (options != null) {
                optionsBundle = options.toBundle()
            }
            if (RequestMultiplePermissions.ACTION_REQUEST_PERMISSIONS == intent.action) {
                var permissions =
                    intent.getStringArrayExtra(RequestMultiplePermissions.EXTRA_PERMISSIONS)
                if (permissions == null) {
                    permissions = arrayOfNulls(0)
                }
                ActivityCompat.requestPermissions(activity, permissions, requestCode)
            } else if (StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST == intent.action) {
                val request: IntentSenderRequest =
                    intent.getParcelableExtra(StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST)!!
                try {
                    // startIntentSenderForResult path
                    ActivityCompat.startIntentSenderForResult(
                        activity, request.intentSender,
                        requestCode, request.fillInIntent, request.flagsMask,
                        request.flagsValues, 0, optionsBundle
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Handler(Looper.getMainLooper()).post {
                        dispatchResult(
                            requestCode, RESULT_CANCELED,
                            Intent()
                                .setAction(StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST)
                                .putExtra(StartIntentSenderForResult.EXTRA_SEND_INTENT_EXCEPTION, e)
                        )
                    }
                }
            } else {
                // startActivityForResult path
                ActivityCompat.startActivityForResult(activity, intent, requestCode, optionsBundle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
            && !Utilities.hasStoragePermission(this)
        ) Utilities.requestStoragePermission(this)
        if(!Utilities.hasPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)){
            Utilities.requestBluetoothPermission(this)
        }
        //Load People Info and request permission if needed
        if (prefs.searchContacts.onGetValue()) {
            if (!Utilities.hasPermission(this, android.Manifest.permission.READ_CONTACTS)) {
                Utilities.requestPeoplePermission(this)
            }
        }
        if (Utilities.hasPermission(this, android.Manifest.permission.READ_CONTACTS)) {
            val peopleItems = PeopleItems(this)
            val scope = CoroutineScope(Dispatchers.IO) + CoroutineName("PeopleRepository")
            val repository = PeopleRepository.INSTANCE.get(this)
            scope.launch {
                repository.deleteAll()
                val list = peopleItems.getPeopleInformation()
                list.map { repository.insert(it) }
            }

        } else {
            prefs.searchContacts.onSetValue(false)
        }

        themeOverride = ThemeOverride(themeSet, this)
        themeOverride.applyTheme(this)
        currentAccent = prefs.themeAccentColor.onGetValue()
        currentTheme = themeOverride.getTheme(this)
        val config = Config(this)
        config.setAppLanguage(prefs.language.onGetValue())

        theme.applyStyle(
            resources.getIdentifier(
                Integer.toHexString(currentAccent),
                "style",
                packageName
            ), true
        )

        savedStateRegistryController.performRestore(savedInstanceState)
        super.onCreate(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        prefs.registerCallback(prefCallback)
        prefs.addOnPreferenceChangeListener(PREFS_STATUSBAR_HIDE, this)

        //Set Initial value for idp columns and rows
        if (prefs.firstRun.onGetValue()) {
            val idp = LauncherAppState.getIDP(this)
            prefs.drawerColumns.onSetValue(idp.numAllAppsColumns)
            prefs.desktopColumns.onSetValue(idp.numColumns)
            prefs.desktopRows.onSetValue(idp.numRows)
            prefs.dockNumIcons.onSetValue(idp.numHotseatIcons)
            prefs.firstRun.onSetValue(false)
        }

        mOverlayManager = defaultOverlay
        showFolderNotificationCount = prefs.notificationCountFolder.onGetValue()
        if (prefs.themeCornerRadius.onGetValue() > -1) {
            RoundedCornerEnforcement.sRoundedCornerEnabled = true
            QuickStepContract.sCustomCornerRadius = prefs.themeCornerRadius.onGetValue()
        }

        //Load hidden apps to use with hidden apps preference
        MODEL_EXECUTOR.handler.postAtFrontOfQueue { loadHiddenApps(prefs.drawerHiddenAppSet.onGetValue()) }

        if (prefs.themeIconPackGlobal.onGetValue() == LAWNICONS_PACKAGE_NAME &&
            !packageManager.isPackageInstalled(packageName = LAWNICONS_PACKAGE_NAME)
        ) {
            prefs.themeIconPackGlobal.onSetValue("")
        }
        Utilities.getPrefs(this).edit()
            .putBoolean(
                KEY_THEMED_ICONS,
                prefs.themeIconPackGlobal.onGetValue() == LAWNICONS_PACKAGE_NAME ||
                        prefs.themeIconPackGlobal.onGetValue() == THEME_ICON_THEMED
            ).apply()
    }

    private fun loadHiddenApps(hiddenAppsSet: Set<String>) {
        val mContext = this
        CoroutineScope(Dispatchers.IO).launch {
            val appFilter = AppFilter()
            for (user in UserCache.INSTANCE[mContext].userProfiles) {
                val duplicatePreventionCache: MutableList<ComponentName> = ArrayList()
                for (info in getSystemService(
                    LauncherApps::class.java
                ).getActivityList(null, user)) {
                    val key = ComponentKey(info.componentName, info.user)
                    if (hiddenAppsSet.contains(key.toString())) {
                        val appInfo = AppInfo(info, info.user, false)
                        hiddenApps.add(appInfo)
                    }
                    if (prefs.searchHiddenApps.onGetValue()) {
                        if (!appFilter.shouldShowApp(info.componentName, user)) {
                            continue
                        }
                        if (!duplicatePreventionCache.contains(info.componentName)) {
                            duplicatePreventionCache.add(info.componentName)
                            val appInfo = AppInfo(mContext, info, user)
                            allApps.add(appInfo)
                        }
                    }
                }
            }
        }
    }

    override fun getSupportedShortcuts(): Stream<SystemShortcut.Factory<*>> {
        return Stream.concat(
            super.getSupportedShortcuts(),
            Stream.of(
                OmegaShortcuts.CUSTOMIZE,
                OmegaShortcuts.APP_REMOVE,
                OmegaShortcuts.APP_UNINSTALL
            )
        )
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (currentAccent != prefs.themeAccentColor.onGetValue() || currentTheme != themeOverride.getTheme(
                this
            )
        ) onThemeChanged()
        restartIfPending()
        paused = false
    }

    override fun onPause() {
        super.onPause()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        paused = true
    }

    override fun onStop() {
        super.onStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        prefs.removeOnPreferenceChangeListener(PREFS_STATUSBAR_HIDE, this)
        prefs.unregisterCallback()
        if (sRestart) {
            sRestart = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        savedStateRegistryController.performSave(outState)
    }

    override fun setupViews() {
        super.setupViews()
        findViewById<LauncherRootView>(R.id.launcher).let {
            ViewTreeLifecycleOwner.set(it, this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }
    }

    private fun restartIfPending() {
        if (sRestart) {
            omegaApp.restart(false)
        }
    }

    fun scheduleRestart() {
        if (paused) {
            sRestart = true
        } else {
            Utilities.restartLauncher(this)
        }
    }

    override fun onThemeChanged(forceUpdate: Boolean) = recreate()

    fun shouldRecreate() = !sRestart

    override fun getActivityResultRegistry(): ActivityResultRegistry {
        return activityResultRegistry
    }

    override fun getDefaultOverlay(): LauncherOverlayManager {
        if (mOverlayManager == null) {
            mOverlayManager = OverlayCallbackImpl(this)
        }
        return mOverlayManager
    }

    inline fun prepareDummyView(view: View, crossinline callback: (View) -> Unit) {
        val rect = Rect()
        dragLayer.getViewRectRelativeToSelf(view, rect)
        prepareDummyView(rect.left, rect.top, rect.right, rect.bottom, callback)
    }

    inline fun prepareDummyView(left: Int, top: Int, crossinline callback: (View) -> Unit) {
        val size = resources.getDimensionPixelSize(R.dimen.options_menu_thumb_size)
        val halfSize = size / 2
        prepareDummyView(left - halfSize, top - halfSize, left + halfSize, top + halfSize, callback)
    }

    inline fun prepareDummyView(
        left: Int, top: Int, right: Int, bottom: Int,
        crossinline callback: (View) -> Unit
    ) {
        (dummyView.layoutParams as ViewGroup.MarginLayoutParams).let {
            it.width = right - left
            it.height = bottom - top
            it.leftMargin = left
            it.topMargin = top
        }
        dummyView.requestLayout()
        dummyView.post { callback(dummyView) }
    }

    fun registerSmartspaceView(smartspace: SmartSpaceView) {
        defaultOverlay.registerSmartSpaceView(smartspace)
    }

    override fun onValueChanged(key: String, prefs: OmegaPreferences, force: Boolean) {
        if (key == PREFS_STATUSBAR_HIDE) {
            if (prefs.desktopHideStatusBar.onGetValue()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else if (!force) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
        Taskbar.setEnabled(this, prefs.desktopModeEnabled)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    companion object {
        var showFolderNotificationCount = false

        @JvmStatic
        fun getLauncher(context: Context): OmegaLauncher {
            return context as? OmegaLauncher
                ?: (context as ContextWrapper).baseContext as? OmegaLauncher
                ?: LauncherAppState.getInstance(context).launcher as OmegaLauncher
        }
    }
}