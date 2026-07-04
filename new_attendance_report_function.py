def attendance_report():
    try:
        ok, errors = AttendanceReportSchema.validate(dict(request.args))
        if not ok:
            return jsonify({'status': 'error', 'message': errors[0]}), 400

        # Support BOTH single date and date range
        attendance_date = request.args.get('date')
        from_date       = request.args.get('from_date')
        to_date         = request.args.get('to_date')
        department_id   = request.args.get('department_id')
        emp_code        = request.args.get('emp_code', '').strip()
        emp_name        = request.args.get('emp_name', '').strip()
        shift_name      = request.args.get('shift_name', '').strip()
        branch_id       = request.args.get('branch_id', type=int)

        # Determine which query mode
        if attendance_date:
            # Single date mode
            date_condition = "da.attendance_date = %s"
            date_params = [attendance_date]
        elif from_date and to_date:
            # Date range mode
            date_condition = "da.attendance_date BETWEEN %s AND %s"
            date_params = [from_date, to_date]
        else:
            return jsonify({'status': 'error', 'message': 'Either date or from_date/to_date is required'}), 400

        db     = get_db()
        cursor = db.cursor(dictionary=True)

        # Build dynamic SQL query
        sql = f"""
            SELECT da.daily_atten_id AS id, o.emp_code, o.eb_id,
                   CONCAT(p.first_name, ' ', COALESCE(p.middle_name, ''), ' ', COALESCE(p.last_name, '')) AS emp_name,
                   COALESCE(s.sub_dept_desc, '') AS department_name,
                   COALESCE(d.desig, '')         AS designation_name,
                   COALESCE(da.spell, '')        AS shift_name,
                   da.attendance_date,
                   TIME(da.entry_time)           AS attendance_time,
                   da.attendance_source          AS status,
                   COALESCE(da.attendance_type, 'R') AS att_type,
                   COALESCE(da.spell_hours,   0) AS shift_hours,
                   COALESCE(da.working_hours, 0) AS working_hours,
                   COALESCE(da.idle_hours,    0) AS idle_hours,
                   IF(EXISTS(
                     SELECT 1
                     FROM employee_face_mst ef
                     WHERE ef.eb_id = da.eb_id AND ef.active = 1
                   ), 1, 0) AS has_photo
            FROM daily_attendance da
            LEFT JOIN hrms_ed_personal_details p ON da.eb_id = p.eb_id
            LEFT JOIN hrms_ed_official_details o ON da.eb_id = o.eb_id
            LEFT JOIN sub_dept_mst    s ON o.sub_dept_id    = s.sub_dept_id
            LEFT JOIN designation_mst d ON o.designation_id = d.designation_id
            WHERE {date_condition} AND da.is_active = 1
        """
        params = date_params

        # Add filters
        if branch_id:
            sql += " AND da.branch_id = %s"
            params.append(branch_id)
        
        if department_id:
            sql += " AND o.sub_dept_id = %s"
            params.append(department_id)
        
        if emp_code:
            sql += " AND o.emp_code LIKE %s"
            params.append(f"%{emp_code}%")
        
        if emp_name:
            sql += " AND (p.first_name LIKE %s OR p.middle_name LIKE %s OR p.last_name LIKE %s)"
            params.extend([f"%{emp_name}%", f"%{emp_name}%", f"%{emp_name}%"])
        
        if shift_name and shift_name != 'All Shifts':
            sql += " AND da.spell = %s"
            params.append(shift_name)

        sql += " ORDER BY da.attendance_date DESC, da.entry_time DESC"
        
        cursor.execute(sql, tuple(params))
        rows = cursor.fetchall()

        # Build response with machine numbers
        data = []
        for row in rows:
            # Fetch machine numbers for this attendance record
            cursor.execute("""
                SELECT mm.mech_code
                FROM daily_ebmc_attendance dea
                JOIN machine_mst mm ON dea.mc_id = mm.machine_id
                WHERE dea.daily_atten_id = %s AND dea.is_active = 1
                ORDER BY mm.mech_code
            """, (row['id'],))
            machine_rows = cursor.fetchall()
            
            # Create comma-separated list of machine codes
            machine_nos = ', '.join([m['mech_code'] or '' for m in machine_rows if m.get('mech_code')])
            
            data.append({
                'id':               row['id'],
                'emp_code':         row['emp_code'],
                'eb_id':            row['eb_id'],
                'emp_name':         row['emp_name'] or '',
                'department_name':  row['department_name'] or '',
                'designation_name': row['designation_name'] or '',
                'shift_name':       row['shift_name'] or '',
                'attendance_date':  str(row['attendance_date']),
                'attendance_time':  str(row['attendance_time']),
                'status':           row['status'] or '',
                'att_type':         row['att_type'] or 'R',
                'shift_hours':      float(row['shift_hours']),
                'working_hours':    float(row['working_hours']),
                'idle_hours':       float(row['idle_hours']),
                'has_photo':        bool(row['has_photo']),
                'machine_nos':      machine_nos
            })

        cursor.close()
        db.close()

        return jsonify({'status': 'success', 'data': data, 'total': len(data)})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

