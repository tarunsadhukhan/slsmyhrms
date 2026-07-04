import sys

# Read the backend file and show the dashboard-stats section
backend_file = r"e:\sjm\attendancesystem\app.py"

try:
    with open(backend_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find the dashboard-stats endpoint
    start_marker = "@app.route('/dashboard-stats'"
    if start_marker in content:
        start_idx = content.find(start_marker)
        # Get 2000 characters after the marker
        section = content[start_idx:start_idx+3000]
        
        print("="*70)
        print("FOUND /dashboard-stats ENDPOINT")
        print("="*70)
        print(section[:1500])
        print("\n... (truncated)")
        print("="*70)
        
        # Find the dept_stats_query section specifically
        if "dept_stats_query" in section:
            query_idx = section.find("dept_stats_query")
            query_section = section[query_idx:query_idx+800]
            print("\nQUERY SECTION:")
            print("="*70)
            print(query_section)
            print("="*70)
    else:
        print("ERROR: Could not find /dashboard-stats endpoint")
        
except FileNotFoundError:
    print(f"ERROR: Backend file not found at: {backend_file}")
except Exception as e:
    print(f"ERROR: {e}")

