# 📱 Installation Guide - Machine Name Fix

## ✅ Status
- ✅ Backend code updated
- ✅ Frontend code updated
- ✅ APK built successfully
- 📍 Ready to install on mobile

## 📦 APK Location
```
E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

## 🔧 Installation Methods

### Method 1: USB Connection (Recommended)

1. **Connect your phone via USB cable**
   - Enable USB Debugging on your phone
   - Settings > Developer Options > USB Debugging

2. **Install using ADB:**
   ```powershell
   adb devices
   adb install -r E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
   ```

3. **Open the app and test**

### Method 2: Direct Transfer

1. **Copy APK to your phone:**
   - Copy `E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk` to your phone's Download folder

2. **Install from phone:**
   - Open Files app or Downloads
   - Tap on `app-debug.apk`
   - Allow installation from unknown sources if prompted
   - Tap Install

### Method 3: Using Android Studio

1. **Open Android Studio**
2. **Click Run (Green Play button)** or press `Shift+F10`
3. **Select your connected device**
4. App will be installed automatically

## 🧪 Testing the Fix

After installation:

1. **Open MyHRMS App**
2. **Login**
3. **Go to Mark Attendance**
4. **Select Company, Branch, Department, Sub-Department**
5. **Select a Designation/Occupation**
6. **Tap on "Machine Numbers" field**
7. **Verify machines show proper names:**
   - ✅ Should show: "H06 HESS HELPER LINE NO 6"
   - ❌ Not: "No name"

## 📊 Expected Results

**Machine Selection Dialog:**
```
Select Machine Numbers
[Search box]

☐ H06 HESS HELPER LINE NO 6
☐ H07 HESS HELPER LINE NO 7
☐ H08 HESS HELPER LINE NO 8
☐ H09 HESS HELPER LINE NO 9

0 machine(s) selected
[CANCEL]  [OK]
```

## 🔍 Backend Verification

To verify backend is returning correct data:

```powershell
# Test API endpoint
curl "http://YOUR_SERVER_IP:5051/machines?designation_id=1"
```

Expected response:
```json
{
  "status": "success",
  "data": [
    {
      "id": 1486,
      "name": "H06 HESS HELPER LINE NO 6",
      "mech_code": "H06",
      "machine_no": "1486"
    }
  ]
}
```

## ⚠️ Important Notes

1. **Backend Server Must Be Running:**
   ```powershell
   cd E:\sjm\MyHrms
   python app.py
   ```
   Server should be running on `http://localhost:5051` or your network IP.

2. **App Configuration:**
   - Make sure the app is configured with the correct backend URL
   - Check `RetrofitClient.kt` for the BASE_URL

3. **Network Connection:**
   - Phone and backend server must be on the same network
   - OR use proper IP address if on different networks

4. **Cache Clear (if still showing "No name"):**
   - Go to Settings > Apps > MyHRMS
   - Clear Cache
   - Clear Data (will require re-login)
   - Reinstall the app

## 🐛 Troubleshooting

### Still showing "No name"?

1. **Check backend is running:**
   ```powershell
   netstat -an | findstr "5051"
   ```

2. **Check API response manually:**
   Use a browser or Postman to test the `/machines` endpoint

3. **Verify app is using new APK:**
   - Uninstall old app completely
   - Install new APK
   - Restart phone if needed

4. **Check Android Studio Logcat:**
   - Connect phone via USB
   - Open Logcat in Android Studio
   - Filter by "MyHRMS" or "Machine"
   - Look for API response logs

## 📝 Changes Made

### Backend (app.py)
```python
# Now explicitly maps database columns to frontend expected fields
machines.append({
    'id': m['machine_id'],
    'name': m['machine_name'],
    'mech_code': m['mach_code'],
    'machine_no': m['mech_shr_code']
})
```

### Frontend (MachineResponse.kt)
```kotlin
fun getDisplayName(): String {
    val machineName = name ?: ""
    return when {
        machineName.isNotEmpty() -> machineName  // Prioritize full name
        code.isNotEmpty() -> code
        else -> "No name"
    }
}
```

### Adapter (MachineSelectionAdapter.kt)
```kotlin
// Fixed nullable handling in filter
(it.machineNo?.contains(query, ignoreCase = true) == true) ||
(it.name?.contains(query, ignoreCase = true) == true)
```

---

**Build Date:** April 23, 2026  
**APK File:** app-debug.apk  
**Backend Status:** Updated ✅  
**Frontend Status:** Updated ✅  
**Build Status:** Success ✅

