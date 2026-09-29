# 📱 跑單助手 (POC Node Logger) - AI 開發脈絡與規格文件

這是提供給 AI 助理讀取的專案架構與進度紀錄檔。若對話紀錄遺失，請閱讀此文件以快速接軌專案開發。

---

## 🎯 專案簡介與核心目標
* **軟體名稱**：跑單助手 (POC Node Logger)
* **目標受眾**：Foodpanda / Uber Eats / 其他外送員
* **核心功能**：
  1. 協助外送員即時計算夾單/單單底線時間與超時補貼（基於時薪 $245 元計算）。
  2. 進行中訂單可跨頁面、重開 App 後持續精準計時（採用 Room 資料庫記錄 `startTimeMs`）。
  3. 歷史配送紀錄統計與管理。

---

## 🛠️ 技術架構 (Tech Stack)
* **平台/語言**：Android (Kotlin)
* **UI 架構**：XML Layout, Fragment, RecyclerView, ViewBinding / findViewById
* **資料庫**：Room Database (SQLite)
* **非同步處理**：Kotlin Coroutines (`lifecycleScope.launch`)
* **自動化部署**：GitHub Actions (`.github/workflows/android.yml` 自動打包測試 APK)

---

## 🗄️ 資料庫結構 (Room Schema - Version 4)

### 1. 進行中訂單 (`ActiveOrderEntity` -> `active_orders` 資料表)
* `id`: Long (主鍵)
* `platform`: String ("Foodpanda" / "Uber Eats" / "其他")
* `estimatedAmount`: Double (預估金額/單張分攤金額)
* `baseTimeSeconds`: Long (底線秒數 = `(perAmount / 245.0) * 3600`)
* `startTimeMs`: Long (開始接單時間戳)
* `groupId`: String (夾單群組ID，例: `GRP_170000000`)
* `groupTag`: String (夾單標記，例: `👥 夾單 1/3`)
* `orderNo`: String (單號/取單碼，選填)
* `storeName`: String (店家名稱，選填)

### 2. 歷史完成訂單 (`OrderEntity` -> `orders` 資料表)
*包含進行中訂單的所有欄位，並加上：*
* `endTimeMs`: Long (結束時間戳)
* `durationSeconds`: Long (總花費秒數)
* `overtimeSeconds`: Long (超時秒數)
* `overtimePay`: Double (超時補貼金額)
* `totalPay`: Double (總實收金額)
* `completionReason`: String ("正常配送" / "不想接單" / "實收" / "其他原因")

---

## 🧮 核心演算法與商業邏輯
1. **底線時間計算公式**：
   * `每單底線秒數 = (單筆金額 / 245.0) * 3600`
2. **夾單拆分邏輯**：
   * 總金額拆分：`每單金額 = 總金額 / 夾單數量`
   * 輸入 1 單拆為 1 張卡片；夾單 2 夾/3 夾會一次產生 2 或 3 張獨立計時卡片，共用 `groupId`。
3. **超時金額計算公式**：
   * `超時秒數 = max(0, 總花費秒數 - 底線秒數)`
   * `超時金額 = 超時秒數 * (245.0 / 3600.0)`
4. **跨進程計時（持久化）**：
   * 卡片不安裝 Timer 倒數計時器，而是儲存 `startTimeMs`。
   * UI 每秒重新刷 `System.currentTimeMillis() - startTimeMs` 來顯示經過時間與計算金額，確保背景關閉或切換分頁時資料與計時不遺失。

---

## 📌 目前已完成功能 (Completed Features)
- [x] 專案基礎建立與 GitHub Actions CI/CD 自動打包 APK
- [x] 計時主頁面 (`TimerFragment`)：新增訂單（含單單/2夾/3夾）、即時計算底線時間
- [x] 進行中訂單寫入 Room 資料庫 (`active_orders`)，解決切換分頁與軟體重開後卡片消失問題
- [x] 完成訂單彈窗（選擇完成原因）並移至歷史紀錄 (`orders`)
- [x] 統計與歷史頁面 (`StatisticsFragment`)
- [x] 設定頁面 (`SettingsFragment`)

---

## 🚀 未來規劃 / 待辦事項 (Roadmap)
- [ ] **正式簽名金鑰 (Release Keystore)**：設定 GitHub Secrets，發布正式 Release APK 免卸載更新。
- [ ] **懸浮視窗 (Floating Window / Overlay)**：支援外送員在 Foodpanda/Uber Eats App 上方顯示懸浮卡片。
- [ ] **無障礙服務 (Accessibility Service)**：自動抓取外送 App 螢幕上的店家名稱與金額。

---

## 📄 專案重要檔案路徑
* `app/src/main/java/com/example/pocnodelogger/AppDatabase.kt` (Room 資料庫與 Entities)
* `app/src/main/java/com/example/pocnodelogger/ActiveOrderEntity.kt`
* `app/src/main/java/com/example/pocnodelogger/ActiveOrderDao.kt`
* `app/src/main/java/com/example/pocnodelogger/TimerFragment.kt` (計時核心邏輯)
