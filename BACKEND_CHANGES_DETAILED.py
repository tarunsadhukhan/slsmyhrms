"""
Backend Changes for Attendance Update Feature
Apply these changes to: e:\sjm\attendancesystem\app.py
"""

# ══════════════════════════════════════════════════════════════════
# CHANGE 1: Update /attendance-report endpoint
# ══════════════════════════════════════════════════════════════════

# FIND THIS SECTION (around line 1433):
"""
        sql = '''
            SELECT da.daily_atten_id AS id,
                   p.emp_code,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   ...
"""

# REPLACE WITH:
"""
        sql = '''
            SELECT da.daily_atten_id AS id,
                   p.emp_code,
                   p.eb_id,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   ...
"""

# ══════════════════════════════════════════════════════════════════
# CHANGE 2: Update response building to include machine numbers
# ══════════════════════════════════════════════════════════════════

# FIND THIS SECTION (around line 1475):
"""
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

# REPLACE WITH:
"""
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
# COMPLETE CODE EXAMPLE
# ══════════════════════════════════════════════════════════════════

COMPLETE_ATTENDANCE_REPORT_FUNCTION = """
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

        db = get_db()
        cursor = db.cursor(dictionary=True)

        sql = '''
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
# TESTING
# ══════════════════════════════════════════════════════════════════

TEST_API_CALL = """
# Test the updated API
curl "http://192.168.0.223:5051/attendance-report?from_date=2026-04-01&to_date=2026-04-30&branch_id=29"

# Expected response should now include:
# - "eb_id": 12345
# - "machine_nos": "1001, 1002, 1003"
"""

