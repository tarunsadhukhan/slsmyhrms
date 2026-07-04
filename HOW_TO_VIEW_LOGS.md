# 📱 How to View Android Logs

## Method 1: Using ADB (Android Debug Bridge)

### Prerequisites
- USB Debugging enabled on your Android device
- Device connected to computer via USB
- ADB installed (comes with Android SDK)

### Steps

1. **Connect your device**
   ```powershell
   # Check if device is connected
   adb devices
   ```

2. **View all logs in real-time**
   ```powershell
   adb logcat
   ```

3. **View filtered logs (MACHINE_DEBUG tag)**
   ```powershell
   adb logcat -s MACHINE_DEBUG
   ```

4. **View multiple tags**
   ```powershell
   adb logcat MACHINE_DEBUG:D AttendanceActivity:D *:S
   ```

5. **Clear logs and start fresh**
   ```powershell
   adb logcat -c
   adb logcat
   ```

6. **Save logs to file**
   ```powershell
   adb logcat > E:\sjm\MyHrms\app_logs.txt
   ```

7. **View specific log levels**
   ```powershell
   # V = Verbose, D = Debug, I = Info, W = Warning, E = Error
   adb logcat *:E  # Show only errors
   adb logcat *:W  # Show warnings and errors
   adb logcat *:D  # Show debug and above
   ```

---

## Method 2: Using Android Studio (Logcat Window)

1. Open Android Studio
2. Connect your device via USB
3. Click **View** → **Tool Windows** → **Logcat**
4. Select your device from dropdown
5. Type in search box: `MACHINE_DEBUG` to filter logs
6. Click on filter dropdown to select log level (Verbose, Debug, Info, etc.)

---

## Method 3: View Logs in Code (Already Implemented)

In the code, we use Android's Log class:

```kotlin
android.util.Log.d("MACHINE_DEBUG", "Response code: ${response.code()}")
android.util.Log.d("MACHINE_DEBUG", "Total machines: ${data.size}")
android.util.Log.e("MACHINE_DEBUG", "API call failed", exception)
```

**Log Levels:**
- `Log.v()` - Verbose (lowest priority)
- `Log.d()` - Debug
- `Log.i()` - Info
- `Log.w()` - Warning
- `Log.e()` - Error (highest priority)

---

## Viewing Logs from Your App

### Filter by Your App Package Name
```powershell
adb logcat | findstr "com.example.myhrms"
```

### Filter by Tag
```powershell
adb logcat | findstr "MACHINE_DEBUG"
```

### Combined Filtering (PowerShell)
```powershell
adb logcat | Select-String -Pattern "MACHINE_DEBUG|AttendanceActivity"
```

---

## Common Issues

### Issue: "adb is not recognized"
**Solution:** Add Android SDK platform-tools to PATH
```powershell
# Find adb.exe location (usually):
# C:\Users\YourName\AppData\Local\Android\Sdk\platform-tools\

# Add to PATH environment variable
```

### Issue: "No devices found"
**Solution:** 
1. Enable USB Debugging on phone: Settings → Developer Options → USB Debugging
2. Reconnect USB cable
3. Allow USB debugging when prompted on phone
4. Run `adb devices` again

### Issue: Too many logs
**Solution:** Use filters:
```powershell
# Clear logs first
adb logcat -c

# Then filter by your tag
adb logcat -s MACHINE_DEBUG
```

---

## Quick Reference

| Command | Description |
|---------|-------------|
| `adb devices` | List connected devices |
| `adb logcat` | View all logs |
| `adb logcat -c` | Clear logs |
| `adb logcat -s TAG` | Filter by TAG |
| `adb logcat *:E` | Show only errors |
| `adb logcat > file.txt` | Save logs to file |
| `adb shell` | Open device shell |

---

**Date:** April 24, 2026  
**Status:** ✅ Documentation Complete

