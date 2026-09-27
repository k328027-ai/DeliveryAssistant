package com.example.pocnodelogger

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val hourlyRateBenchmark = 245.0 // 法定時薪基準 245 TWD/HR
    private val perSecondRate = hourlyRateBenchmark / 3600.0 // 約 0.068055 TWD/sec

    private var isRunning = false
    private var startTimeMs: Long = 0
    private var baseTimeSeconds: Long = 661 // 預設 45 元底線時間 (秒)

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    // UI 元件
    private lateinit var rgPlatform: RadioGroup
    private lateinit var etAmount: EditText
    private lateinit var tvBaseTimeHint: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvOvertimeStatus: TextView
    private lateinit var tvOvertimePay: TextView
    private lateinit var btnStartStop: Button
    private lateinit var tvTodayStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupTimer()
        loadTodayStats()
    }

    private fun initViews() {
        rgPlatform = findViewById(R.id.rgPlatform)
        etAmount = findViewById(R.id.etAmount)
        tvBaseTimeHint = findViewById(R.id.tvBaseTimeHint)
        tvTimer = findViewById(R.id.tvTimer)
        tvOvertimeStatus = findViewById(R.id.tvOvertimeStatus)
        tvOvertimePay = findViewById(R.id.tvOvertimePay)
        btnStartStop = findViewById(R.id.btnStartStop)
        tvTodayStats = findViewById(R.id.tvTodayStats)

        recalculateBaseTime()
    }

    private fun setupListeners() {
        // 快捷金額按鈕
        findViewById<Button>(R.id.btnQuick45).setOnClickListener { etAmount.setText("45") }
        findViewById<Button>(R.id.btnQuick55).setOnClickListener { etAmount.setText("55") }
        findViewById<Button>(R.id.btnQuick65).setOnClickListener { etAmount.setText("65") }
        findViewById<Button>(R.id.btnQuick75).setOnClickListener { etAmount.setText("75") }

        // 金額連動法定時間底線
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                recalculateBaseTime()
            }
        })

        // 開始 / 送達按鈕
        btnStartStop.setOnClickListener {
            if (!isRunning) {
                startOrderTimer()
            } else {
                completeOrder()
            }
        }
    }

    private fun recalculateBaseTime() {
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        // 法定時間底線 (秒) = (金額 / 245) * 3600
        baseTimeSeconds = ((amount / hourlyRateBenchmark) * 3600).toLong()
        val minutes = baseTimeSeconds / 60
        val seconds = baseTimeSeconds % 60
        tvBaseTimeHint.text = String.format(Locale.getDefault(), "法定時間底線：%d 分 %02d 秒", minutes, seconds)
    }

    private fun setupTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    val elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                    if (elapsedSeconds > baseTimeSeconds) {
                        val overtimeSec = elapsedSeconds - baseTimeSeconds
                        val overtimePay = overtimeSec * perSecondRate

                        tvOvertimeStatus.text = "狀態：已超時拖單中"
                        tvOvertimeStatus.setTextColor(Color.parseColor("#D32F2F"))
                        tvOvertimePay.text = String.format(Locale.getDefault(), "超時補貼：+ $%.1f 元", overtimePay)
                    } else {
                        tvOvertimeStatus.text = "狀態：正常送達中"
                        tvOvertimeStatus.setTextColor(Color.parseColor("#2E7D32"))
                        tvOvertimePay.text = "超時補貼：+ $0.0 元"
                    }

                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startOrderTimer() {
        val amount = etAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "請輸入有效的預估金額", Toast.LENGTH_SHORT).show()
            return
        }

        isRunning = true
        startTimeMs = System.currentTimeMillis()
        btnStartStop.text = "完成送達 (寫入紀錄)"
        btnStartStop.setBackgroundColor(Color.parseColor("#2E7D32"))
        etAmount.isEnabled = false

        handler.post(timerRunnable)
    }

    private fun completeOrder() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)

        val endTimeMs = System.currentTimeMillis()
        val durationSeconds = (endTimeMs - startTimeMs) / 1000
        val estimatedAmount = etAmount.text.toString().toDoubleOrNull() ?: 0.0

        val overtimeSeconds = if (durationSeconds > baseTimeSeconds) durationSeconds - baseTimeSeconds else 0L
        val overtimePay = overtimeSeconds * perSecondRate
        val totalPay = estimatedAmount + overtimePay

        val selectedPlatform = when (rgPlatform.checkedRadioButtonId) {
            R.id.rbFoodpanda -> "Foodpanda"
            R.id.rbUberEats -> "Uber Eats"
            else -> "其他"
        }

        val order = OrderEntity(
            platform = selectedPlatform,
            estimatedAmount = estimatedAmount,
            baseTimeSeconds = baseTimeSeconds,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            durationSeconds = durationSeconds,
            overtimeSeconds = overtimeSeconds,
            overtimePay = overtimePay,
            totalPay = totalPay
        )

        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(applicationContext).orderDao().insertOrder(order)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "訂單已儲存！補貼：+$${String.format("%.1f", overtimePay)}", Toast.LENGTH_SHORT).show()
                resetTimerUI()
                loadTodayStats()
            }
        }
    }

    private fun resetTimerUI() {
        btnStartStop.text = "開始接單/出發"
        btnStartStop.setBackgroundColor(Color.parseColor("#1976D2"))
        etAmount.isEnabled = true
        tvTimer.text = "00:00"
        tvOvertimeStatus.text = "狀態：待命開跑"
        tvOvertimeStatus.setTextColor(Color.parseColor("#2E7D32"))
        tvOvertimePay.text = "超時補貼：+ $0.0 元"
    }

    private fun loadTodayStats() {
        lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDayMs = calendar.timeInMillis

            val todayOrders = AppDatabase.getDatabase(applicationContext).orderDao().getTodayOrders(startOfDayMs)

            val totalCount = todayOrders.size
            val totalPay = todayOrders.sumOf { it.totalPay }
            val totalDurationSeconds = todayOrders.sumOf { it.durationSeconds }
            val totalHours = totalDurationSeconds / 3600.0

            val hourlyRate = if (totalHours > 0) totalPay / totalHours else 0.0

            withContext(Dispatchers.Main) {
                tvTodayStats.text = String.format(
                    Locale.getDefault(),
                    "今日總單數：%d 單\n總計收入：$%.1f 元\n總跑單時數：%.2f 小時\n實質折算時薪：$%.0f / hr",
                    totalCount, totalPay, totalHours, hourlyRate
                )
            }
        }
    }
}
