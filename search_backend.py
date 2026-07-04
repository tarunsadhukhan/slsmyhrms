import sys
import re

# Read the backend file and search for dashboard-related endpoints
backend_file = r"e:\sjm\attendancesystem\app.py"

try:
    with open(backend_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    print("="*70)
    print("SEARCHING FOR DASHBOARD ENDPOINTS IN BACKEND")
    print("="*70)
    print(f"File size: {len(content)} characters\n")
    
    # Search for any route decorators
    routes = re.findall(r"@\w+\.route\(['\"]([^'\"]+)['\"]", content)
    print(f"Found {len(routes)} routes:")
    for i, route in enumerate(routes, 1):
        print(f"  {i}. {route}")
    
    # Search for dashboard
    if "dashboard" in content.lower():
        print("\n" + "="*70)
        print("FOUND 'dashboard' in file")
        print("="*70)
        
        # Find all occurrences
        idx = 0
        count = 0
        while True:
            idx = content.lower().find("dashboard", idx)
            if idx == -1:
                break
            count += 1
            # Show context
            start = max(0, idx - 50)
            end = min(len(content), idx + 100)
            context = content[start:end].replace('\n', ' ')
            print(f"\n{count}. ...{context}...")
            idx += 1
            if count >= 5:  # Limit to first 5
                break
    else:
        print("\n'dashboard' NOT found in file")
    
    # Search for department_present
    if "department_present" in content:
        print("\n" + "="*70)
        print("FOUND 'department_present' in file")
        print("="*70)
    else:
        print("\n" + "="*70)
        print("'department_present' NOT FOUND - This is the problem!")
        print("="*70)
        
except FileNotFoundError:
    print(f"ERROR: Backend file not found at: {backend_file}")
except Exception as e:
    print(f"ERROR: {e}")
    import traceback
    traceback.print_exc()

