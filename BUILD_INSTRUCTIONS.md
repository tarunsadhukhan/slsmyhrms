# 🔨 BUILD THE UPDATED APK - MANUAL STEPS

## ⚠️ PowerShell Execution Policy is Blocking Automated Build

Since the PowerShell execution policy is preventing automated builds, please follow these **MANUAL STEPS**:

---

## Option 1: Using Command Prompt (RECOMMENDED)

1. **Open Command Prompt** (cmd.exe)
2. **Navigate to project:**
   ```
   cd E:\sjm\MyHrms
   ```
3. **Run build:**
   ```
   gradlew.bat assembleDebug
   ```
4. **Wait for completion** (10-20 seconds)
5. **Look for:** `BUILD SUCCESSFUL`

---

## Option 2: Using PowerShell (Manual)

1. **Open PowerShell as Administrator**
2. **Navigate to project:**
   ```powershell
   cd E:\sjm\MyHrms
   ```
3. **Run build:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
4. **Wait for completion**

---

## Option 3: Using Batch File

1. **Double-click:** `E:\sjm\MyHrms\build_hours_fix.bat`
2. **Wait for build to complete**
3. **Press any key to close**

---

## Option 4: Using Android Studio

1. **Open Android Studio**
2. **Open Project:** E:\sjm\MyHrms
3. **Menu:** Build > Build Bundle(s) / APK(s) > Build APK(s)
4. **Wait for notification:** "APK(s) generated successfully"

---

## ✅ After Build Completes

### APK Location:
```
E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
```

### File Size:
Should be around 10-20 MB

### Next Steps:
1. ✅ Install APK on your Android device
2. ✅ **IMPORTANT:** Restart backend server using `restart_server.bat`
3. ✅ Test the hours field functionality

---

## 🧪 Testing After Installation

1. Open MyHrms app
2. Navigate to Drawing Meter Entry
3. **Test 1:** Hours should auto-populate when you open the screen
4. **Test 2:** Change spell → Hours should update
5. **Test 3:** Save entry → Hours should remain filled (not cleared)
6. **Test 4:** Save another entry → Hours should still be there

---

## ❌ If Build Fails

### Common Issues:

**Error: "SDK location not found"**
- Create `local.properties` file in E:\sjm\MyHrms
- Add line: `sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk`
- Adjust path to your actual Android SDK location

**Error: "Java version incompatible"**
- Ensure Java 11 or 17 is installed
- Set JAVA_HOME environment variable

**Error: "Gradle version too old"**
- Run: `gradlew.bat wrapper --gradle-version=8.0`

---

## 📋 Files Changed in This Build

✅ **DrawingMeterEntryActivity.kt**
   - Lines 108-117: Fixed spell change listener
   - Lines 178-183: Auto-fill hours on load
   - Lines 384-389: Preserve hours after save

✅ **Backend app.py**
   - Line 383: Returns 'shifts' instead of 'data'
   - **MUST restart server for this to work!**

---

## 🚀 Quick Build Command Reference

### Command Prompt:
```batch
cd E:\sjm\MyHrms
gradlew.bat assembleDebug
```

### PowerShell:
```powershell
cd E:\sjm\MyHrms
.\gradlew.bat assembleDebug
```

### Clean Build (if having issues):
```batch
cd E:\sjm\MyHrms
gradlew.bat clean assembleDebug
```

---

## ✅ Build Status

- ✅ Code changes complete
- ✅ Frontend fixes applied
- ✅ Backend fixes applied
- ⏳ **APK BUILD PENDING - Please build manually using steps above**

---

**Once built, follow instructions in `DEPLOY_HOURS_FIX.md` for deployment and testing!**

