"""
Test Script - Verify daily_ebmc_attendance Insert
Run this to test if backend properly inserts machine data
"""

import requests
import json

# Configuration
BASE_URL = "http://192.168.0.223:5051"

def test_manual_attendance_with_machines():

    """Test POST /mark-attendance with machine_ids"""


    print("=" * 60)
    print("TEST: Manual Attendance with Machine IDs")
    print("=" * 60)
    
    payload = {
        "emp_code": "13177",
        "status": "Manual",
        "att_type": "R",
        "department_id": 1,
        "shift_id": 5,
        "designation_id": 199,
        "attendance_date": "2026-04-24",
        "shift_hours": 8.0,
        "working_hours": 8.0,
        "idle_hours": 0.0,
        "machine_ids": [1344, 1345, 1346],  # Test with 3 machines
        "branch_id": 29
    }
    
    print("\n📤 Sending Request:")
    print(f"URL: {BASE_URL}/mark-attendance")
    print(f"Payload:\n{json.dumps(payload, indent=2)}")
    
    try:
        response = requests.post(
            f"{BASE_URL}/mark-attendance",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        
        print(f"\n📥 Response:")
        print(f"Status Code: {response.status_code}")
        print(f"Response Body:\n{json.dumps(response.json(), indent=2)}")
        
        if response.status_code == 200:
            print("\n✅ SUCCESS: Attendance marked successfully")
            print("\n📋 Next Steps:")
            print("1. Check daily_attendance table for new record")
            print("2. Get the daily_atten_id from the new record")
            print("3. Check daily_ebmc_attendance for 3 machine records")
            print("\nSQL Commands:")
            print("   SELECT * FROM daily_attendance ORDER BY daily_atten_id DESC LIMIT 1;")
            print("   SELECT * FROM daily_ebmc_attendance WHERE daily_atten_id = <last_id>;")
        else:
            print(f"\n❌ FAILED: {response.json().get('message', 'Unknown error')}")
            
    except requests.exceptions.ConnectionError:
        print("\n❌ ERROR: Cannot connect to backend")
        print("   Make sure Flask server is running: python app.py")
    except Exception as e:
        print(f"\n❌ ERROR: {str(e)}")

def verify_code_exists():
    """Verify the code exists in app.py"""
    
    print("\n" + "=" * 60)
    print("VERIFICATION: Check if code exists in app.py")
    print("=" * 60)
    
    with open("app.py", "r", encoding="utf-8") as f:
        content = f.read()
        
    # Check for both occurrences
    count = content.count("INSERT INTO daily_ebmc_attendance")
    
    print(f"\n✅ Found {count} occurrences of 'INSERT INTO daily_ebmc_attendance'")
    
    if count >= 2:
        print("✅ Code is present in both endpoints (Face Recognition & Manual)")
        
        # Find line numbers
        lines = content.split('\n')
        line_numbers = []
        for i, line in enumerate(lines, 1):
            if "INSERT INTO daily_ebmc_attendance" in line:
                line_numbers.append(i)
        
        print(f"✅ Line numbers: {line_numbers}")
        print("\nEndpoint locations:")
        print(f"   1. Face Recognition (/attendance): Line ~{line_numbers[0]}")
        print(f"   2. Manual Attendance (/mark-attendance): Line ~{line_numbers[1]}")
    else:
        print("❌ Code is missing or incomplete!")

if __name__ == "__main__":
    print("\n🔍 BACKEND VERIFICATION SCRIPT")
    print("=" * 60)
    
    # Step 1: Verify code exists
    verify_code_exists()
    
    # Step 2: Test API

    input("\n\n⏸️  Press ENTER to test the API (make sure server is running)...")
    test_manual_attendance_with_machines()
    
    print("\n" + "=" * 60)
    print("✅ VERIFICATION COMPLETE")
    print("=" * 60)

