# Mobile Installation Guide

## 📱 APK Location
**File**: `E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk`  
**Size**: ~6.7 MB  
**Build Date**: April 23, 2026 (20:00)  
**Build Status**: ✅ SUCCESS

---

## 🚀 Installation Methods

### Method 1: USB Cable (Recommended)

#### Prerequisites:
1. **Enable Developer Options** on your Android phone:
   - Go to `Settings` → `About Phone`
   - Tap `Build Number` 7 times until you see "You are now a developer"

2. **Enable USB Debugging**:
   - Go to `Settings` → `Developer Options`
   - Enable `USB Debugging`
   - Enable `Install via USB` (if available)

#### Installation Steps:
1. Connect your phone to PC via USB cable
2. On your phone, allow USB debugging when prompted
3. Open PowerShell in the project directory
4. Run the following command:
   ```powershell
   cd E:\sjm\MyHrms
   .\gradlew.bat installDebug
   ```
5. The app will be automatically installed on your phone
6. Look for "MyHrms" icon on your phone

**OR manually using ADB:**
```powershell
adb install -r "E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk"
```

---

### Method 2: Transfer APK File

#### Steps:
1. **Copy APK to Phone**:
   - Connect phone via USB
   - Copy `app-debug.apk` to your phone's Downloads folder
   - Or use cloud storage (Google Drive, Telegram, WhatsApp, etc.)

2. **Install on Phone**:
   - Open `Files` or `My Files` app on your phone
   - Navigate to `Downloads` folder
   - Tap on `app-debug.apk`
   - If prompted, enable "Install from Unknown Sources" for that app
   - Tap `Install`
   - Wait for installation to complete
   - Tap `Open`

---

### Method 3: Wireless ADB (Advanced)

#### Prerequisites:
- Phone and PC on same WiFi network
- USB debugging enabled

#### Steps:
1. Connect phone via USB first
2. Run these commands:
   ```powershell
   adb tcpip 5555
   adb connect <your-phone-ip>:5555
   ```
3. Disconnect USB cable
4. Install wirelessly:
   ```powershell
   adb install -r "E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk"
   ```

---

## ⚙️ First Time Setup

After installing the app:

1. **Open the app** - Look for "MyHrms" icon
2. **Grant Permissions** when prompted:
   - Camera (for photo capture)
   - Storage (for saving photos)
   - Location (if required)
3. **Login** with your credentials
4. **Test the new Machine Selection feature**:
   - Navigate to `Attendance Entry`
   - Select Employee and Designation
   - Tap on `Machine Numbers` field
   - Try the multi-select with search

---

## 🔧 Quick Install Command

**Easiest method** - Just run this in PowerShell:

```powershell
cd E:\sjm\MyHrms
.\gradlew.bat installDebug
```

This will:
- Build the APK (if needed)
- Install directly to connected phone
- Launch the app automatically

---

## ❌ Troubleshooting

### "Installation Blocked" Error
**Solution**: Enable "Install Unknown Apps" permission
- Go to `Settings` → `Security` → `Install Unknown Apps`
- Select the app you're using to install (Files, Chrome, etc.)
- Enable "Allow from this source"

### "App Not Installed" Error
**Solution**: 
- Uninstall the existing app first
- Or use `-r` flag: `adb install -r app-debug.apk`

### "Device Unauthorized" Error
**Solution**:
- Disconnect USB
- Disable USB debugging
- Re-enable USB debugging
- Reconnect USB
- Accept the authorization prompt on phone

### ADB Not Found
**Solution**:
- Android SDK Platform Tools not installed
- Install Android Studio or download Platform Tools separately
- Add to PATH environment variable

---

## 📋 System Requirements

### Minimum:
- Android 5.0 (API 21) or higher
- ~20 MB free space
- Internet connection for API calls

### Recommended:
- Android 8.0 or higher
- 2GB RAM
- Stable internet connection

---

## 🔄 Updating the App

To rebuild and reinstall after making changes:

```powershell
cd E:\sjm\MyHrms
.\gradlew.bat clean
.\gradlew.bat installDebug
```

This will:
1. Clean previous build
2. Rebuild the APK
3. Install updated version

---

## 📞 Testing Checklist

After installation, verify:
- [ ] App opens successfully
- [ ] Login works
- [ ] Attendance Dashboard loads
- [ ] Attendance Entry form works
- [ ] Machine Selection feature works
- [ ] Camera capture works (if applicable)
- [ ] Data saves correctly
- [ ] Backend API connectivity

---

## 🌐 Backend Configuration

Make sure your Flask backend is running and accessible:

1. **Check backend URL** in the app:
   - Usually in `RetrofitClient.kt` or similar
   - Example: `http://192.168.1.100:5000/`

2. **Start Flask server**:
   ```powershell
   cd E:\sjm\MyHrms
   python app.py
   ```

3. **Test API**:
   ```powershell
   curl http://localhost:5000/employees
   ```

4. **Network access**:
   - If testing on real device, use PC's IP address
   - Make sure phone is on same network
   - Check firewall settings

---

## 📱 APK Details

**Package Name**: com.example.myhrms  
**Version**: Check `app/build.gradle.kts`  
**Build Type**: Debug  
**Signed**: Debug keystore  

---

## ✅ Installation Complete!

Your MyHrms app is now installed and ready to use with the new Machine Selection feature implemented on April 23, 2026.

**Features included**:
- ✅ Attendance Dashboard
- ✅ Attendance Entry
- ✅ On Boarding
- ✅ Attendance Update
- ✅ Machine Selection (Multi-select with search)
- ✅ **Employee Branch Validation** - NEW FIX!

---

**Latest Changes (April 23, 2026 20:00)**:
- **Fixed:** GET /employee/{emp_code} now sends branch_id parameter
- **Benefit:** Proper employee validation within selected branch
- **Result:** No more 400 errors on employee lookup

---

**Last Build**: April 23, 2026 20:00  
**APK Size**: 6.7 MB  
**Status**: Ready for Installation

