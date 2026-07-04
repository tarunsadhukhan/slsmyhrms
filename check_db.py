import mysql.connector
db = mysql.connector.connect(host='13.126.47.172', user='myroot', password='deb#9876', database='sjm')
cur = db.cursor()

# What branches have April 1-7 data?
cur.execute('''SELECT branch_id, COUNT(*) FROM daily_attendance 
               WHERE attendance_date BETWEEN "2026-04-01" AND "2026-04-07"
               GROUP BY branch_id''')
print('Branch data Apr 1-7:', cur.fetchall())

# Sample attendance with emp details for Apr 1-7
cur.execute('''SELECT da.eb_id, da.branch_id, o.emp_code, p.first_name,
               s.sub_dept_desc, da.attendance_date, da.working_hours, da.spell_hours, da.spell
               FROM daily_attendance da
               LEFT JOIN hrms_ed_official_details o ON da.eb_id=o.eb_id
               LEFT JOIN hrms_ed_personal_details p ON da.eb_id=p.eb_id
               LEFT JOIN sub_dept_mst s ON da.worked_department_id=s.sub_dept_id
               WHERE da.attendance_date BETWEEN "2026-04-01" AND "2026-04-07"
               LIMIT 5''')
rows = cur.fetchall()
for r in rows:
    print(r)





