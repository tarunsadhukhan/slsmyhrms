# How to View Android Logs (Logcat)

## 📱 Method 1: Using Android Studio (Recommended)

### Prerequisites:
- Phone connected to PC via USB
- USB Debugging enabled on phone

### Steps:

1. **Connect Phone to PC**
   - Connect phone via USB cable
   - On phone: Allow USB debugging when prompted

2. **Open Android Studio**
   - Open the MyHrms project

3. **Open Logcat Window**
   - Bottom of Android Studio: Click **"Logcat"** tab
   - OR: `View` → `Tool Windows` → `Logcat`

4. **Filter by Tag**
   - In the search box at top, type: `MACHINE_DEBUG`
   - OR use filter: `tag:MACHINE_DEBUG`

5. **Run the App**
   - Go to Attendance screen
   - Tap "Machine Numbers" field
   - Watch the Logcat window - you'll see:
   ```
   MACHINE_DEBUG: Response code: 200
   MACHINE_DEBUG: Response successful: true
   MACHINE_DEBUG: Response body: MachineResponse(...)
   MACHINE_DEBUG: API Status: success
   MACHINE_DEBUG: Total in response: 130
   MACHINE_DEBUG: Raw JSON: {...}
   ```

---

## 📱 Method 2: Using ADB Command Line (Quick & Easy)

### Prerequisites:
- Phone connected to PC via USB
- USB Debugging enabled
- ADB installed (comes with Android Studio)

### Steps:

1. **Enable USB Debugging on Phone**
   - Settings → About Phone → Tap "Build Number" 7 times
   - Settings → Developer Options → Enable "USB Debugging"
   - Connect phone to PC via USB
   - Allow USB debugging when prompted

2. **Open PowerShell/Command Prompt**
   ```powershell
   cd C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools
   ```

3. **Check Device Connected**
   ```powershell
   .\adb devices
   ```
   Should show your device listed

4. **View All Logs in Real-Time**
   ```powershell
   .\adb logcat
   ```

5. **Filter by MACHINE_DEBUG Tag Only**
   ```powershell
   .\adb logcat -s MACHINE_DEBUG
   ```

6. **Clear Old Logs First, Then Watch**
   ```powershell
   .\adb logcat -c
   .\adb logcat -s MACHINE_DEBUG
   ```

7. **Save Logs to File**
   ```powershell
   .\adb logcat -s MACHINE_DEBUG > Desktop\machine_logs.txt
   ```

---

## 📱 Method 3: Using ADB Shortcut (Easiest!)

### Quick Command (Copy-Paste Ready):

```powershell
# Clear old logs and watch MACHINE_DEBUG in real-time:
C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -c ; C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -s MACHINE_DEBUG
```

---

## 🎯 What to Look For

When you tap "Machine Numbers" after selecting designation, you should see:

### ✅ **If Working Correctly:**
```
D/MACHINE_DEBUG: Response code: 200
D/MACHINE_DEBUG: Response successful: true
D/MACHINE_DEBUG: API Status: success
D/MACHINE_DEBUG: Total in response: 130
D/MACHINE_DEBUG: Data list size: 130
D/MACHINE_DEBUG: Machine #1:
D/MACHINE_DEBUG: ID: 1344
D/MACHINE_DEBUG: name: 1001 WINDING1001
D/MACHINE_DEBUG: mech_code: 1001
D/MACHINE_DEBUG: machine_no: 1001
D/MACHINE_DEBUG: Valid machines: 130
D/MACHINE_DEBUG: Invalid machines: 0
```

### ❌ **If Still Broken:**
```
D/MACHINE_DEBUG: Response code: 200
D/MACHINE_DEBUG: Response successful: true
D/MACHINE_DEBUG: API Status: success
D/MACHINE_DEBUG: Total in response: 130
D/MACHINE_DEBUG: Data list size: 130
D/MACHINE_DEBUG: ID: NULL
D/MACHINE_DEBUG: name: NULL
D/MACHINE_DEBUG: Valid machines: 0
D/MACHINE_DEBUG: Invalid machines: 130
```

---

## 📸 What to Do Next

1. **Run one of the commands above**
2. **Open the app** on phone
3. **Go to Attendance** screen
4. **Tap "Machine Numbers"** (after selecting designation)
5. **Copy the log output** and send it to me

---

## 🔧 Troubleshooting

### "adb: command not found"
- **Solution**: Use full path:
  ```powershell
  C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -s MACHINE_DEBUG
  ```

### "No devices/emulators found"
- **Check**: Phone is connected via USB
- **Check**: USB debugging is enabled
- **Try**: Unplug and replug USB cable
- **Try**: On phone, revoke and re-allow USB debugging authorization

### Phone Not Showing in ADB
1. On phone: Settings → Developer Options → Revoke USB Debugging Authorization
2. Unplug USB
3. Plug USB back in
4. Allow USB debugging when prompted
5. Run: `adb devices` again

---

## 💡 Quick Test Command

**Copy and paste this into PowerShell:**

```powershell
$adb = "C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $adb) {
    Write-Host "✅ ADB Found!" -ForegroundColor Green
    & $adb devices
    Write-Host "`nStarting logcat with MACHINE_DEBUG filter..." -ForegroundColor Cyan
    Write-Host "Now open the app and tap Machine Numbers field..." -ForegroundColor Yellow
    & $adb logcat -c
    & $adb logcat -s MACHINE_DEBUG
} else {
    Write-Host "❌ ADB not found at expected location" -ForegroundColor Red
    Write-Host "Please install Android Studio or provide ADB path" -ForegroundColor Yellow
}
```

---

## 📋 Summary

**Easiest Method**: 
1. Connect phone via USB
2. Enable USB debugging
3. Open PowerShell
4. Run: `C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat -s MACHINE_DEBUG`
5. Open app and tap "Machine Numbers"
6. Copy the logs and send them to me

The logs will show us EXACTLY what data the app is receiving from the API!

