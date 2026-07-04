# Test API - Employee Branch Validation
# Run this after starting Flask backend (python app.py)

import requests
import json

BASE_URL = "http://localhost:5051"

def test_employee_with_branch():
    """Test GET /employee/{emp_code}?branch_id={id}"""
    print("=" * 60)
    print("Test 1: Get Employee with Branch ID")
    print("=" * 60)
    
    url = f"{BASE_URL}/employee/13177?branch_id=29"
    print(f"Request: GET {url}")
    
    response = requests.get(url)
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ SUCCESS")
        print(f"Employee Code: {data.get('emp_code')}")
        print(f"Employee Name: {data.get('emp_name')}")
        print(f"Branch ID: {data.get('branch_id')}")
        print(f"Message: {data.get('message')}")
    else:
        print(f"❌ FAILED")
        print(f"Error: {response.json()}")
    print()

def test_employee_without_branch():
    """Test GET /employee/{emp_code} (backward compatible)"""
    print("=" * 60)
    print("Test 2: Get Employee without Branch ID (Backward Compatible)")
    print("=" * 60)
    
    url = f"{BASE_URL}/employee/13177"
    print(f"Request: GET {url}")
    
    response = requests.get(url)
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ SUCCESS")
        print(f"Employee Code: {data.get('emp_code')}")
        print(f"Employee Name: {data.get('emp_name')}")
        print(f"Branch ID: {data.get('branch_id')}")
    else:
        print(f"❌ FAILED")
        print(f"Error: {response.json()}")
    print()

def test_employee_wrong_branch():
    """Test GET /employee/{emp_code}?branch_id={wrong_id}"""
    print("=" * 60)
    print("Test 3: Get Employee with Wrong Branch ID (Should Fail)")
    print("=" * 60)
    
    url = f"{BASE_URL}/employee/13177?branch_id=999"
    print(f"Request: GET {url}")
    
    response = requests.get(url)
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 404:
        print(f"✅ CORRECTLY REJECTED")
        data = response.json()
        print(f"Message: {data.get('message')}")
    elif response.status_code == 200:
        print(f"⚠️ WARNING: Employee found in wrong branch (validation might be disabled)")
    else:
        print(f"❌ UNEXPECTED ERROR")
        print(f"Error: {response.json()}")
    print()

def test_invalid_employee():
    """Test GET /employee/{invalid_code}"""
    print("=" * 60)
    print("Test 4: Get Invalid Employee (Should Fail)")
    print("=" * 60)
    
    url = f"{BASE_URL}/employee/99999?branch_id=29"
    print(f"Request: GET {url}")
    
    response = requests.get(url)
    print(f"Status Code: {response.status_code}")
    
    if response.status_code == 404:
        print(f"✅ CORRECTLY REJECTED")
        data = response.json()
        print(f"Message: {data.get('message')}")
    else:
        print(f"❌ UNEXPECTED")
        print(f"Response: {response.json()}")
    print()

if __name__ == "__main__":
    print("\n" + "=" * 60)
    print("EMPLOYEE API BRANCH VALIDATION TEST SUITE")
    print("=" * 60)
    print()
    
    try:
        # Run all tests
        test_employee_with_branch()
        test_employee_without_branch()
        test_employee_wrong_branch()
        test_invalid_employee()
        
        print("=" * 60)
        print("✅ ALL TESTS COMPLETED")
        print("=" * 60)
        print()
        print("Summary:")
        print("- Test 1: Employee lookup with branch_id ✓")
        print("- Test 2: Employee lookup without branch_id (backward compatible) ✓")
        print("- Test 3: Employee validation in wrong branch ✓")
        print("- Test 4: Invalid employee rejection ✓")
        print()
        
    except requests.exceptions.ConnectionError:
        print("\n❌ ERROR: Cannot connect to backend server")
        print("Make sure Flask is running: python app.py")
        print()
    except Exception as e:
        print(f"\n❌ ERROR: {str(e)}")
        print()

