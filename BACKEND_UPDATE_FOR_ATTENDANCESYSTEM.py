"""
BACKEND UPDATE FOR ATTENDANCE REPORT API
Apply these changes to: e:\sjm\attendancesystem\app.py

This updates the /attendance-report endpoint to:
1. Support BOTH single date AND date range queries
2. Accept emp_name and shift_name parameters for single date queries
3. Return eb_id and machine_nos in response
4. Maintain backwards compatibility for date range queries
"""

# ══════════════════════════════════════════════════════════════════
# STEP 1: Find the /attendance-report endpoint (around line 1414)
# ══════════════════════════════════════════════════════════════════

# REPLACE THIS:
OLD_CODE_DOCSTRING = """
@app.route('/attendance-report', methods=['GET'])
def attendance_report():
    '''
    Returns attendance records from daily_attendance with filters.
    Query params: from_date, to_date, department_id, emp_code, branch_id
    '''
    try:
        from_date = request.args.get('from_date')
        to_date = request.args.get('to_date')
        department_id = request.args.get('department_id')
        emp_code = request.args.get('emp_code', '').strip()
        branch_id = request.args.get('branch_id', type=int)

        if not from_date or not to_date:
            return jsonify({'status': 'error', 'message': 'from_date and to_date are required'}), 400
"""

# WITH THIS:
NEW_CODE_DOCSTRING = """
@app.route('/attendance-report', methods=['GET'])
def attendance_report():
    '''
    Returns attendance records from daily_attendance with filters.
    Supports TWO query modes:
    1. Single date: ?date=YYYY-MM-DD&emp_code=...&emp_name=...&shift_name=...&branch_id=...
    2. Date range: ?from_date=YYYY-MM-DD&to_date=YYYY-MM-DD&department_id=...&emp_code=...&branch_id=...
    '''
    try:
        # Check which query mode
        attendance_date = request.args.get('date')
        from_date = request.args.get('from_date')
        to_date = request.args.get('to_date')
        
        emp_code = request.args.get('emp_code', '').strip()
        emp_name = request.args.get('emp_name', '').strip()
        shift_name = request.args.get('shift_name', '').strip()
        department_id = request.args.get('department_id')
        branch_id = request.args.get('branch_id', type=int)

        # Validate parameters
        if attendance_date:
            # Single date mode
            query_date_condition = "da.attendance_date = %s"
            date_params = [attendance_date]
        elif from_date and to_date:
            # Date range mode
            query_date_condition = "da.attendance_date BETWEEN %s AND %s"
            date_params = [from_date, to_date]
        else:
            return jsonify({'status': 'error', 'message': 'Either date or from_date/to_date is required'}), 400
"""

# ══════════════════════════════════════════════════════════════════
# STEP 2: Update the SQL query
# ══════════════════════════════════════════════════════════════════

# REPLACE THIS:
OLD_SQL_QUERY = """
        sql = '''
            SELECT da.daily_atten_id AS id,
                   p.emp_code,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   COALESCE(s.sub_dept_desc, '') AS department_name,
                   COALESCE(d.desig, '') AS designation_name,
                   COALESCE(da.spell, '') AS shift_name,
                   da.attendance_date,
                   TIME(da.entry_time) AS attendance_time,
                   da.attendance_source AS status,
                   da.attendance_type AS att_type,
                   COALESCE(da.spell_hours, 0) AS shift_hours,
                   COALESCE(da.working_hours, 0) AS working_hours,
                   COALESCE(da.idle_hours, 0) AS idle_hours
            FROM daily_attendance da
            JOIN hrms_ed_personal_details p ON da.eb_id = p.eb_id
            LEFT JOIN sub_dept_mst s ON da.worked_department_id = s.sub_dept_id
            LEFT JOIN designation_mst d ON da.worked_designation_id = d.designation_id
            WHERE da.attendance_date BETWEEN %s AND %s AND da.is_active = 1
        '''
        params = [from_date, to_date]

        if branch_id:
            sql += ' AND da.branch_id = %s'
            params.append(branch_id)

        if department_id:
            sql += ' AND da.worked_department_id = %s'
            params.append(department_id)

        if emp_code:
            sql += ' AND p.emp_code LIKE %s'
            params.append(f'%{emp_code}%')

        sql += ' ORDER BY da.attendance_date DESC, da.entry_time DESC'
"""

# WITH THIS:
NEW_SQL_QUERY = """
        sql = f'''
            SELECT da.daily_atten_id AS id,
                   p.emp_code,
                   p.eb_id,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   COALESCE(s.sub_dept_desc, '') AS department_name,
                   COALESCE(d.desig, '') AS designation_name,
                   COALESCE(da.spell, '') AS shift_name,
                   da.attendance_date,
                   TIME(da.entry_time) AS attendance_time,
                   da.attendance_source AS status,
                   da.attendance_type AS att_type,
                   COALESCE(da.spell_hours, 0) AS shift_hours,
                   COALESCE(da.working_hours, 0) AS working_hours,
                   COALESCE(da.idle_hours, 0) AS idle_hours
            FROM daily_attendance da
            JOIN hrms_ed_personal_details p ON da.eb_id = p.eb_id
            LEFT JOIN sub_dept_mst s ON da.worked_department_id = s.sub_dept_id
            LEFT JOIN designation_mst d ON da.worked_designation_id = d.designation_id
            WHERE {query_date_condition} AND da.is_active = 1
        '''
        params = date_params

        if branch_id:
            sql += ' AND da.branch_id = %s'
            params.append(branch_id)

        if department_id:
            sql += ' AND da.worked_department_id = %s'
            params.append(department_id)

        if emp_code:
            sql += ' AND p.emp_code LIKE %s'
            params.append(f'%{emp_code}%')

        if emp_name:
            sql += ' AND (p.first_name LIKE %s OR p.middle_name LIKE %s OR p.last_name LIKE %s)'
            params.extend([f'%{emp_name}%', f'%{emp_name}%', f'%{emp_name}%'])

        if shift_name and shift_name != 'All Shifts':
            sql += ' AND da.spell = %s'
            params.append(shift_name)

        sql += ' ORDER BY da.attendance_date DESC, da.entry_time DESC'
"""

# ══════════════════════════════════════════════════════════════════
# STEP 3: Update response building to include machine numbers
# ══════════════════════════════════════════════════════════════════

# REPLACE THIS:
OLD_RESPONSE = """
        data = []
        for row in rows:
            data.append({
                'id': row['id'],
                'emp_code': row['emp_code'],
                'emp_name': (row['emp_name'] or '').strip(),
                'department_name': row['department_name'] or '',
                'designation_name': row['designation_name'] or '',
                'shift_name': row['shift_name'] or '',
                'attendance_date': str(row['attendance_date']),
                'attendance_time': str(row['attendance_time']),
                'status': row['status'] or '',
                'att_type': row['att_type'] or 'R',
                'shift_hours': float(row['shift_hours']),
                'working_hours': float(row['working_hours']),
                'idle_hours': float(row['idle_hours']),
                'photo_att': ''
            })
"""

# WITH THIS:
NEW_RESPONSE = """
        data = []
        for row in rows:
            # Fetch machine numbers for this attendance record
            cursor.execute('''
                SELECT mm.mech_code, mm.machine_name
                FROM daily_ebmc_attendance dea
                JOIN machine_mst mm ON dea.mech_id = mm.machine_id
                WHERE dea.daily_atten_id = %s AND dea.is_active = 1
                ORDER BY mm.mech_code
            ''', (row['id'],))
            machine_rows = cursor.fetchall()
            
            # Create comma-separated list of machine codes
            machine_nos = ', '.join([m['mech_code'] or '' for m in machine_rows if m['mech_code']])
            
            data.append({
                'id': row['id'],
                'emp_code': row['emp_code'],
                'eb_id': row['eb_id'],
                'emp_name': (row['emp_name'] or '').strip(),
                'department_name': row['department_name'] or '',
                'designation_name': row['designation_name'] or '',
                'shift_name': row['shift_name'] or '',
                'attendance_date': str(row['attendance_date']),
                'attendance_time': str(row['attendance_time']),
                'status': row['status'] or '',
                'att_type': row['att_type'] or 'R',
                'shift_hours': float(row['shift_hours']),
                'working_hours': float(row['working_hours']),
                'idle_hours': float(row['idle_hours']),
                'photo_att': '',
                'machine_nos': machine_nos
            })
"""

# ══════════════════════════════════════════════════════════════════
# COMPLETE UPDATED FUNCTION
# ══════════════════════════════════════════════════════════════════

COMPLETE_FUNCTION = """
@app.route('/attendance-report', methods=['GET'])
def attendance_report():
    '''
    Returns attendance records from daily_attendance with filters.
    Supports TWO query modes:
    1. Single date: ?date=YYYY-MM-DD&emp_code=...&emp_name=...&shift_name=...&branch_id=...
    2. Date range: ?from_date=YYYY-MM-DD&to_date=YYYY-MM-DD&department_id=...&emp_code=...&branch_id=...
    '''
    try:
        # Check which query mode
        attendance_date = request.args.get('date')
        from_date = request.args.get('from_date')
        to_date = request.args.get('to_date')
        
        emp_code = request.args.get('emp_code', '').strip()
        emp_name = request.args.get('emp_name', '').strip()
        shift_name = request.args.get('shift_name', '').strip()
        department_id = request.args.get('department_id')
        branch_id = request.args.get('branch_id', type=int)

        # Validate parameters
        if attendance_date:
            # Single date mode
            query_date_condition = "da.attendance_date = %s"
            date_params = [attendance_date]
        elif from_date and to_date:
            # Date range mode
            query_date_condition = "da.attendance_date BETWEEN %s AND %s"
            date_params = [from_date, to_date]
        else:
            return jsonify({'status': 'error', 'message': 'Either date or from_date/to_date is required'}), 400

        db = get_db()
        cursor = db.cursor(dictionary=True)

        sql = f'''
            SELECT da.daily_atten_id AS id,
                   p.emp_code,
                   p.eb_id,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   COALESCE(s.sub_dept_desc, '') AS department_name,
                   COALESCE(d.desig, '') AS designation_name,
                   COALESCE(da.spell, '') AS shift_name,
                   da.attendance_date,
                   TIME(da.entry_time) AS attendance_time,
                   da.attendance_source AS status,
                   da.attendance_type AS att_type,
                   COALESCE(da.spell_hours, 0) AS shift_hours,
                   COALESCE(da.working_hours, 0) AS working_hours,
                   COALESCE(da.idle_hours, 0) AS idle_hours
            FROM daily_attendance da
            JOIN hrms_ed_personal_details p ON da.eb_id = p.eb_id
            LEFT JOIN sub_dept_mst s ON da.worked_department_id = s.sub_dept_id
            LEFT JOIN designation_mst d ON da.worked_designation_id = d.designation_id
            WHERE {query_date_condition} AND da.is_active = 1
        '''
        params = date_params

        if branch_id:
            sql += ' AND da.branch_id = %s'
            params.append(branch_id)

        if department_id:
            sql += ' AND da.worked_department_id = %s'
            params.append(department_id)

        if emp_code:
            sql += ' AND p.emp_code LIKE %s'
            params.append(f'%{emp_code}%')

        if emp_name:
            sql += ' AND (p.first_name LIKE %s OR p.middle_name LIKE %s OR p.last_name LIKE %s)'
            params.extend([f'%{emp_name}%', f'%{emp_name}%', f'%{emp_name}%'])

        if shift_name and shift_name != 'All Shifts':
            sql += ' AND da.spell = %s'
            params.append(shift_name)

        sql += ' ORDER BY da.attendance_date DESC, da.entry_time DESC'

        cursor.execute(sql, tuple(params))
        rows = cursor.fetchall()

        data = []
        for row in rows:
            # Fetch machine numbers for this attendance record
            cursor.execute('''
                SELECT mm.mech_code, mm.machine_name
                FROM daily_ebmc_attendance dea
                JOIN machine_mst mm ON dea.mech_id = mm.machine_id
                WHERE dea.daily_atten_id = %s AND dea.is_active = 1
                ORDER BY mm.mech_code
            ''', (row['id'],))
            machine_rows = cursor.fetchall()
            
            # Create comma-separated list of machine codes
            machine_nos = ', '.join([m['mech_code'] or '' for m in machine_rows if m['mech_code']])
            
            data.append({
                'id': row['id'],
                'emp_code': row['emp_code'],
                'eb_id': row['eb_id'],
                'emp_name': (row['emp_name'] or '').strip(),
                'department_name': row['department_name'] or '',
                'designation_name': row['designation_name'] or '',
                'shift_name': row['shift_name'] or '',
                'attendance_date': str(row['attendance_date']),
                'attendance_time': str(row['attendance_time']),
                'status': row['status'] or '',
                'att_type': row['att_type'] or 'R',
                'shift_hours': float(row['shift_hours']),
                'working_hours': float(row['working_hours']),
                'idle_hours': float(row['idle_hours']),
                'photo_att': '',
                'machine_nos': machine_nos
            })

        cursor.close()
        db.close()

        return jsonify({'status': 'success', 'data': data, 'total': len(data)})

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500
"""

# ══════════════════════════════════════════════════════════════════
# TEST THE API AFTER UPDATE
# ══════════════════════════════════════════════════════════════════

TEST_COMMANDS = """
# Test with date only
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&branch_id=29"

# Test with date and emp_code
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&emp_code=13111&branch_id=29"

# Test with all parameters
curl "http://192.168.0.223:5051/attendance-report?date=2026-04-24&emp_code=13111&emp_name=John&shift_name=Morning&branch_id=29"

# Expected response format:
{
  "status": "success",
  "data": [
    {
      "id": 1234,
      "emp_code": "13111",
      "eb_id": 5678,
      "emp_name": "John Doe",
      "designation_name": "Operator",
      "shift_name": "Morning",
      "attendance_date": "2026-04-24",
      "working_hours": 8.0,
      "machine_nos": "1001, 1002, 1003"
    }
  ],
  "total": 1
}
"""

