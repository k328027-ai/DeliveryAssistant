package com.example.pocnodelogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        setupOverlayView()
        startUpdatingOverlay()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "overlay_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "懸浮視窗服務",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // 使用 Android 系統內建圖示，避免找不到 ic_launcher 報錯
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("跑單助手懸浮卡片執行中")
            .setContentText("正在外送 App 上方提供即時超時試算")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1001, notification)
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val headerView = overlayView?.findViewById<View>(R.id.llHeader)
        headerView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        overlayView?.findViewById<TextView>(R.id.btnCloseOverlay)?.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(overlayView, params)
    }

    private fun startUpdatingOverlay() {
        timerRunnable = object : Runnable {
            override fun run() {
                updateOverlayContent()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun updateOverlayContent() {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val activeList = db.activeOrderDao().getAllActiveOrders()

            withContext(Dispatchers.Main) {
                val tvStatus = overlayView?.findViewById<TextView>(R.id.tvOverlayStatus)
                val tvOvertime = overlayView?.findViewById<TextView>(R.id.tvOverlayOvertime)

                if (activeList.isEmpty()) {
                    tvStatus?.text = "目前無進行中訂單"
                    tvOvertime?.text = "回到主選單新增配送"
                } else {
                    val count = activeList.size
                    val firstOrder = activeList.first()
                    val nowMs = System.currentTimeMillis()
                    val durationSec = (nowMs - firstOrder.startTimeMs) / 1000
                    val overtimeSec = maxOf(0L, durationSec - firstOrder.baseTimeSeconds)
                    val overtimePay = overtimeSec * (245.0 / 3600.0)

                    tvStatus?.text = "進行中訂單：$count 筆 (${firstOrder.platform})"
                    if (overtimeSec > 0) {
                        tvOvertime?.text = String.format("已超時 %d 分！補貼: +$%.1f", overtimeSec / 60, overtimePay)
                        tvOvertime?.setTextColor(android.graphics.Color.RED)
                    } else {
                        val remainSec = firstOrder.baseTimeSeconds - durationSec
                        tvOvertime?.text = String.format("底線倒數：%d 分 %02d 秒", remainSec / 60, remainSec % 60)
                        tvOvertime?.setTextColor(android.graphics.Color.WHITE)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }
}
