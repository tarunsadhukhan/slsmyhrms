# Debug APK Installation Guide

## 📦 New Debug APK Ready!

**Location**: `E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk`

---

## 🔍 What This Debug Version Does

When you tap the **Machine Numbers** field after selecting a designation, the app will:

1. **Load machine data from API**
2. **Show a popup with raw data** - this will display:
   - Total number of machines loaded
   - First 3 machines with ALL their fields:
     - `ID` - Machine database ID
     - `name` - The name field from API
     - `mech_code` - The machine code
     - `machine_no` - The machine number
     - `getDisplayName()` - What the display function returns
3. **Click OK to proceed** - After you click OK, it will load the machines into the list

---

## 📱 Installation Steps

### Step 1: Uninstall Old App
```
Settings → Apps → MyHRMS → Uninstall
```

### Step 2: Transfer APK to Phone
- Location: `E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk`
- Use WhatsApp, Email, or USB cable

### Step 3: Install New APK
- Tap the APK file on your phone
- Allow installation from unknown sources if asked
- Install

---

## 🧪 Testing Steps

1. **Open MyHRMS app**
2. **Navigate to Mark Attendance**
3. **Select**:
   - Company: Shiv Jyoti Machine (ID: 1)
   - Branch: SJM WINDING - BHIWANDI (ID: 29)
   - Department: Any department
   - Designation: **WINDING (ID: 199)**

4. **Tap "Machine Numbers" field**

5. **Debug Popup Will Appear** showing something like:
   ```
   Loaded 130 machines:

   ID: 1344
   name: 1001 WINDING1001
   mech_code: 1001
   machine_no: 1001
   getDisplayName(): 1001 WINDING1001
   ---

   ID: 1345
   name: 1002 WINDING1002
   mech_code: 1002
   machine_no: 1002
   getDisplayName(): 1002 WINDING1002
   ---

   ID: 1346
   name: 1003 WINDING1003
   mech_code: 1003
   machine_no: 1003
   getDisplayName(): 1003 WINDING1003
   ---
   ```

6. **Take a screenshot of this popup** and send it to me

7. **Click OK** to see the machine list

---

## 🎯 What We're Looking For

The debug popup will tell us:

### ✅ If this shows up correctly:
```
name: 1001 WINDING1001
getDisplayName(): 1001 WINDING1001
```
**→ Then the API is working and data is being received correctly**
**→ The problem is in how the adapter displays it**

### ❌ If this shows up:
```
name: NULL
getDisplayName(): No name
```
**→ Then the API data is not being parsed correctly**
**→ The problem is in the data model or API parsing**

---

## 📸 Please Send Me

1. **Screenshot of the debug popup**
2. **Tell me what you see in the machine list after clicking OK**:
   - Does it show "No name"?
   - Does it show "1001 WINDING1001"?
   - Something else?

---

## 🔧 Backend Status

Make sure backend is running:
```powershell
netstat -ano | findstr :5051
```

Should show Python process listening on port 5051.

If not running, start it:
```powershell
cd E:\sjm\MyHrms
python app.py
```

---

## ⚡ Quick Command to Copy APK

If you want to copy the APK to desktop for easy transfer:
```powershell
Copy-Item "E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk" "$env:USERPROFILE\Desktop\MyHRMS-Debug.apk"
```

Then you can easily attach it to WhatsApp/Email from desktop.

---

**This debug version will help us see exactly what data the app is receiving from the API!** 📊

