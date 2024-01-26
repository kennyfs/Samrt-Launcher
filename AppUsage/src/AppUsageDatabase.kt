import androidx.room.*
import java.util.*

@Entity
data class AppUsage(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val timestamp: Date,
    val packageName: String,
    val isHeadsetConnected: Boolean,
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

    @Insert
    fun insert(appUsage: AppUsage)

    @Delete
    fun delete(appUsage: AppUsage)
}

@Database(entities = [AppUsage::class], version = 1)
abstract class AppUsageDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
}