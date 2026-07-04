import os
import re

# Search for all Python files in the attendancesystem directory
backend_dir = r"e:\sjm\attendancesystem"

print("="*70)
print("SEARCHING BACKEND DIRECTORY (excluding venv)")
print("="*70)
print(f"Directory: {backend_dir}\n")

# Walk through all subdirectories
for root, dirs, files in os.walk(backend_dir):
    # Skip venv directory
    if 'venv' in root or '__pycache__' in root:
        continue
    
    for file in files:
        if file.endswith('.py'):
            filepath = os.path.join(root, file)
            relative_path = filepath.replace(backend_dir + "\\", "")
            
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check for dashboard-stats endpoint
                if 'dashboard-stats' in content or 'dashboard_stats' in content:
                    print(f"\n{'='*70}")
                    print(f"✓ FOUND dashboard-stats in: {relative_path}")
                    print(f"File size: {len(content)} characters")
                    print(f"{'='*70}")
                    
                    # Check for department_present
                    if 'department_present' in content:
                        print("  ✓ HAS department_present")
                    else:
                        print("  ✗ MISSING department_present - THIS NEEDS TO BE ADDED!")
                    
                    # Show snippet around dashboard-stats
                    idx = content.find('dashboard')
                    if idx > 0:
                        snippet_start = max(0, idx-100)
                        snippet_end = min(len(content), idx+500)
                        snippet = content[snippet_start:snippet_end]
                        print(f"\nCode snippet:")
                        print("-" * 70)
                        print(snippet)
                        print("-" * 70)
                        
            except Exception as e:
                pass

print("\n" + "="*70)
print("ALL PYTHON FILES (excluding venv):")
print("="*70)
file_count = 0
for root, dirs, files in os.walk(backend_dir):
    # Skip venv directory
    if 'venv' in root or '__pycache__' in root:
        continue
    
    for file in files:
        if file.endswith('.py'):
            filepath = os.path.join(root, file)
            relative_path = filepath.replace(backend_dir + "\\", "")
            size = os.path.getsize(filepath)
            print(f"  {relative_path} ({size} bytes)")
            file_count += 1

print(f"\nTotal: {file_count} Python files found")

