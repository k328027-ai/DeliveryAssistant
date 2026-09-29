package com.example.pocnodelogger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_orders")
data class ActiveOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val estimatedAmount: Double,
    val baseTimeSeconds: Long,
    val startTimeMs: Long,
    val groupId: String = "",
    val groupTag: String = "",
    val orderNo: String = "",
    val storeName: String = ""
)
