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
    val packageName: String, //If anonymous package name is turned on, this will be UUID
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

    @Query("SELECT * FROM AppUsage ORDER BY id DESC LIMIT :n")
    fun getLastNRows(n: Int): List<AppUsage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(appUsage: AppUsage)

    @Delete
    fun delete(appUsage: AppUsage)

    @Query("DELETE FROM AppUsage")
    fun clearAll(): Int
}

@Database(entities = [AppUsage::class], version = 2, exportSchema = false)
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
    val header = listOf("ID", "Hour of Day", "Package Name", "Is Audio Device Connected", "Is Charging", "Is Wifi Connected", "Is Mobile Data Connected", "Is Bluetooth Connected", "Brightness").joinToString(",")
    val rows = appUsages.joinToString("\n") { appUsage ->
        listOf(appUsage.id, appUsage.hourOfDay, appUsage.packageName, appUsage.isAudioDeviceConnected, appUsage.isCharging, appUsage.isWifiConnected, appUsage.isMobileDataConnected, appUsage.isBluetoothConnected, appUsage.brightness).joinToString(",")
    }
    return "$header\n$rows"
}