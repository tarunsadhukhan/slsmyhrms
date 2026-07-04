# Drawing Meter Entry - Complete Implementation Guide

**Date:** May 6, 2026  
**Status:** ✅ Android Complete | ⏳ Backend Integration Pending

---

## 🎯 QUICK START

### 1. Copy Backend Files
```powershell
cd E:\sjm\MyHrms
.\copy_drawing_to_backend.ps1
```

### 2. Setup Database
```sql
-- Run this file in MySQL
E:\sjm\MyHrms\database_setup_drawing.sql
```

### 3. Configure Backend
- Add blueprint registration to `e:\sjm\attendancesystem\app.py`
- See detailed steps in `BACKEND_INTEGRATION_REFERENCE.md`

### 4. Build Android App
```powershell
cd E:\sjm\MyHrms
.\gradlew assembleDebug
```

---

## 📁 FILE LOCATIONS

### Reference Files (Created)
```
E:\sjm\MyHrms\
├── src\drawing\
│   ├── __init__.py          ← Blueprint initialization
│   └── routes.py            ← All 5 API endpoints
├── BACKEND_INTEGRATION_REFERENCE.md  ← Complete guide
├── copy_drawing_to_backend.ps1       ← Copy script
└── database_setup_drawing.sql        ← Database setup
```

### Backend Files (To Be Created)
```
e:\sjm\attendancesystem\
├── app.py                   ← Register drawing_bp here
└── src\
    ├── __init__.py         ← Empty file
    ├── database.py         ← get_db() function
    └── drawing\
        ├── __init__.py     ← Copy from reference
        └── routes.py       ← Copy from reference
```

### Android Files (Already Complete)
```
E:\sjm\MyHrms\app\src\main\
├── java\com\example\myhrms\
│   ├── DrawingMeterEntryActivity.kt
│   ├── adapter\DrawingSummaryAdapter.kt
│   └── api\
│       ├── ApiRoutes.kt
│       ├── ApiService.kt
│       └── DrawingResponse.kt
└── res\layout\
    ├── activity_drawing_meter_entry.xml
    └── item_drawing_summary.xml
```

---

## 🔧 BACKEND PATHS (IMPORTANT!)

```
ACTUAL BACKEND:    e:\sjm\attendancesystem
DATABASE:          sjm @ 13.126.47.172
REFERENCE CODE:    E:\sjm\MyHrms\src\drawing\
PORT:              5051
```

---

## 📋 IMPLEMENTATION CHECKLIST

### Backend Setup:
- [ ] Run `copy_drawing_to_backend.ps1`
- [ ] Create `e:\sjm\attendancesystem\src\database.py`
- [ ] Add blueprint registration to `app.py`
- [ ] Run `database_setup_drawing.sql`
- [ ] Start backend: `python e:\sjm\attendancesystem\app.py`
- [ ] Test endpoints (see reference doc)

### Android Build:
- [ ] Build APK: `.\gradlew assembleDebug`
- [ ] Install on device
- [ ] Test feature end-to-end

---

## 🗄️ DATABASE TABLES

### tbl_drawing_mst (Master)
- Stores machine information
- Fields: mc_id, mc_short_name, shed_type, cont_meter, branch_id

### tbl_daily_drawing (Transaction)
- Stores daily meter readings
- Fields: date, spell_id, shed_type, mc_id, opening_meter, closing_meter, unit, hours, eff

---

## 🔗 API ENDPOINTS

All prefixed with `/drawing`:

1. **GET /drawing/sheds** - Get unique shed types
2. **GET /drawing/machines** - Get machines by shed
3. **GET /drawing/opening-meter** - Get previous closing meter
4. **POST /drawing/entry** - Save meter entry
5. **GET /drawing/summary** - Get summary list

See `BACKEND_INTEGRATION_REFERENCE.md` for detailed API documentation.

---

## 🧮 CALCULATIONS

```
Unit = Closing Meter - Opening Meter
Efficiency% = ((Unit / Hours * 8) / Const * 100) rounded to 2 decimals
```

**Default Const Value:** 100.0

---

## 📖 DOCUMENTATION FILES

1. **BACKEND_INTEGRATION_REFERENCE.md** ← READ THIS FIRST
   - Complete backend integration guide
   - Database setup instructions
   - API endpoint documentation
   - Troubleshooting tips

2. **copy_drawing_to_backend.ps1**
   - PowerShell script to copy files
   - Automatically creates folders
   - Run this to set up backend structure

3. **database_setup_drawing.sql**
   - SQL script to create tables
   - Includes sample data
   - Run in MySQL Workbench or CLI

4. **THIS FILE (README_DRAWING_METER_ENTRY.md)**
   - Quick start guide
   - File locations
   - Checklist

---

## ⚡ QUICK COMMANDS

### Copy Backend Files
```powershell
cd E:\sjm\MyHrms
.\copy_drawing_to_backend.ps1
```

### Setup Database
```bash
mysql -h 13.126.47.172 -u myroot -p sjm < database_setup_drawing.sql
```

### Start Backend
```powershell
cd e:\sjm\attendancesystem
python app.py
```

### Build Android
```powershell
cd E:\sjm\MyHrms
.\gradlew assembleDebug
```

### Test API
```bash
curl http://13.126.47.172:5051/drawing/sheds?branch_id=29
```

---

## 🎯 FEATURE OVERVIEW

### Android UI
- Date picker (dd-MM-yyyy format)
- Spell dropdown (shifts)
- Shed dropdown (unique types)
- Machine buttons (horizontal scroll)
- Auto-fetch opening meter
- Auto-calculate unit & efficiency
- Save button
- Summary list (RecyclerView)

### Backend API
- 5 RESTful endpoints
- MySQL database integration
- Automatic calculations
- Unique constraint on date+spell+machine
- Branch filtering support

---

## 📱 USER WORKFLOW

1. Open app → Production Dashboard → Drawing Meter Entry
2. Select Date, Spell, and Shed
3. Machine buttons appear - tap to select
4. Opening meter auto-loads from previous entry
5. Enter Closing meter and Hours
6. Unit and Efficiency calculate automatically
7. Tap Save
8. Entry appears in Summary list below

---

## 🔍 TESTING

After setup, test this flow:

1. Backend running on port 5051 ✓
2. Database tables created with sample data ✓
3. Android app installed on device ✓
4. Can navigate to Drawing Meter Entry ✓
5. Sheds load in dropdown ✓
6. Machines appear as buttons ✓
7. Opening meter fetches correctly ✓
8. Save works and summary updates ✓

---

## 🆘 SUPPORT

If you encounter issues:

1. Check `BACKEND_INTEGRATION_REFERENCE.md` Troubleshooting section
2. Verify backend is running: `http://13.126.47.172:5051/`
3. Check database tables exist: `SHOW TABLES LIKE 'tbl_%drawing%';`
4. Test endpoints with curl commands
5. Check Android app logs in Logcat

---

## ✅ IMPLEMENTATION STATUS

| Component | Status |
|-----------|--------|
| Android UI | ✅ Complete |
| Android API Layer | ✅ Complete |
| Backend Routes (Reference) | ✅ Complete |
| Backend Integration | ⏳ Pending |
| Database Schema | ✅ Designed |
| Documentation | ✅ Complete |

---

**Next Action:** Run `copy_drawing_to_backend.ps1` and follow `BACKEND_INTEGRATION_REFERENCE.md`

**Last Updated:** May 6, 2026

