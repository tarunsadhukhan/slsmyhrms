"""
Employee Master API Endpoints for Flask
Add these routes to your existing Flask app (app.py)

Table structure:
CREATE TABLE `employees` (
  `id` int NOT NULL AUTO_INCREMENT,
  `emp_code` varchar(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `department_id` int DEFAULT NULL,
  `designation_id` int DEFAULT NULL,
  `shift_id` int DEFAULT NULL,
  `face_embedding` longtext,
  `photo_html` longtext DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `emp_code` (`emp_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
"""

import os
import base64
import json
import numpy as np
from flask import Blueprint, request, jsonify
from datetime import datetime

# If using face_recognition library for face embeddings:
# pip install face_recognition numpy
try:
    import face_recognition
    FACE_RECOGNITION_AVAILABLE = True
except ImportError:
    FACE_RECOGNITION_AVAILABLE = False
    print("WARNING: face_recognition not installed. Face embedding will be skipped.")
    print("Install with: pip install face_recognition")

employee_bp = Blueprint('employees', __name__)

# Directory to store employee photos
PHOTO_DIR = os.path.join(os.path.dirname(__file__), 'employee_photos')
os.makedirs(PHOTO_DIR, exist_ok=True)


def get_db():
    """Get database connection - adjust to match your existing setup"""
    import mysql.connector
    return mysql.connector.connect(
        host='localhost',
        user='root',
        password='your_password',   # <-- Update this
        database='your_database'    # <-- Update this
    )


def generate_face_embedding(image_bytes):
    """Generate 128-d face embedding from image bytes"""
    if not FACE_RECOGNITION_AVAILABLE:
        return None

    # Save temp file for face_recognition
    temp_path = os.path.join(PHOTO_DIR, '_temp_face.jpg')
    with open(temp_path, 'wb') as f:
        f.write(image_bytes)

    try:
        image = face_recognition.load_image_file(temp_path)
        encodings = face_recognition.face_encodings(image)
        if len(encodings) > 0:
            return encodings[0].tolist()  # Convert numpy array to list
        else:
            return None
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


# ─── GET /employees ──────────────────────────────────────────────
@employee_bp.route('/employees', methods=['GET'])
def get_employees():
    """List all employees with department, designation, shift names"""
    try:
        db = get_db()
        cursor = db.cursor(dictionary=True)

        cursor.execute("""
            SELECT
                e.id,
                e.emp_code,
                e.name,
                e.department_id,
                e.designation_id,
                e.shift_id,
                e.photo_html,
                e.is_active,
                e.created_at,
                d.name AS department_name,
                o.name AS designation_name,
                s.name AS shift_name
            FROM employees e
            LEFT JOIN departments d ON e.department_id = d.id
            LEFT JOIN occupations o ON e.designation_id = o.id
            LEFT JOIN shifts s ON e.shift_id = s.id
            WHERE e.is_active = 1
            ORDER BY e.name
        """)

        employees = cursor.fetchall()

        # Convert datetime to string
        for emp in employees:
            if emp.get('created_at'):
                emp['created_at'] = emp['created_at'].strftime('%Y-%m-%d %H:%M:%S')


        cursor.close()
        db.close()

        return jsonify({
            'status': 'success',
            'data': employees,
            'total': len(employees)
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ─── POST /employees ─────────────────────────────────────────────
@employee_bp.route('/employees', methods=['POST'])
def add_employee():
    """Add a new employee with optional face registration"""
    try:
        data = request.get_json()

        emp_code = data.get('emp_code', '').strip()
        name = data.get('name', '').strip()
        department_id = data.get('department_id')
        designation_id = data.get('designation_id')
        shift_id = data.get('shift_id')
        face_image_b64 = data.get('face_image')  # base64 encoded image

        if not emp_code or not name:
            return jsonify({'status': 'error', 'message': 'emp_code and name are required'}), 400

        db = get_db()
        cursor = db.cursor()

        # Check duplicate emp_code
        cursor.execute("SELECT id FROM employees WHERE emp_code = %s", (emp_code,))
        if cursor.fetchone():
            cursor.close()
            db.close()
            return jsonify({'status': 'error', 'message': f'Employee code {emp_code} already exists'}), 400

        # Process face image
        photo_html = None
        face_embedding = None

        if face_image_b64:
            # Decode base64 image
            image_bytes = base64.b64decode(face_image_b64)

            # Store photo as HTML (base64 embedded) — no physical file saved
            photo_html = f'<img src="data:image/jpeg;base64,{face_image_b64}" />'

            # Generate face embedding for attendance matching
            embedding = generate_face_embedding(image_bytes)
            if embedding is not None:
                face_embedding = json.dumps(embedding)
            else:
                print(f"WARNING: No face detected in image for {emp_code}")

        # Insert into database
        cursor.execute("""
            INSERT INTO employees (emp_code, name, department_id, designation_id, shift_id,
                                   face_embedding, photo_html, is_active)
            VALUES (%s, %s, %s, %s, %s, %s, %s, 1)
        """, (emp_code, name, department_id, designation_id, shift_id,
              face_embedding, photo_html))

        db.commit()
        new_id = cursor.lastrowid
        cursor.close()
        db.close()

        return jsonify({
            'status': 'success',
            'id': new_id,
            'message': 'Employee added successfully!'
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ─── PUT /employees/<id> ─────────────────────────────────────────
@employee_bp.route('/employees/<int:emp_id>', methods=['PUT'])
def update_employee(emp_id):
    """Update an existing employee, optionally re-register face"""
    try:
        data = request.get_json()

        emp_code = data.get('emp_code', '').strip()
        name = data.get('name', '').strip()
        department_id = data.get('department_id')
        designation_id = data.get('designation_id')
        shift_id = data.get('shift_id')
        face_image_b64 = data.get('face_image')

        if not emp_code or not name:
            return jsonify({'status': 'error', 'message': 'emp_code and name are required'}), 400

        db = get_db()
        cursor = db.cursor(dictionary=True)

        # Check employee exists
        cursor.execute("SELECT * FROM employees WHERE id = %s", (emp_id,))
        existing = cursor.fetchone()
        if not existing:
            cursor.close()
            db.close()
            return jsonify({'status': 'error', 'message': 'Employee not found'}), 404

        # Check duplicate emp_code (excluding current)
        cursor.execute("SELECT id FROM employees WHERE emp_code = %s AND id != %s", (emp_code, emp_id))
        if cursor.fetchone():
            cursor.close()
            db.close()
            return jsonify({'status': 'error', 'message': f'Employee code {emp_code} already exists'}), 400

        # Process new face image if provided
        photo_html = existing.get('photo_html')
        face_embedding = existing.get('face_embedding')

        if face_image_b64:
            image_bytes = base64.b64decode(face_image_b64)

            # Store photo as HTML (base64 embedded) — no physical file saved
            photo_html = f'<img src="data:image/jpeg;base64,{face_image_b64}" />'

            # Regenerate face embedding
            embedding = generate_face_embedding(image_bytes)
            if embedding is not None:
                face_embedding = json.dumps(embedding)

        # Update database
        cursor.execute("""
            UPDATE employees
            SET emp_code = %s, name = %s, department_id = %s, designation_id = %s,
                shift_id = %s, face_embedding = %s, photo_html = %s
            WHERE id = %s
        """, (emp_code, name, department_id, designation_id, shift_id,
              face_embedding, photo_html, emp_id))

        db.commit()
        cursor.close()
        db.close()

        return jsonify({
            'status': 'success',
            'message': 'Employee updated successfully!'
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ─── DELETE /employees/<id> ───────────────────────────────────────
@employee_bp.route('/employees/<int:emp_id>', methods=['DELETE'])
def delete_employee(emp_id):
    """Soft delete an employee (set is_active = 0)"""
    try:
        db = get_db()
        cursor = db.cursor()

        cursor.execute("SELECT id FROM employees WHERE id = %s", (emp_id,))
        if not cursor.fetchone():
            cursor.close()
            db.close()
            return jsonify({'status': 'error', 'message': 'Employee not found'}), 404

        # Soft delete
        cursor.execute("UPDATE employees SET is_active = 0 WHERE id = %s", (emp_id,))
        db.commit()
        cursor.close()
        db.close()

        return jsonify({
            'status': 'success',
            'message': 'Employee deleted successfully!'
        })

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ─── POST /attendance (face matching) ────────────────────────────
# This is an enhanced version of your existing attendance endpoint
@employee_bp.route('/attendance', methods=['POST'])
def check_attendance_face():
    """
    Receives a base64 face image, compares against all registered employees'
    face_embeddings, and returns the matching employee.
    """
    try:
        if not FACE_RECOGNITION_AVAILABLE:
            return jsonify({
                'status': 'error',
                'message': 'face_recognition library not installed on server'
            }), 500

        data = request.get_json()
        face_image_b64 = data.get('image')

        if not face_image_b64:
            return jsonify({'status': 'error', 'message': 'No image provided'}), 400

        # Decode and get face encoding from uploaded image
        image_bytes = base64.b64decode(face_image_b64)
        temp_path = os.path.join(PHOTO_DIR, '_temp_attendance.jpg')
        with open(temp_path, 'wb') as f:
            f.write(image_bytes)

        try:
            image = face_recognition.load_image_file(temp_path)
            unknown_encodings = face_recognition.face_encodings(image)
        finally:
            if os.path.exists(temp_path):
                os.remove(temp_path)

        if len(unknown_encodings) == 0:
            return jsonify({
                'status': 'error',
                'message': 'No face detected in the image'
            }), 400

        unknown_encoding = unknown_encodings[0]

        # Load all registered employees with face embeddings
        db = get_db()
        cursor = db.cursor(dictionary=True)
        cursor.execute("""
            SELECT id, emp_code, name, face_embedding, photo_html
            FROM employees
            WHERE is_active = 1 AND face_embedding IS NOT NULL
        """)
        employees = cursor.fetchall()
        cursor.close()
        db.close()

        # Compare against each employee
        best_match = None
        best_distance = 1.0  # threshold

        for emp in employees:
            try:
                known_encoding = np.array(json.loads(emp['face_embedding']))
                distance = face_recognition.face_distance([known_encoding], unknown_encoding)[0]

                if distance < best_distance and distance < 0.6:  # 0.6 = tolerance threshold
                    best_distance = distance
                    best_match = emp
            except Exception:
                continue

        if best_match:
            return jsonify({
                'status': 'success',
                'emp_code': best_match['emp_code'],
                'emp_name': best_match['name'],
                'photo_html': best_match.get('photo_html'),
                'confidence': round((1 - best_distance) * 100, 2),
                'message': f"Face matched: {best_match['name']}"
            })
        else:
            return jsonify({
                'status': 'error',
                'message': 'Face not recognized. No matching employee found.'
            }), 404

    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ══════════════════════════════════════════════════════════════════
# HOW TO USE: Add this to your existing Flask app.py:
# ══════════════════════════════════════════════════════════════════
#
# from employee_routes import employee_bp
# app.register_blueprint(employee_bp)
#
# Or copy the routes directly into your app.py
# ══════════════════════════════════════════════════════════════════

