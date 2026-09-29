package com.example.pocnodelogger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ActiveOrderDao {
    @Query("SELECT * FROM active_orders ORDER BY startTimeMs ASC")
    suspend fun getAllActiveOrders(): List<ActiveOrderEntity>

    @Insert
    suspend fun insertActiveOrder(order: ActiveOrderEntity): Long

    @Query("DELETE FROM active_orders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
