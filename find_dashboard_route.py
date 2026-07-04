import os
import re

# Search for all Python files in the attendancesystem directory
backend_dir = r"e:\sjm\attendancesystem"

print("="*70)
print("SEARCHING ENTIRE BACKEND DIRECTORY")
print("="*70)
print(f"Directory: {backend_dir}\n")

# Walk through all subdirectories
for root, dirs, files in os.walk(backend_dir):
    for file in files:
        if file.endswith('.py'):
            filepath = os.path.join(root, file)
            relative_path = filepath.replace(backend_dir, "")
            
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check for dashboard-stats endpoint
                if 'dashboard-stats' in content or 'dashboard_stats' in content:
                    print(f"\n{'='*70}")
                    print(f"FOUND dashboard-stats in: {relative_path}")
                    print(f"File size: {len(content)} characters")
                    print(f"{'='*70}")
                    
                    # Find the function/route definition
                    routes = re.findall(r"@[\w.]+route\(['\"]([^'\"]+)['\"].*?\ndef\s+(\w+)", content, re.DOTALL)
                    if routes:
                        print(f"Routes found: {routes}")
                    
                    # Check for department_present
                    if 'department_present' in content:
                        print("✓ HAS department_present")
                    else:
                        print("✗ MISSING department_present")
                    
                    # Show snippet around dashboard-stats
                    idx = content.find('dashboard')
                    if idx > 0:
                        snippet = content[max(0, idx-200):idx+1000]
                        print(f"\nCode snippet:\n{snippet[:500]}...")
                        
            except Exception as e:
                print(f"Error reading {filepath}: {e}")

print("\n" + "="*70)
print("LISTING ALL .PY FILES:")
print("="*70)
for root, dirs, files in os.walk(backend_dir):
    for file in files:
        if file.endswith('.py'):
            filepath = os.path.join(root, file)
            relative_path = filepath.replace(backend_dir + "\\", "")
            size = os.path.getsize(filepath)
            print(f"  {relative_path} ({size} bytes)")

