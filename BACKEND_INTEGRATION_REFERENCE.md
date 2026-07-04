# Drawing Meter Entry - Backend Integration Reference

**Date:** May 6, 2026  
**Feature:** Drawing Meter Entry for Production Tracking

---

## 📁 IMPORTANT PATHS

### Backend Location
```
ACTUAL BACKEND: e:\sjm\attendancesystem
REFERENCE CODE: E:\sjm\MyHrms\src\drawing\
```

⚠️ **Note:** The code in `E:\sjm\MyHrms\src\drawing\` is REFERENCE ONLY.  
Copy it to your actual backend at `e:\sjm\attendancesystem\src\drawing\`

### Android App Location
```
ANDROID APP: E:\sjm\MyHrms\
```

---

## 🗄️ DATABASE INFORMATION

### Database Name
```
Database: sjm
Host: 13.126.47.172
User: myroot
```

### Required Tables

#### 1. tbl_drawing_mst (Master Table)
```sql
CREATE TABLE IF NOT EXISTS tbl_drawing_mst (
    mc_id INT PRIMARY KEY AUTO_INCREMENT,
    mc_short_name VARCHAR(50) NOT NULL,
    shed_type VARCHAR(50) NOT NULL,
    cont_meter DECIMAL(10,2) DEFAULT 0,
    branch_id INT,
    active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shed_type (shed_type),
    INDEX idx_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Sample Data:**
```sql
INSERT INTO tbl_drawing_mst (mc_short_name, shed_type, cont_meter, branch_id) VALUES
('D1', 'Shed A', 1000.00, 29),
('D2', 'Shed A', 1200.00, 29),
('D3', 'Shed B', 1500.00, 29),
('D4', 'Shed B', 1800.00, 29);
```

#### 2. tbl_daily_drawing (Transaction Table)
```sql
CREATE TABLE IF NOT EXISTS tbl_daily_drawing (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    spell_id INT NOT NULL,
    shed_type VARCHAR(50),
    mc_id INT NOT NULL,
    opening_meter DECIMAL(10,2) DEFAULT 0,
    closing_meter DECIMAL(10,2) DEFAULT 0,
    unit DECIMAL(10,2) DEFAULT 0,
    hours DECIMAL(5,2) DEFAULT 0,
    eff DECIMAL(5,2) DEFAULT 0,
    branch_id INT,
    user_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_entry (date, spell_id, mc_id),
    FOREIGN KEY (mc_id) REFERENCES tbl_drawing_mst(mc_id),
    INDEX idx_date_spell (date, spell_id),
    INDEX idx_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 📂 BACKEND FILE STRUCTURE

### Step 1: Create Drawing Module in Actual Backend

```
e:\sjm\attendancesystem\
├── app.py (or main.py)
└── src\
    └── drawing\
        ├── __init__.py
        └── routes.py
```

### Step 2: Copy Files

**From Reference Location:**
```
E:\sjm\MyHrms\src\drawing\__init__.py
E:\sjm\MyHrms\src\drawing\routes.py
```

**To Actual Backend:**
```
e:\sjm\attendancesystem\src\drawing\__init__.py
e:\sjm\attendancesystem\src\drawing\routes.py
```

---

## 🔧 BACKEND INTEGRATION STEPS

### 1. Create database.py (if not exists)

**File:** `e:\sjm\attendancesystem\src\database.py`

```python
"""
Database connection module
"""
import mysql.connector

DB_CONFIG = {
    'host': '13.126.47.172',
    'user': 'myroot',
    'password': 'deb#9876',
    'database': 'sjm'
}

def get_db():
    """Get database connection"""
    return mysql.connector.connect(**DB_CONFIG)
```

### 2. Register Blueprint in Main App

**File:** `e:\sjm\attendancesystem\app.py` (or your main Flask file)

Add these lines:

```python
# Import the drawing blueprint
from src.drawing import drawing_bp

# Register the blueprint
app.register_blueprint(drawing_bp)
```

**Full Example:**
```python
from flask import Flask
from src.drawing import drawing_bp

app = Flask(__name__)

# Register blueprints
app.register_blueprint(drawing_bp)

# ... rest of your app code ...

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5051, debug=True)
```

### 3. Create Database Tables

Connect to MySQL and run:

```sql
USE sjm;

-- Create master table
CREATE TABLE IF NOT EXISTS tbl_drawing_mst (
    mc_id INT PRIMARY KEY AUTO_INCREMENT,
    mc_short_name VARCHAR(50) NOT NULL,
    shed_type VARCHAR(50) NOT NULL,
    cont_meter DECIMAL(10,2) DEFAULT 0,
    branch_id INT,
    active TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shed_type (shed_type),
    INDEX idx_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create transaction table
CREATE TABLE IF NOT EXISTS tbl_daily_drawing (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    spell_id INT NOT NULL,
    shed_type VARCHAR(50),
    mc_id INT NOT NULL,
    opening_meter DECIMAL(10,2) DEFAULT 0,
    closing_meter DECIMAL(10,2) DEFAULT 0,
    unit DECIMAL(10,2) DEFAULT 0,
    hours DECIMAL(5,2) DEFAULT 0,
    eff DECIMAL(5,2) DEFAULT 0,
    branch_id INT,
    user_id INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_entry (date, spell_id, mc_id),
    FOREIGN KEY (mc_id) REFERENCES tbl_drawing_mst(mc_id),
    INDEX idx_date_spell (date, spell_id),
    INDEX idx_branch (branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert sample data
INSERT INTO tbl_drawing_mst (mc_short_name, shed_type, cont_meter, branch_id) VALUES
('D1', 'Shed A', 1000.00, 29),
('D2', 'Shed A', 1200.00, 29),
('D3', 'Shed B', 1500.00, 29),
('D4', 'Shed B', 1800.00, 29),
('D5', 'Shed C', 2000.00, 29);
```

### 4. Test Backend Endpoints

Start your backend server:
```powershell
cd e:\sjm\attendancesystem
python app.py
```

Test the endpoints:

```bash
# Test 1: Get Sheds
curl http://localhost:5051/drawing/sheds?branch_id=29

# Test 2: Get Machines
curl http://localhost:5051/drawing/machines?shed_type=Shed%20A&branch_id=29

# Test 3: Get Opening Meter
curl "http://localhost:5051/drawing/opening-meter?date=2026-05-06&spell_id=1&mc_id=1"

# Test 4: Save Entry
curl -X POST http://localhost:5051/drawing/entry \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-05-06",
    "spell_id": 1,
    "shed_type": "Shed A",
    "mc_id": 1,
    "opening_meter": 1000.00,
    "closing_meter": 1500.00,
    "hours": 8.0,
    "const_value": 100.0,
    "branch_id": 29,
    "user_id": 1
  }'

# Test 5: Get Summary
curl "http://localhost:5051/drawing/summary?date=2026-05-06&spell_id=1&branch_id=29"
```

---

## 🔗 API ENDPOINTS

All endpoints are prefixed with `/drawing`

### 1. GET /drawing/sheds
**Purpose:** Get unique shed types  
**Query Params:** `?branch_id=<id>` (optional)  
**Response:**
```json
{
  "status": "success",
  "sheds": ["Shed A", "Shed B", "Shed C"]
}
```

### 2. GET /drawing/machines
**Purpose:** Get machines for shed type  
**Query Params:** `?shed_type=<type>&branch_id=<id>`  
**Response:**
```json
{
  "status": "success",
  "machines": [
    {
      "mc_id": 1,
      "mc_short_name": "D1",
      "cont_meter": 1000.00
    }
  ]
}
```

### 3. GET /drawing/opening-meter
**Purpose:** Get opening meter (previous closing)  
**Query Params:** `?date=YYYY-MM-DD&spell_id=<id>&mc_id=<id>`  
**Response:**
```json
{
  "status": "success",
  "opening_meter": 1500.00
}
```

### 4. POST /drawing/entry
**Purpose:** Save meter entry  
**Body:**
```json
{
  "date": "2026-05-06",
  "spell_id": 1,
  "shed_type": "Shed A",
  "mc_id": 1,
  "opening_meter": 1000.00,
  "closing_meter": 1500.00,
  "hours": 8.0,
  "const_value": 100.0,
  "branch_id": 29,
  "user_id": 1
}
```
**Response:**
```json
{
  "status": "success",
  "message": "Entry saved successfully",
  "id": 1,
  "unit": 500.00,
  "eff": 50.00
}
```

### 5. GET /drawing/summary
**Purpose:** Get summary list for date+spell  
**Query Params:** `?date=YYYY-MM-DD&spell_id=<id>&branch_id=<id>`  
**Response:**
```json
{
  "status": "success",
  "summary": [
    {
      "mc_id": 1,
      "mc_short_name": "D1",
      "unit": 500.00,
      "eff": 50.00
    }
  ]
}
```

---

## 📱 ANDROID APP CONFIGURATION

### Update RetrofitClient Base URL (if needed)

**File:** `E:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\RetrofitClient.kt`

Ensure BASE_URL points to your backend:
```kotlin
private const val BASE_URL = "http://13.126.47.172:5051/"
```

---

## ✅ VERIFICATION CHECKLIST

### Backend Setup:
- [ ] Created `e:\sjm\attendancesystem\src\drawing\` folder
- [ ] Copied `__init__.py` from reference
- [ ] Copied `routes.py` from reference
- [ ] Created/updated `database.py` with correct DB config
- [ ] Registered `drawing_bp` in main `app.py`
- [ ] Started backend server successfully

### Database Setup:
- [ ] Connected to MySQL database `sjm`
- [ ] Created `tbl_drawing_mst` table
- [ ] Created `tbl_daily_drawing` table
- [ ] Inserted sample machine data
- [ ] Verified foreign key constraints work

### Testing:
- [ ] Tested GET /drawing/sheds - returns shed list
- [ ] Tested GET /drawing/machines - returns machine list
- [ ] Tested GET /drawing/opening-meter - returns previous closing
- [ ] Tested POST /drawing/entry - saves entry successfully
- [ ] Tested GET /drawing/summary - returns summary list

### Android App:
- [ ] Built Android app: `.\gradlew assembleDebug`
- [ ] Installed APK on device
- [ ] Launched Drawing Meter Entry from menu
- [ ] Can select date, spell, shed
- [ ] Machine buttons appear
- [ ] Opening meter auto-loads
- [ ] Unit and Eff calculate correctly
- [ ] Save works and summary updates

---

## 🔍 TROUBLESHOOTING

### Issue: "Module 'src.drawing' not found"
**Solution:** Ensure you have `__init__.py` in both:
- `e:\sjm\attendancesystem\src\__init__.py`
- `e:\sjm\attendancesystem\src\drawing\__init__.py`

### Issue: "Cannot import name 'get_db'"
**Solution:** Create `e:\sjm\attendancesystem\src\database.py` with the `get_db()` function

### Issue: "Table 'tbl_drawing_mst' doesn't exist"
**Solution:** Run the CREATE TABLE SQL statements on your database

### Issue: Android app can't connect
**Solution:** 
1. Check backend is running: `http://13.126.47.172:5051/`
2. Check firewall allows port 5051
3. Verify BASE_URL in RetrofitClient.kt

### Issue: "Foreign key constraint fails"
**Solution:** 
1. Ensure machine exists in `tbl_drawing_mst` before creating entry
2. Check `mc_id` is valid in your POST request

---

## 📝 EFFICIENCY FORMULA

```
Unit = Closing Meter - Opening Meter
Efficiency% = ((Unit / Hours * 8) / Const * 100) rounded to 2 decimals
```

**Default Const Value:** 100.0 (configurable in Android app)

**Example Calculation:**
- Opening: 1000.00
- Closing: 1500.00
- Hours: 8.0
- Const: 100.0

```
Unit = 1500 - 1000 = 500
Eff = ((500 / 8 * 8) / 100 * 100) = 500.00%
```

---

## 📞 QUICK REFERENCE

```
Backend Path:     e:\sjm\attendancesystem
Database:         sjm @ 13.126.47.172
Reference Code:   E:\sjm\MyHrms\src\drawing\
Android App:      E:\sjm\MyHrms\
Server Port:      5051
```

---

**Last Updated:** May 6, 2026  
**Status:** ✅ Android Complete | ⏳ Backend Integration Pending

