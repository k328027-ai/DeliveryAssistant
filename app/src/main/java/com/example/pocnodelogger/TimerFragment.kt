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
        val tvBaseTimeHint = view.findViewById<TextView>(R.id.tvBaseTimeHint)
        val btnAddOrder = view.findViewById<Button>(R.id.btnAddOrder)
        val rgPlatform = view.findViewById<RadioGroup>(R.id.rgPlatform)
        val rvActiveOrders = view.findViewById<RecyclerView>(R.id.rvActiveOrders)

        // 初始化 RecyclerView 適配器
        adapter = ActiveOrdersAdapter(activeOrders) { item ->
            showCompletionDialog(item)
        }
        rvActiveOrders.layoutManager = LinearLayoutManager(requireContext())
        rvActiveOrders.adapter = adapter

        // 輸入金額時動態即時計算時間底線
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val amount = s.toString().toDoubleOrNull() ?: 45.0
                val baseSec = (amount / 245.0 * 3600).toLong()
                val min = baseSec / 60
                val sec = baseSec % 60
                tvBaseTimeHint.text = String.format("法定時間底線：%d 分 %02d 秒", min, sec)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 按下「+ 開始接單」新增獨立計時卡片
        btnAddOrder.setOnClickListener {
            val amount = etAmount.text.toString().toDoubleOrNull() ?: 45.0
            val baseSec = (amount / 245.0 * 3600).toLong()

            val selectedPlatformId = rgPlatform.checkedRadioButtonId
            val platform = when (selectedPlatformId) {
                R.id.rbUberEats -> "Uber Eats"
                R.id.rbOther -> "其他"
                else -> "Foodpanda"
            }

            val newItem = ActiveOrderItem(
                platform = platform,
                estimatedAmount = amount,
                baseTimeSeconds = baseSec,
                startTimeMs = System.currentTimeMillis()
            )

            activeOrders.add(newItem)
            adapter.notifyItemInserted(activeOrders.size - 1)
            Toast.makeText(requireContext(), "已新增 $platform 計時訂單", Toast.LENGTH_SHORT).show()
        }

        startTimerLoop()
    }

    // 每一秒驅動一次全卡片畫面刷新
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

    // 跳出結束原因對話框
    private fun showCompletionDialog(item: ActiveOrderItem) {
        val reasons = arrayOf("正常配送", "不想接單", "實收", "其他原因")
        AlertDialog.Builder(requireContext())
            .setTitle("請選擇訂單結束原因")
            .setItems(reasons) { _, which ->
                val selectedReason = reasons[which]
                saveCompletedOrder(item, selectedReason)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 儲存結算訂單至 Room 資料庫並從畫面移除卡片
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
            completionReason = reason
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
