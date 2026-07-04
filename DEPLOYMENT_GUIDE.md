# MyHrms Attendance System - Deployment Guide

## Date: April 23, 2026

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Backend Setup](#backend-setup)
3. [Database Configuration](#database-configuration)
4. [Android App Deployment](#android-app-deployment)
5. [Network Configuration](#network-configuration)
6. [Testing & Verification](#testing--verification)
7. [Troubleshooting](#troubleshooting)
8. [Production Checklist](#production-checklist)

---

## Prerequisites

### System Requirements

#### Backend Server
- **OS:** Windows/Linux/macOS
- **Python:** 3.7 or higher
- **MySQL:** 5.7 or higher
- **RAM:** Minimum 2GB, Recommended 4GB
- **Storage:** Minimum 500MB free space
- **Network:** LAN or WiFi connectivity

#### Android Devices
- **OS:** Android 7.0 (API 24) or higher
- **RAM:** Minimum 2GB
- **Storage:** Minimum 100MB free space
- **Camera:** Required for face recognition
- **Network:** WiFi connectivity

### Required Software

#### For Backend
```powershell
# Python 3.x
python --version

# pip (Python package manager)
pip --version

# MySQL Server
mysql --version
```

#### For Android Development (Optional)
```powershell
# Android SDK & Gradle
.\gradlew --version
```

---

## Backend Setup

### Step 1: Install Python Dependencies

Navigate to project directory:
```powershell
cd E:\sjm\MyHrms
```

Create virtual environment (recommended):
```powershell
python -m venv venv
.\venv\Scripts\Activate
```

Install required packages:
```powershell
pip install flask
pip install mysql-connector-python
pip install face_recognition
pip install numpy
pip install pillow
```

Or use requirements file if available:
```powershell
pip install -r requirements.txt
```

### Step 2: Verify Installation

Check all packages installed:
```powershell
pip list
```

Expected packages:
- flask
- mysql-connector-python
- face_recognition (optional for face recognition)
- numpy
- pillow

---

## Database Configuration

### Step 1: MySQL Setup

Start MySQL service:
```powershell
# Windows
net start MySQL

# Or via Services GUI
services.msc
```

### Step 2: Create Database

Login to MySQL:
```powershell
mysql -u root -p
```

Create database:
```sql
CREATE DATABASE IF NOT EXISTS sls CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sls;
```

### Step 3: Import Database Schema

If you have a backup file:
```powershell
mysql -u root -p sls < slsbackup.sql
```

Or manually create tables (see DATABASE_SCHEMA.md for complete schema).

### Step 4: Verify Required Tables

```sql
USE sls;

-- Check essential tables exist
SHOW TABLES;

-- Should include:
-- - employees
-- - daily_attendance
-- - spell_mst (shifts)
-- - sub_dept_mst (departments)
-- - designation_mst
-- - branch_mst
-- - company_mst
```

### Step 5: Configure Database Connection

Edit `app.py` database connection settings:
```python
db_connection = mysql.connector.connect(
    host='localhost',      # Change if MySQL is on different server
    user='root',           # Your MySQL username
    password='your_password',  # Your MySQL password
    database='sls',        # Database name
    port=3306              # MySQL port (default 3306)
)
```

**Security Note:** For production, use environment variables:
```python
import os
db_connection = mysql.connector.connect(
    host=os.getenv('DB_HOST', 'localhost'),
    user=os.getenv('DB_USER', 'root'),
    password=os.getenv('DB_PASSWORD'),
    database=os.getenv('DB_NAME', 'sls')
)
```

---

## Backend Server Deployment

### Step 1: Configure Server Settings

Edit `app.py` server configuration:
```python
if __name__ == '__main__':
    app.run(
        host='0.0.0.0',  # Listen on all network interfaces
        port=5051,       # Port number
        debug=False,     # Set to False for production
        threaded=True    # Enable multi-threading
    )
```

### Step 2: Start Server

Development mode:
```powershell
cd E:\sjm\MyHrms
python app.py
```

Expected output:
```
✅ face_recognition loaded
MySQL connection successful!
 * Serving Flask app 'app'
 * Running on http://0.0.0.0:5051
Press CTRL+C to quit
```

### Step 3: Test Server

Open browser or use curl:
```powershell
# Test if server is running
curl http://localhost:5051/health

# Or in browser
http://localhost:5051
```

### Step 4: Production Deployment (Optional)

For production, use a WSGI server like Gunicorn:

Install Gunicorn:
```powershell
pip install gunicorn
```

Run with Gunicorn:
```bash
gunicorn -w 4 -b 0.0.0.0:5051 app:app
```

Or use Windows Service (for Windows servers):
```powershell
# Install NSSM (Non-Sucking Service Manager)
# Create Windows service
nssm install MyHrmsBackend "C:\Python\python.exe" "E:\sjm\MyHrms\app.py"
nssm start MyHrmsBackend
```

---

## Android App Deployment

### Step 1: Configure API Base URL

Edit API configuration file:
**File:** `app/src/main/java/com/example/myhrms/api/ApiConfig.kt`

```kotlin
object ApiConfig {
    // Development
    private const val DEV_BASE_URL = "http://192.168.0.223:5051/"
    
    // Production
    private const val PROD_BASE_URL = "http://your-server-ip:5051/"
    
    fun getBaseUrl(context: Context): String {
        // Change to PROD_BASE_URL for production
        return DEV_BASE_URL
    }
}
```

**Important:** Replace `192.168.0.223` with your server's actual IP address.

### Step 2: Find Server IP Address

On server machine:
```powershell
# Windows
ipconfig | Select-String "IPv4"

# Result example: IPv4 Address: 192.168.1.100
```

### Step 3: Build APK

Clean and build:
```powershell
cd E:\sjm\MyHrms
.\gradlew clean
.\gradlew assembleDebug
```

For release build (production):
```powershell
.\gradlew assembleRelease
```

### Step 4: Locate APK

APK location:
```
Debug: E:\sjm\MyHrms\app\build\outputs\apk\debug\app-debug.apk
Release: E:\sjm\MyHrms\app\build\outputs\apk\release\app-release.apk
```

### Step 5: Install on Devices

#### Method 1: USB Installation
```powershell
# Connect device via USB
# Enable USB debugging on device
adb devices
adb install app\build\outputs\apk\debug\app-debug.apk
```

#### Method 2: File Transfer
1. Copy APK to device via USB, email, or cloud
2. Open APK on device
3. Enable "Install from Unknown Sources" if prompted
4. Tap Install

#### Method 3: Wi-Fi Installation (Multiple Devices)
1. Set up file sharing server
2. Share APK via network
3. Devices download and install

### Step 6: Initial App Configuration

On each device:
1. Launch MyHrms app
2. Login with credentials
3. Navigate to Settings (if available)
4. Verify API connection
5. Test attendance marking

---

## Network Configuration

### Server Firewall Configuration

Allow inbound connections on port 5051:

#### Windows Firewall
```powershell
# Open port 5051
New-NetFirewallRule -DisplayName "MyHrms Backend" -Direction Inbound -Protocol TCP -LocalPort 5051 -Action Allow
```

#### Linux (ufw)
```bash
sudo ufw allow 5051/tcp
sudo ufw reload
```

### Router Configuration (If needed)

If devices are on different networks:
1. Configure port forwarding on router
2. Forward external port to server IP:5051
3. Use external IP in ApiConfig.kt

### Network Testing

From Android device, test connectivity:
```
Settings → WiFi → Connected Network → Advanced → Gateway
Ping server IP or test in browser: http://SERVER_IP:5051
```

---

## Testing & Verification

### Backend Health Check

Test API endpoints:

#### 1. Health Check
```powershell
curl http://localhost:5051/health
```

#### 2. Get Employees
```powershell
curl http://localhost:5051/employees
```

#### 3. Get Departments
```powershell
curl http://localhost:5051/departments
```

#### 4. Get Shifts
```powershell
curl http://localhost:5051/shifts
```

### App Functionality Tests

#### Test 1: Login
- Open app
- Enter credentials
- Verify login successful
- Check dashboard loads

#### Test 2: Face Recognition
- Navigate to Attendance
- Click Camera button
- Take photo
- Verify employee identified
- Fill form and submit
- Verify success message

#### Test 3: Manual Entry
- Navigate to Attendance
- Enter employee code
- Click Check (✓)
- Verify employee found
- Fill form and submit
- Verify success message

#### Test 4: Database Verification
```sql
-- Check recent attendance
SELECT 
    eb_id, 
    attendance_date, 
    attendance_source, 
    attendance_type,
    spell,
    spell_hours,
    working_hours
FROM daily_attendance
WHERE DATE(entry_time) = CURDATE()
ORDER BY entry_time DESC;
```

---

## Troubleshooting

### Common Issues

#### 1. Server Won't Start

**Symptom:** `python app.py` shows errors

**Solutions:**
```powershell
# Check Python installation
python --version

# Check required packages
pip list

# Install missing packages
pip install flask mysql-connector-python

# Check if port 5051 is already in use
netstat -ano | findstr :5051

# Kill process using port (if needed)
taskkill /F /PID <PID>
```

#### 2. Database Connection Failed

**Symptom:** "MySQL connection failed"

**Solutions:**
```sql
-- Verify MySQL is running
services.msc

-- Test MySQL connection
mysql -u root -p

-- Check user privileges
SHOW GRANTS FOR 'root'@'localhost';

-- Grant permissions if needed
GRANT ALL PRIVILEGES ON sls.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. App Can't Connect to Server

**Symptom:** "Network error" in app

**Solutions:**
1. Verify server is running
2. Check IP address in ApiConfig.kt
3. Ensure device and server on same WiFi
4. Check firewall settings
5. Ping server from device

```powershell
# On server, get IP
ipconfig

# On device, check connectivity
# Use network testing app or browser
```

#### 4. Face Recognition Not Working

**Symptom:** "Face not recognized"

**Solutions:**
- Verify face_recognition library installed: `pip install face_recognition`
- Check employee has photo in database
- Ensure good lighting when capturing
- Face must be clearly visible
- Try re-registering employee photo

#### 5. Camera Permission Denied

**Symptom:** Camera doesn't open

**Solutions:**
- Go to device Settings → Apps → MyHrms → Permissions
- Enable Camera permission
- Restart app

#### 6. Build Errors

**Symptom:** `gradlew assembleDebug` fails

**Solutions:**
```powershell
# Clean build
.\gradlew clean

# Rebuild
.\gradlew assembleDebug --stacktrace

# Check errors in output
# Fix dependency issues in build.gradle.kts
```

---

## Production Checklist

### Security

- [ ] Change debug mode to False in app.py
- [ ] Use HTTPS instead of HTTP
- [ ] Implement JWT authentication
- [ ] Use environment variables for sensitive data
- [ ] Enable SQL injection protection
- [ ] Add rate limiting
- [ ] Implement password hashing
- [ ] Remove debug logs
- [ ] Configure CORS properly

### Performance

- [ ] Enable database connection pooling
- [ ] Add database indexes
- [ ] Implement caching
- [ ] Compress images before upload
- [ ] Enable gzip compression
- [ ] Monitor memory usage

### Reliability

- [ ] Set up automated backups
- [ ] Implement error logging
- [ ] Add health monitoring
- [ ] Set up crash reporting
- [ ] Configure automatic restarts
- [ ] Test failover scenarios

### Monitoring

- [ ] Set up server monitoring
- [ ] Track API response times
- [ ] Monitor database performance
- [ ] Log all errors
- [ ] Track user activity
- [ ] Set up alerts

### Documentation

- [ ] Update API documentation
- [ ] Create user manual
- [ ] Document admin procedures
- [ ] Maintain change log
- [ ] Update deployment notes

---

## Maintenance Procedures

### Daily Tasks
- Check server logs for errors
- Verify all services running
- Monitor disk space usage

### Weekly Tasks
- Review error logs
- Check database backups
- Verify network connectivity
- Test critical functions

### Monthly Tasks
- Database optimization
- Security updates
- Performance review
- Backup verification
- User feedback review

---

## Backup & Recovery

### Database Backup

Daily backup:
```powershell
# Create backup
mysqldump -u root -p sls > backup_$(Get-Date -Format "yyyyMMdd").sql

# Automate with Task Scheduler
```

### Restore from Backup

```powershell
# Stop server
# Restore database
mysql -u root -p sls < backup_20260423.sql

# Restart server
```

---

## Scaling Considerations

### For More Users

1. **Load Balancing**
   - Deploy multiple backend servers
   - Use nginx or HAProxy for load balancing

2. **Database Optimization**
   - Add read replicas
   - Implement connection pooling
   - Add database indexes

3. **Caching**
   - Implement Redis for session management
   - Cache frequently accessed data

---

## Support & Contacts

### Technical Support
- **Documentation:** See README.md and related docs
- **Logs Location:** Check console output and log files
- **Issue Tracking:** Use issue tracker or ticketing system

### Emergency Contacts
- **Database Admin:** [Contact Info]
- **Network Admin:** [Contact Info]
- **Developer:** [Contact Info]

---

## Quick Reference Commands

### Server Management
```powershell
# Start server
cd E:\sjm\MyHrms
python app.py

# Stop server
Ctrl+C

# Check if running
Test-NetConnection localhost -Port 5051
```

### Database Management
```powershell
# Login
mysql -u root -p

# Backup
mysqldump -u root -p sls > backup.sql

# Restore
mysql -u root -p sls < backup.sql
```

### App Management
```powershell
# Build
.\gradlew assembleDebug

# Install
adb install app-debug.apk

# View logs
adb logcat | Select-String "ATTENDANCE"
```

---

## Appendix

### A. Environment Variables Template

Create `.env` file:
```
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=sls
DB_PORT=3306

API_PORT=5051
API_DEBUG=False

SECRET_KEY=your_secret_key_here
```

### B. Sample Configuration Files

See configuration templates in `/docs/samples/`

### C. Database Schema

See complete schema in `DATABASE_SCHEMA_UPDATES.md`

### D. API Documentation

See API endpoints and examples in `ATTENDANCE_SAVE_IMPLEMENTATION.md`

---

**Document Version:** 1.0  
**Last Updated:** April 23, 2026  
**Status:** ✅ Complete  
**Next Review:** May 1, 2026

