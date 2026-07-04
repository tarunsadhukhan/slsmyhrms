#!/usr/bin/env python
import sys
sys.path.insert(0, 'E:\\sjm\\MyHrms')

# Simple test without prompts
try:
    import mysql.connector
    
    db = mysql.connector.connect(
        host='localhost',
        user='root',
        password='',
        database='sls'
    )
    cursor = db.cursor(dictionary=True)
    
    cursor.execute("""
        SELECT spell_id AS id, spell_name AS name,
               starting_time AS start_time, end_time,
               COALESCE(working_hours, 8.0) AS working_hours
        FROM spell_mst ORDER BY spell_name LIMIT 3
    """)
    
    results = cursor.fetchall()
    print("Database query successful!")
    print(f"Found {len(results)} spells\n")
    
    for row in results:
        print(f"Spell: {row['name']}")
        print(f"  ID: {row['id']}")
        print(f"  Working Hours: {row['working_hours']}")
        print()
    
    cursor.close()
    db.close()
    
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()

