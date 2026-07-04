# ═══════════════════════════════════════════════════════════════════
# BACKEND FIX FOR department_present EMPTY ISSUE
# ═══════════════════════════════════════════════════════════════════
# 
# PROBLEM: department_present is showing empty even though department_wise has data
# CAUSE: The query joins on hrms_ed_official_details which doesn't match worked_department_id
# SOLUTION: Simplify query to directly count from daily_attendance using worked_department_id
#
# FILE TO UPDATE: e:\sjm\attendancesystem\app.py
# ENDPOINT: /dashboard-stats
#
# ═══════════════════════════════════════════════════════════════════

# FIND THIS CODE BLOCK (around line 1336-1360 in dashboard-stats endpoint):

"""
        # Department-wise statistics (filtered by branch)
        # Using better query to get department data with present count in one go
        dept_stats_query = \"\"\"
            SELECT 
                sdm.sub_dept_id AS department_id,
                sdm.sub_dept_desc AS department_name,
                COUNT(DISTINCT o.emp_id) AS total_employees,
                COALESCE(SUM(CASE WHEN da.attendance_date = %s THEN 1 ELSE 0 END), 0) AS present
            FROM sub_dept_mst sdm
            LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id
            LEFT JOIN hrms_ed_official_details o ON sdm.sub_dept_id = o.sub_dept_id
            LEFT JOIN daily_attendance da ON da.eb_id = o.eb_id 
                AND da.worked_department_id = sdm.sub_dept_id 
                AND da.attendance_date = %s
        \"\"\"
        dept_stats_params = [stat_date, stat_date]
"""

# REPLACE WITH THIS CODE:

FIXED_CODE = """
        # Department-wise statistics (filtered by branch)
        # Query to get department data with present count based on worked_department_id
        dept_stats_query = \"\"\"
            SELECT 
                sdm.sub_dept_id AS department_id,
                sdm.sub_dept_desc AS department_name,
                0 AS total_employees,
                COUNT(da.eb_id) AS present
            FROM sub_dept_mst sdm
            LEFT JOIN dept_mst dm ON dm.dept_id = sdm.dept_id
            LEFT JOIN daily_attendance da 
                ON da.worked_department_id = sdm.sub_dept_id 
                AND da.attendance_date = %s
                AND da.is_active = 1
        \"\"\"
        dept_stats_params = [stat_date]
"""

# ADD LOGGING AFTER THE QUERY EXECUTION (around line 1368):

LOGGING_CODE = """
        dept_stats_query += " GROUP BY sdm.sub_dept_id, sdm.sub_dept_desc ORDER BY sdm.sub_dept_desc"

        print(f"Executing dept_stats query with params: {dept_stats_params}")
        cursor.execute(dept_stats_query, tuple(dept_stats_params))
        dept_stats = cursor.fetchall()
        print(f"Department-wise query results: {dept_stats}")
"""

# ADD LOGGING BEFORE RESPONSE (around line 1395):

FINAL_LOGGING = """
        print(f"department_wise: {len(department_wise)} items - {department_wise}")
        print(f"department_present: {len(department_present)} items - {department_present}")
        print(f"department_master: {len(department_master)} items - {department_master}")

        cursor.close()
        db.close()
"""

print("=" * 70)
print("COPY THIS FIX TO: e:\\sjm\\attendancesystem\\app.py")
print("=" * 70)
print("\n1. Open e:\\sjm\\attendancesystem\\app.py in a text editor")
print("2. Find the /dashboard-stats endpoint (search for @app.route('/dashboard-stats')")
print("3. Replace the dept_stats_query section with FIXED_CODE above")
print("4. Add the logging statements")
print("5. Save and restart the server")
print("\n" + "=" * 70)

