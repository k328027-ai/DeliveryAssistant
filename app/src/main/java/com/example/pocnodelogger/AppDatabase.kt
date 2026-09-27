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

// 1. 定義跑單紀錄資料表
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,             // "Foodpanda", "Uber Eats", "其他"
    val estimatedAmount: Double,     // 輸入的預估金額 (例: 45.0)
    val baseTimeSeconds: Long,       // 金額反推的法定時間底線 (秒)
    val startTimeMs: Long,           // 開始計時時間戳 (毫秒)
    val endTimeMs: Long,             // 完成送達時間戳 (毫秒)
    val durationSeconds: Long,       // 實際總耗時 (秒)
    val overtimeSeconds: Long,       // 實際超時秒數 (若未超時則為 0)
    val overtimePay: Double,         // 超時實時加算金額 (元)
    val totalPay: Double             // 最終總收入 (預估金額 + 超時補貼)
)

// 2. 定義資料庫操作 API (DAO)
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

// 3. 單例資料庫物件
@Database(entities = [OrderEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivery_assistant_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
