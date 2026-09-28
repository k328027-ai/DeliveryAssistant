package com.example.pocnodelogger

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 記憶體中進行中訂單的資料結構
data class ActiveOrderItem(
    val id: Long = System.currentTimeMillis(),
    val platform: String,
    val estimatedAmount: Double,
    val baseTimeSeconds: Long,
    val startTimeMs: Long
)

class ActiveOrdersAdapter(
    private val activeList: MutableList<ActiveOrderItem>,
    private val onCompleteClick: (ActiveOrderItem) -> Unit
) : RecyclerView.Adapter<ActiveOrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlatform: TextView = itemView.findViewById(R.id.tvCardPlatform)
        val tvTimer: TextView = itemView.findViewById(R.id.tvCardTimer)
        val tvOvertime: TextView = itemView.findViewById(R.id.tvCardOvertime)
        val btnComplete: Button = itemView.findViewById(R.id.btnCompleteCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = activeList[position]
        holder.tvPlatform.text = "${item.platform} ($${item.estimatedAmount.toInt()})"

        // 計算已進行時間
        val elapsedSeconds = (System.currentTimeMillis() - item.startTimeMs) / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        holder.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

        // 超時計算
        val overtime = elapsedSeconds - item.baseTimeSeconds
        if (overtime > 0) {
            val overtimePay = overtime * (245.0 / 3600.0)
            holder.tvOvertime.text = String.format("已超時！補貼：+$%.1f", overtimePay)
            holder.tvOvertime.setTextColor(Color.parseColor("#D32F2F")) // 超時顯示紅色
        } else {
            val remainSeconds = item.baseTimeSeconds - elapsedSeconds
            val remainMin = remainSeconds / 60
            val remainSec = remainSeconds % 60
            holder.tvOvertime.text = String.format("剩餘底線：%02d:%02d", remainMin, remainSec)
            holder.tvOvertime.setTextColor(Color.parseColor("#2E7D32")) // 正常顯示綠色
        }

        // 點擊卡片上的「結束/完成」
        holder.btnComplete.setOnClickListener {
            onCompleteClick(item)
        }
    }

    override fun getItemCount(): Int = activeList.size
}
