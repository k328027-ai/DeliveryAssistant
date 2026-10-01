# 📱 跑單助手 (POC Node Logger) - AI 開發脈絡與規格文件

---

## 📌 模組功能地圖 (Feature Map)

### 1. 計時器模組 (Timer Module) - 進度: 80%
- [x] 平台選擇 (Foodpanda / Uber Eats / 其他)
- [x] 夾單拆算 (1單 / 2夾 / 3夾) 與底線時間自動試算 ($245/hr)
- [x] 選填欄位：店家名稱、取單碼/單號
- [x] 跨頁面與關閉重開持久化 (Room DB: `active_orders`)
- [ ] 懸浮視窗 (Overlay Window + Foreground Service)

### 2. 統計/收入模組 (Statistics Module) - 進度: 20%
- [x] 歷史訂單紀錄清單 (Room DB: `orders`)
- [ ] 平台/週期 (日、週、月、年) 多維度動態篩選
- [ ] 總筆數、總金額、超時補貼匯總統計
- [ ] 匯出 Excel / CSV 檔案

### 3. 設定模組 (Settings Module) - 進度: 0%
- [ ] App 主題/顏色切換
- [ ] 新增/管理自訂平台
- [ ] 版本顯示與更新公告
- [ ] 圖文使用教學說明
- [ ] Pro 訂閱進階版 UI 入口預留
