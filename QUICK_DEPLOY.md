# Drawing Meter Entry - Quick Deploy

## 🚀 Quick Deployment Steps

### 1. Backend (Flask)
```powershell
# Restart Flask server to load updated app.py
cd E:\sjm\attendancesystem
python app.py
```

### 2. Android App
```powershell
# Install updated APK
cd E:\sjm\MyHrms
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 3. Test
1. Open Drawing Meter Entry
2. Select spell → Hours should auto-fill
3. Tap shed button → Should turn green
4. Tap machine → Should turn green
5. Enter integer meters → Save

---

## ✅ What Changed

- **Shed**: Dropdown → Buttons (horizontal scroll)
- **Meters**: Decimal → Integer input
- **Hours**: Empty → Auto-filled from spell
- **Machine**: "MC" + id → Short name only

---

## 📝 Files Changed

**Android:**
- `ShiftResponse.kt` - Added working_hours
- `activity_drawing_meter_entry.xml` - Button grid + integer inputs
- `DrawingMeterEntryActivity.kt` - Button logic + auto-fill

**Backend:**
- `app.py` - Added working_hours to /shifts endpoint

---

**Status:** ✅ Build successful, ready to deploy!

