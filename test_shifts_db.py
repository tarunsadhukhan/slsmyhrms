import mysql.connector
import json

# Test database connection and shifts query
config = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'sls'
}

try:
    db = mysql.connector.connect(**config)
    cursor = db.cursor(dictionary=True)
    
    # Test the query
    cursor.execute("""
        SELECT spell_id AS id, spell_name AS name,
               starting_time AS start_time, end_time,
               COALESCE(working_hours, 8.0) AS working_hours
        FROM spell_mst ORDER BY spell_name LIMIT 5
    """)
    
    results = cursor.fetchall()
    print("✅ Database connection successful!")
    print(f"✅ Found {len(results)} spells")
    print("\nSample data:")
    for row in results:
        print(json.dumps(row, default=str, indent=2))
    
    cursor.close()
    db.close()
    
except Exception as e:
    print(f"❌ Error: {e}")

