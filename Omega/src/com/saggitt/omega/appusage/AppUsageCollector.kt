package com.saggitt.omega.appusage

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import android.bluetooth.BluetoothAdapter
import androidx.core.content.ContextCompat.getSystemService
import java.util.Date

class AppUsageCollector(private val context: Context) {
    fun collectAppUsage(): AppUsage {
        val timestamp = Date()

        val packageName = context.packageName
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS)

        val isAudioDeviceConnected = audioDevices.any { device ->
            device.type in setOf(
                AudioDeviceInfo.TYPE_AUX_LINE,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_LINE_DIGITAL,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET
            )
        }

        val isHeadsetConnected = context.registerReceiver(null, IntentFilter(Intent.ACTION_HEADSET_PLUG)).getIntExtra("state", 0) == 1

        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isWifiConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMobileDataConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothConnected = bluetoothAdapter != null && bluetoothAdapter.isEnabled && bluetoothAdapter.bondedDevices.isNotEmpty()

        val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)

        return AppUsage(
            0,
            timestamp,
            packageName,
            isHeadsetConnected,
            isCharging,
            isWifiConnected,
            isMobileDataConnected,
            isBluetoothConnected,
            brightness
        )
    }
}