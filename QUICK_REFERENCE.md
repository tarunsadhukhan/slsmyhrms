# MyHrms Quick Reference Guide

## 🚀 Quick Start

### Start Backend Server
```powershell
cd E:\sjm\MyHrms
python app.py
```
**Expected:** Server running on http://0.0.0.0:5051

### Build Android App
```powershell
cd E:\sjm\MyHrms
.\gradlew assembleDebug
```
**Output:** app-debug.apk in `app\build\outputs\apk\debug\`

### Install APK
```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## 📱 API Configuration

### Update Base URL
**File:** `app/src/main/java/com/example/myhrms/api/ApiConfig.kt`

```kotlin
private const val BASE_URL = "http://192.168.0.223:5051/"
```
Change IP to your server's IP address.

---

## 🔧 Key Commands

### Clean Build
```powershell
.\gradlew clean
.\gradlew assembleDebug
```

### View Logs
```powershell
adb logcat | Select-String "ATTENDANCE_DEBUG"
```

### Database Query
```sql
SELECT * FROM daily_attendance 
WHERE attendance_date = CURDATE() 
ORDER BY entry_time DESC;
```

---

## 📋 API Endpoints

### Face Recognition
```http
POST http://192.168.0.223:5051/attendance
Content-Type: application/json

{
  "image": "base64_encoded_image",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 1,
  "designation_id": 1,
  "attendance_date": "2026-04-23",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

### Manual Entry
```http
POST http://192.168.0.223:5051/mark-attendance
Content-Type: application/json

{
  "emp_code": "13177",
  "status": "Manual",
  "att_type": "R",
  "department_id": 1,
  "shift_id": 1,
  "designation_id": 1,
  "attendance_date": "2026-04-23",
  "shift_hours": 8.0,
  "working_hours": 8.0,
  "idle_hours": 0.0
}
```

### Check Face Only
```http
POST http://192.168.0.223:5051/check-face
Content-Type: application/json

{
  "image": "base64_encoded_image"
}
```

---

## 📊 Database Tables

### daily_attendance
```sql
CREATE TABLE daily_attendance (
  id INT AUTO_INCREMENT PRIMARY KEY,
  attendance_date DATE,
  attendance_mark CHAR(1) DEFAULT 'P',
  attendance_source VARCHAR(10),
  attendance_type CHAR(1) DEFAULT 'R',
  branch_id INT,
  eb_id INT,
  entry_time DATETIME,
  idle_hours DECIMAL(5,2),
  is_active TINYINT DEFAULT 1,
  spell VARCHAR(50),
  spell_hours DECIMAL(5,2),
  worked_department_id INT,
  worked_designation_id INT,
  working_hours DECIMAL(5,2),
  update_date_time DATETIME
);
```

---

## 🎨 UI Components

### Attendance Form Fields
- **Date Picker:** tvDate
- **Department Spinner:** spinnerDepartment
- **Shift Spinner:** spinnerShift
- **Shift Hours:** etShiftHours
- **Occupation Spinner:** spinnerOccupation
- **Working Hours:** etWorkingHours
- **Idle Hours:** etIdleHours
- **Employee Code:** etEmployeeCode
- **Tabs:** tabRegular, tabOT, tabCash
- **Buttons:** btnCamera, btnCheck, btnSearch, btnSubmit

---

## 🔍 Troubleshooting

### Camera Not Working
```kotlin
// Check FileProvider authority in AndroidManifest.xml
android:authorities="${applicationId}.fileprovider"

// In Activity:
photoUri = FileProvider.getUriForFile(
    this, 
    "${packageName}.fileprovider", 
    photoFile!!
)
```

### Network Errors
1. Check server is running
2. Verify IP address in ApiConfig.kt
3. Ensure device and server on same network
4. Check firewall settings

### Face Not Recognized
1. Verify face_recognition library installed
2. Check employee has registered photo
3. Ensure good lighting
4. Face must be clearly visible

---

## 📁 Important Files

| File | Purpose |
|------|---------|
| `AttendanceActivity.kt` | Main attendance screen |
| `app.py` | Backend Flask server |
| `activity_attendance.xml` | Attendance UI layout |
| `ApiService.kt` | API definitions |
| `RetrofitClient.kt` | HTTP client setup |

---

## 🧪 Testing

### Test Face Recognition
1. Start server
2. Open app → Attendance
3. Click camera
4. Take photo
5. Verify employee identified
6. Submit attendance

### Test Manual Entry
1. Open app → Attendance
2. Enter employee code
3. Click check (✓)
4. Verify employee found
5. Fill form
6. Submit attendance

---

## 📚 Documentation Files

| Document | Description |
|----------|-------------|
| `PROJECT_STATUS.md` | Complete project overview |
| `ATTENDANCE_SAVE_IMPLEMENTATION.md` | Technical implementation details |
| `CODE_CLEANUP_REPORT.md` | Code quality improvements |
| `COMPLETE_TESTING_GUIDE.md` | Testing procedures |
| `CAMERA_FIX_REPORT.md` | Camera issue resolution |

---

## ⚙️ Configuration

### Server Settings
```python
# app.py
app.run(
    host='0.0.0.0',  # Listen on all interfaces
    port=5051,        # Port number
    debug=True        # Debug mode
)
```

### Database Connection
```python
db_connection = mysql.connector.connect(
    host='localhost',
    user='root',
    password='your_password',
    database='sls'
)
```

---

## 🔐 Security Notes

### Production Checklist
- [ ] Change debug mode to False
- [ ] Use HTTPS instead of HTTP
- [ ] Implement JWT authentication
- [ ] Enable SQL injection protection
- [ ] Add rate limiting
- [ ] Encrypt sensitive data
- [ ] Remove debug logs
- [ ] Implement password hashing

---

## 📞 Support

### Get Logs
```powershell
# Android logs
adb logcat -v time > logcat.txt

# Backend logs
python app.py > server.log 2>&1
```

### Common Issues
| Issue | Solution |
|-------|----------|
| Build fails | Run `.\gradlew clean` |
| Server won't start | Check port 5051 not in use |
| Database error | Verify MySQL running |
| Camera permission | Grant in app settings |

---

## 📊 Performance Tips

### Optimize Images
- Compress photos before upload
- Use JPEG format
- Recommended size: 640x480

### Database Optimization
```sql
-- Add indexes
CREATE INDEX idx_eb_date ON daily_attendance(eb_id, attendance_date);
CREATE INDEX idx_entry_time ON daily_attendance(entry_time);
```

---

## 🎯 Best Practices

### Code Style
- Use meaningful variable names
- Add comments for complex logic
- Follow Kotlin coding conventions
- Keep functions small and focused

### Git Workflow
```bash
git add .
git commit -m "feat: description"
git push origin main
```

### Testing
- Test on different Android versions
- Test on various screen sizes
- Test with slow network
- Test error scenarios

---

## 🔄 Update Procedure

### Update Backend
```powershell
cd E:\sjm\MyHrms
git pull
python app.py
```

### Update Mobile App
```powershell
cd E:\sjm\MyHrms
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 📈 Monitoring

### Check Server Status
```powershell
curl http://192.168.0.223:5051/health
```

### Monitor Database
```sql
-- Recent attendance
SELECT COUNT(*) FROM daily_attendance 
WHERE DATE(entry_time) = CURDATE();

-- Failed attempts
SELECT * FROM error_log 
WHERE DATE(timestamp) = CURDATE();
```

---

## 🎓 Learning Resources

### Kotlin
- Official docs: https://kotlinlang.org/docs/
- Android developers: https://developer.android.com/kotlin

### Flask
- Official docs: https://flask.palletsprojects.com/
- RESTful API guide

### Face Recognition
- face_recognition library: https://github.com/ageitgey/face_recognition
- OpenCV documentation

---

## ✅ Pre-Deployment Checklist

- [ ] All tests passing
- [ ] Documentation updated
- [ ] API endpoints tested
- [ ] Database migrations applied
- [ ] Security review completed
- [ ] Performance tested
- [ ] Backup created
- [ ] Rollback plan ready

---

**Quick Reference Version:** 1.0  
**Last Updated:** April 23, 2026  
**Status:** ✅ Current

