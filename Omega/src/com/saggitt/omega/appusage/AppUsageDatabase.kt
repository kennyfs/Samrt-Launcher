package com.saggitt.omega.appusage

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.OnConflictStrategy

@Entity
data class AppUsage(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val hourOfDay: Int,
    val packageName: String,
    val isAudioDeviceConnected: Boolean,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val isMobileDataConnected: Boolean,
    val isBluetoothConnected: Boolean,
    val brightness: Int
)

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM AppUsage")
    fun getAll(): List<AppUsage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(appUsage: AppUsage)

    @Delete
    fun delete(appUsage: AppUsage)
}

@Database(entities = [AppUsage::class], version = 1, exportSchema = false)
abstract class AppUsageDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    companion object {
        @Volatile
        private var INSTANCE: AppUsageDatabase? = null

        fun getDatabase(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    name = "app_usage_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

fun usagesToString(appUsages:List<AppUsage>): String {
    return appUsages.joinToString(separator = "\n\n") { appUsage ->
        """
        ID: ${appUsage.id}
        Hour of Day: ${appUsage.hourOfDay}
        Package Name: ${appUsage.packageName}
        Is Audio Device Connected: ${appUsage.isAudioDeviceConnected}
        Is Charging: ${appUsage.isCharging}
        Is Wifi Connected: ${appUsage.isWifiConnected}
        Is Mobile Data Connected: ${appUsage.isMobileDataConnected}
        Is Bluetooth Connected: ${appUsage.isBluetoothConnected}
        Brightness: ${appUsage.brightness}
        """.trimIndent()
    }
}