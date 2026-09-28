package com.example.pocnodelogger

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,             // "Foodpanda", "Uber Eats", "自訂平台"
    val estimatedAmount: Double,     // 輸入的預估金額
    val baseTimeSeconds: Long,       // 金額反推的法定時間底線 (秒)
    val startTimeMs: Long,           // 開始計時時間戳 (毫秒)
    val endTimeMs: Long,             // 完成送達時間戳 (毫秒)
    val durationSeconds: Long,       // 實際總耗時 (秒)
    val overtimeSeconds: Long,       // 實際超時秒數
    val overtimePay: Double,         // 超時補貼金額
    val totalPay: Double,            // 最終總收入
    val completionReason: String = "正常配送", // 結束原因
    val groupId: String = "",         // 夾單群組 ID (空字串代表單張單)
    val orderNo: String = "",         // 訂單編號/取單碼 (選填)
    val storeName: String = ""        // 店家名稱 (選填)
)

@Dao
interface OrderDao {
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY startTimeMs DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE startTimeMs >= :startOfDayMs ORDER BY startTimeMs DESC")
    suspend fun getTodayOrders(startOfDayMs: Long): List<OrderEntity>

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long)

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()
}

@Database(entities = [OrderEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): Context {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivery_assistant_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }.applicationContext
    }
}
