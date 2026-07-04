"""
Drawing Module - API Routes
All endpoint handlers for drawing meter entry functionality
"""
from flask import request, jsonify
from . import drawing_bp
from ..database import get_db


@drawing_bp.route('/spells', methods=['GET'])
def get_drawing_spells():
    """
    Get spells (shifts) with working_hours for drawing entry
    Query params: ?branch_id=<id> (optional)
    
    Returns:
    {
        "status": "success",
        "spells": [
            {
                "id": <spell_id>,
                "name": "<spell_name>",
                "start_time": "HH:MM:SS",
                "end_time": "HH:MM:SS",
                "working_hours": <float>  // Default 8.0 if NULL
            }
        ]
    }
    """
    try:
        branch_id = request.args.get('branch_id', type=int)
        
        db = get_db()
        cursor = db.cursor(dictionary=True)
        
        if branch_id:
            cursor.execute("""
                SELECT sm.spell_id AS id, sm.spell_name AS name,
                       sm.starting_time AS start_time, sm.end_time,
                       COALESCE(sm.working_hours, 8.0) AS working_hours
                FROM spell_mst sm
                JOIN shift_mst sh ON sm.shift_id = sh.shift_id
                WHERE sh.branch_id = %s
                ORDER BY sm.spell_name
            """, (branch_id,))
        else:
            cursor.execute("""
                SELECT spell_id AS id, spell_name AS name,
                       starting_time AS start_time, end_time,
                       COALESCE(sm.working_hours, 8.0) AS working_hours
                FROM spell_mst sm
                ORDER BY sm.spell_name
            """)
        
        spells = cursor.fetchall()
        cursor.close()
        db.close()
        
        return jsonify({
            'status': 'success',
            'spells': spells,
            'total': len(spells)
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@drawing_bp.route('/sheds', methods=['GET'])
def get_drawing_sheds():
    """
    Get unique shed types from tbl_drawing_mst
    Query params: ?branch_id=<id> (optional)
    """
    try:
        branch_id = request.args.get('branch_id', type=int)
        
        db = get_db()
        cursor = db.cursor(dictionary=True)
        
        query = """
            SELECT DISTINCT shed_type
            FROM tbl_drawing_mst
            WHERE 1=1
        """
        params = []
        
        if branch_id:
            query += " AND branch_id = %s"
            params.append(branch_id)
        
        query += " ORDER BY shed_type"
        
        cursor.execute(query, params if params else None)
        sheds = cursor.fetchall()
        
        cursor.close()
        db.close()
        
        return jsonify({
            'status': 'success',
            'sheds': [s['shed_type'] for s in sheds]
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@drawing_bp.route('/machines', methods=['GET'])
def get_drawing_machines():
    """
    Get machines for a specific shed type
    Query params: 
        ?shed_type=<type> (required)
        ?branch_id=<id> (optional)
    """
    try:
        shed_type = request.args.get('shed_type')
        branch_id = request.args.get('branch_id', type=int)
        
        if not shed_type:
            return jsonify({'status': 'error', 'message': 'shed_type required'}), 400
        
        db = get_db()
        cursor = db.cursor(dictionary=True)
        
        query = """
            SELECT mc_id, mc_short_name, cont_meter
            FROM tbl_drawing_mst
            WHERE shed_type = %s
        """
        params = [shed_type]
        
        if branch_id:
            query += " AND branch_id = %s"
            params.append(branch_id)
        
        query += " ORDER BY mc_short_name"
        
        cursor.execute(query, params)
        machines = cursor.fetchall()
        
        cursor.close()
        db.close()
        
        return jsonify({
            'status': 'success',
            'machines': machines
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@drawing_bp.route('/opening-meter', methods=['GET'])
def get_drawing_opening_meter():
    """
    Get opening meter (previous closing meter) for a machine
    Query params:
        ?date=YYYY-MM-DD (required)
        ?spell_id=<id> (required)
        ?mc_id=<id> (required)
    
    Returns the closing meter from the most recent previous entry
    for the same machine, before the given date+spell
    """
    try:
        date_str = request.args.get('date')
        spell_id = request.args.get('spell_id', type=int)
        mc_id = request.args.get('mc_id', type=int)
        
        if not all([date_str, spell_id, mc_id]):
            return jsonify({'status': 'error', 'message': 'date, spell_id, mc_id required'}), 400
        
        db = get_db()
        cursor = db.cursor(dictionary=True)
        
        # Get the most recent closing meter for this machine before the current date+spell
        query = """
            SELECT closing_meter
            FROM tbl_daily_drawing
            WHERE mc_id = %s
              AND (date < %s OR (date = %s AND spell_id < %s))
            ORDER BY date DESC, spell_id DESC
            LIMIT 1
        """
        
        cursor.execute(query, (mc_id, date_str, date_str, spell_id))
        result = cursor.fetchone()
        
        cursor.close()
        db.close()
        
        opening_meter = result['closing_meter'] if result else 0.0
        
        return jsonify({
            'status': 'success',
            'opening_meter': opening_meter
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@drawing_bp.route('/entry', methods=['POST'])
def save_drawing_entry():
    """
    Save drawing meter entry
    
    Request body (JSON):
    {
        "date": "YYYY-MM-DD",
        "spell_id": <int>,
        "shed_type": "<string>",
        "mc_id": <int>,
        "opening_meter": <float>,
        "closing_meter": <float>,
        "hours": <float>,
        "const_value": <float>,  // Efficiency constant
        "branch_id": <int>,
        "user_id": <int>
    }
    
    Calculates:
    - unit = closing_meter - opening_meter
    - eff = ((unit / hours * 8) / const_value * 100) rounded to 2 decimals
    
    If entry exists for date+spell+mc, updates it; otherwise inserts new
    """
    try:
        data = request.get_json()
        
        date_str = data.get('date')
        spell_id = data.get('spell_id')
        shed_type = data.get('shed_type')
        mc_id = data.get('mc_id')
        opening_meter = data.get('opening_meter', 0.0)
        closing_meter = data.get('closing_meter', 0.0)
        hours = data.get('hours', 0.0)
        const_value = data.get('const_value', 1.0)  # Efficiency constant
        branch_id = data.get('branch_id')
        user_id = data.get('user_id', 0)
        
        if not all([date_str, spell_id, shed_type, mc_id]):
            return jsonify({'status': 'error', 'message': 'Required fields missing'}), 400
        
        # Calculate unit and efficiency
        unit = closing_meter - opening_meter
        if hours > 0 and const_value > 0:
            eff = round(((unit / hours * 8) / const_value * 100), 2)
        else:
            eff = 0.0
        
        db = get_db()
        cursor = db.cursor()
        
        # Check if entry exists for this date+spell+mc
        cursor.execute("""
            SELECT id FROM tbl_daily_drawing
            WHERE date = %s AND spell_id = %s AND mc_id = %s
        """, (date_str, spell_id, mc_id))
        
        existing = cursor.fetchone()
        
        if existing:
            # Update existing entry
            cursor.execute("""
                UPDATE tbl_daily_drawing
                SET shed_type = %s, opening_meter = %s, closing_meter = %s,
                    unit = %s, hours = %s, eff = %s, branch_id = %s,
                    user_id = %s, updated_at = NOW()
                WHERE id = %s
            """, (shed_type, opening_meter, closing_meter, unit, hours, eff,
                  branch_id, user_id, existing[0]))
            entry_id = existing[0]
            message = 'Entry updated successfully'
        else:
            # Insert new entry
            cursor.execute("""
                INSERT INTO tbl_daily_drawing
                (date, spell_id, shed_type, mc_id, opening_meter, closing_meter,
                 unit, hours, eff, branch_id, user_id, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
            """, (date_str, spell_id, shed_type, mc_id, opening_meter, closing_meter,
                  unit, hours, eff, branch_id, user_id))
            entry_id = cursor.lastrowid
            message = 'Entry saved successfully'
        
        db.commit()
        cursor.close()
        db.close()
        
        return jsonify({
            'status': 'success',
            'message': message,
            'id': entry_id,
            'unit': unit,
            'eff': eff
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@drawing_bp.route('/summary', methods=['GET'])
def get_drawing_summary():
    """
    Get drawing meter entry summary for date+spell
    
    Query params:
        ?date=YYYY-MM-DD (required)
        ?spell_id=<id> (required)
        ?branch_id=<id> (optional)
    
    Returns list of entries with machine name, unit, and efficiency
    """
    try:
        date_str = request.args.get('date')
        spell_id = request.args.get('spell_id', type=int)
        branch_id = request.args.get('branch_id', type=int)
        
        if not all([date_str, spell_id]):
            return jsonify({'status': 'error', 'message': 'date and spell_id required'}), 400
        
        db = get_db()
        cursor = db.cursor(dictionary=True)
        
        query = """
            SELECT 
                d.mc_id,
                m.mc_short_name,
                d.unit,
                d.eff
            FROM tbl_daily_drawing d
            JOIN tbl_drawing_mst m ON d.mc_id = m.mc_id
            WHERE d.date = %s AND d.spell_id = %s
        """
        params = [date_str, spell_id]
        
        if branch_id:
            query += " AND d.branch_id = %s"
            params.append(branch_id)
        
        query += " ORDER BY m.mc_short_name"
        
        cursor.execute(query, params)
        summary = cursor.fetchall()
        
        cursor.close()
        db.close()
        
        return jsonify({
            'status': 'success',
            'summary': summary
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

