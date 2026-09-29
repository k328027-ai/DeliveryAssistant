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

// 歷史完成訂單資料結構
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val estimatedAmount: Double,
    val baseTimeSeconds: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Long,
    val overtimeSeconds: Long,
    val overtimePay: Double,
    val totalPay: Double,
    val completionReason: String = "正常配送",
    val groupId: String = "",
    val orderNo: String = "",
    val storeName: String = ""
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

// Room 資料庫主體（含歷史訂單與進行中訂單）
@Database(entities = [OrderEntity::class, ActiveOrderEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun activeOrderDao(): ActiveOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
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
        }
    }
}
