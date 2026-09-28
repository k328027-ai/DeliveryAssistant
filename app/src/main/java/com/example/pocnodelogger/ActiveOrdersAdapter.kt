package com.example.pocnodelogger

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ActiveOrderItem(
    val id: Long = System.currentTimeMillis(),
    val platform: String,
    val estimatedAmount: Double,
    val baseTimeSeconds: Long,
    val startTimeMs: Long,
    val groupId: String = "",
    val groupTag: String = "",
    val orderNo: String = "",
    val storeName: String = ""
)

class ActiveOrdersAdapter(
    private val activeList: MutableList<ActiveOrderItem>,
    private val onCompleteClick: (ActiveOrderItem) -> Unit
) : RecyclerView.Adapter<ActiveOrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardLayout: LinearLayout = itemView.findViewById(R.id.cardLayout)
        val tvGroupBadge: TextView = itemView.findViewById(R.id.tvGroupBadge)
        val tvPlatform: TextView = itemView.findViewById(R.id.tvCardPlatform)
        val tvTimer: TextView = itemView.findViewById(R.id.tvCardTimer)
        val tvStore: TextView = itemView.findViewById(R.id.tvCardStore)
        val tvOvertime: TextView = itemView.findViewById(R.id.tvCardOvertime)
        val tvTotalPay: TextView = itemView.findViewById(R.id.tvCardTotalPay)
        val btnComplete: Button = itemView.findViewById(R.id.btnCompleteCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = activeList[position]

        // 標題與單號
        val titleText = if (item.orderNo.isNotEmpty()) "${item.platform} (${item.orderNo})" else item.platform
        holder.tvPlatform.text = titleText

        // 平台專屬色彩
        val colorCode = when (item.platform) {
            "Foodpanda" -> "#D81B60"
            "Uber Eats" -> "#06C167"
            else -> "#374151"
        }
        holder.tvPlatform.setTextColor(Color.parseColor(colorCode))

        // 夾單群組標籤
        if (item.groupTag.isNotEmpty()) {
            holder.tvGroupBadge.visibility = View.VISIBLE
            holder.tvGroupBadge.text = item.groupTag
        } else {
            holder.tvGroupBadge.visibility = View.GONE
        }

        // 店家名稱
        if (item.storeName.isNotEmpty()) {
            holder.tvCardStore.visibility = View.VISIBLE
            holder.tvCardStore.text = "店家：${item.storeName}"
        } else {
            holder.tvCardStore.visibility = View.GONE
        }

        // 計時與超時計算
        val elapsedSeconds = (System.currentTimeMillis() - item.startTimeMs) / 1000
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        holder.tvTimer.text = String.format("%02d:%02d", minutes, seconds)

        val overtime = elapsedSeconds - item.baseTimeSeconds
        val overtimePay = if (overtime > 0) overtime * (245.0 / 3600.0) else 0.0
        val totalPay = item.estimatedAmount + overtimePay

        if (overtime > 0) {
            holder.tvOvertime.text = String.format("已超時！補貼：+\$%.1f", overtimePay)
            holder.tvOvertime.setTextColor(Color.parseColor("#DC2626"))
        } else {
            val remainSeconds = item.baseTimeSeconds - elapsedSeconds
            val remainMin = remainSeconds / 60
            val remainSec = remainSeconds % 60
            holder.tvOvertime.text = String.format("剩餘底線：%02d:%02d", remainMin, remainSec)
            holder.tvOvertime.setTextColor(Color.parseColor("#15803D"))
        }

        holder.tvTotalPay.text = String.format("目前金額: \$%.1f", totalPay)

        holder.btnComplete.setOnClickListener {
            onCompleteClick(item)
        }
    }

    override fun getItemCount(): Int = activeList.size
}
