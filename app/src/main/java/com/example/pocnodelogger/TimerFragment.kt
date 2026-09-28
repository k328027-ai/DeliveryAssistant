package com.example.pocnodelogger

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerFragment : Fragment() {

    private val activeOrders = mutableListOf<ActiveOrderItem>()
    private lateinit var adapter: ActiveOrdersAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val etStoreName = view.findViewById<EditText>(R.id.etStoreName)
        val etOrderNo = view.findViewById<EditText>(R.id.etOrderNo)
        val tvBaseTimeHint = view.findViewById<TextView>(R.id.tvBaseTimeHint)
        val btnAddOrder = view.findViewById<Button>(R.id.btnAddOrder)
        val rgPlatform = view.findViewById<RadioGroup>(R.id.rgPlatform)
        val rgSplitCount = view.findViewById<RadioGroup>(R.id.rgSplitCount)
        val rvActiveOrders = view.findViewById<RecyclerView>(R.id.rvActiveOrders)

        adapter = ActiveOrdersAdapter(activeOrders) { item ->
            showCompletionDialog(item)
        }
        rvActiveOrders.layoutManager = LinearLayoutManager(requireContext())
        rvActiveOrders.adapter = adapter

        // 動態試算單張底線
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val amount = s.toString().toDoubleOrNull() ?: 45.0
                val splitCount = when (rgSplitCount.checkedRadioButtonId) {
                    R.id.rbSplit2 -> 2
                    R.id.rbSplit3 -> 3
                    else -> 1
                }
                val perAmount = amount / splitCount
                val baseSec = (perAmount / 245.0 * 3600).toLong()
                val min = baseSec / 60
                val sec = baseSec % 60
                val formattedPerAmount = String.format("%.1f", perAmount)
                tvBaseTimeHint.text = String.format("每單底線：%d 分 %02d 秒 ($%s/單)", min, sec, formattedPerAmount)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnAddOrder.setOnClickListener {
            val totalAmount = etAmount.text.toString().toDoubleOrNull() ?: 45.0
            val storeName = etStoreName.text.toString().trim()
            val orderNo = etOrderNo.text.toString().trim()

            val splitCount = when (rgSplitCount.checkedRadioButtonId) {
                R.id.rbSplit2 -> 2
                R.id.rbSplit3 -> 3
                else -> 1
            }

            val perAmount = totalAmount / splitCount
            val baseSec = (perAmount / 245.0 * 3600).toLong()

            val platform = when (rgPlatform.checkedRadioButtonId) {
                R.id.rbUberEats -> "Uber Eats"
                R.id.rbOther -> "其他"
                else -> "Foodpanda"
            }

            val groupId = if (splitCount > 1) "GRP_${System.currentTimeMillis()}" else ""
            val nowMs = System.currentTimeMillis()

            for (i in 1..splitCount) {
                val tag = if (splitCount > 1) "👥 夾單 $i/$splitCount" else ""
                val itemOrderNo = if (splitCount > 1 && orderNo.isNotEmpty()) "$orderNo-#$i" else orderNo

                val newItem = ActiveOrderItem(
                    platform = platform,
                    estimatedAmount = perAmount,
                    baseTimeSeconds = baseSec,
                    startTimeMs = nowMs,
                    groupId = groupId,
                    groupTag = tag,
                    orderNo = itemOrderNo,
                    storeName = storeName
                )
                activeOrders.add(newItem)
            }

            adapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "已新增 $splitCount 筆訂單", Toast.LENGTH_SHORT).show()

            // 清空選填輸入欄位
            etStoreName.text.clear()
            etOrderNo.text.clear()
        }

        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (activeOrders.isNotEmpty()) {
                    adapter.notifyDataSetChanged()
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun showCompletionDialog(item: ActiveOrderItem) {
        val reasons = arrayOf("正常配送", "不想接單", "實收", "其他原因")
        AlertDialog.Builder(requireContext())
            .setTitle("請選擇訂單結束原因")
            .setItems(reasons) { _, which ->
                saveCompletedOrder(item, reasons[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveCompletedOrder(item: ActiveOrderItem, reason: String) {
        val endTimeMs = System.currentTimeMillis()
        val durationSec = (endTimeMs - item.startTimeMs) / 1000
        val overtimeSec = maxOf(0L, durationSec - item.baseTimeSeconds)
        val overtimePay = overtimeSec * (245.0 / 3600.0)
        val totalPay = item.estimatedAmount + overtimePay

        val orderEntity = OrderEntity(
            platform = item.platform,
            estimatedAmount = item.estimatedAmount,
            baseTimeSeconds = item.baseTimeSeconds,
            startTimeMs = item.startTimeMs,
            endTimeMs = endTimeMs,
            durationSeconds = durationSec,
            overtimeSeconds = overtimeSec,
            overtimePay = overtimePay,
            totalPay = totalPay,
            completionReason = reason,
            groupId = item.groupId,
            orderNo = item.orderNo,
            storeName = item.storeName
        )

        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(requireContext()).orderDao().insertOrder(orderEntity)
            withContext(Dispatchers.Main) {
                val index = activeOrders.indexOf(item)
                if (index != -1) {
                    activeOrders.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(requireContext(), "已記錄訂單 ($reason)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerRunnable?.let { handler.removeCallbacks(it) }
    }
}
