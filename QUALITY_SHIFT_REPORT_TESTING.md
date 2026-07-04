# Quality-wise Shift-wise Production Report - Testing Guide

## Prerequisites
1. Ensure backend server is running (`python app.py`)
2. Build and install the Android app
3. Have some winding entry data in the database

## Test Steps

### 1. Access the Report
1. Open the app and navigate to **Production Dashboard**
2. Click on **Winding Entry** (under Doff Entry section)
3. You should see the Winding Entry screen

### 2. Locate the Report Icon
1. Scroll down to the **Summary** section (bottom card)
2. Look at the Summary heading
3. You should see a **report icon (📊)** on the right side of "Summary"
4. The icon should be blue (#1565C0)

### 3. Open the Report
1. Click on the **report icon**
2. A dialog should appear with:
   - Title: "Quality-wise Shift-wise Production Report"
   - Current date displayed below title
   - Loading indicator initially

### 4. Verify Report Content
After loading completes, verify:

#### Header Row (Blue background):
- Quality
- Shift A
- Shift B  
- Shift C
- Total

#### Data Rows:
- Each quality should show in its own row
- Numbers should be formatted with 2 decimal places (e.g., 123.45)
- Total column should equal sum of Shift A + B + C

#### Grand Total (Blue highlighted row at bottom):
- Should show "Grand Total" label
- Shift A total = sum of all Shift A values
- Shift B total = sum of all Shift B values
- Shift C total = sum of all Shift C values
- Total = sum of all totals

### 5. Test Empty State
1. Select a date with no winding entries
2. Click the report icon
3. Should show: "No data available"

### 6. Test Error Handling
1. Stop the backend server
2. Click the report icon
3. Should show error message

### 7. Test Close Functionality
1. Open the report
2. Click "Close" button at bottom
3. Dialog should dismiss

## Expected Results

### Sample Report Output:
```
Quality-wise Shift-wise Production Report
15-01-2024

┌──────────────┬─────────┬─────────┬─────────┬─────────┐
│ Quality      │ Shift A │ Shift B │ Shift C │ Total   │
├──────────────┼─────────┼─────────┼─────────┼─────────┤
│ 40s Count    │  125.50 │  234.75 │  156.25 │  516.50 │
│ 60s Count    │   89.00 │  145.50 │  201.25 │  435.75 │
│ 80s Count    │   67.25 │  123.00 │  189.50 │  379.75 │
└──────────────┴─────────┴─────────┴─────────┴─────────┘

══════════════════════════════════════════════════════════
Grand Total   │  281.75 │  503.25 │  547.00 │ 1332.00 │
══════════════════════════════════════════════════════════
```

## Backend API Test

You can test the API directly using curl or Postman:

```bash
# Test API endpoint
curl "http://localhost:5051/doff/winding-entry-2-quality-shift-report?date=2024-01-15&branch_id=1"

# Expected Response:
{
  "status": "success",
  "message": "Quality-wise shift-wise report generated",
  "report": [
    {
      "quality_name": "40s Count",
      "shift_a": 125.50,
      "shift_b": 234.75,
      "shift_c": 156.25,
      "total": 516.50
    }
  ],
  "grand_total": {
    "shift_a": 281.75,
    "shift_b": 503.25,
    "shift_c": 547.00,
    "total": 1332.00
  }
}
```

## Troubleshooting

### Issue: Report icon not visible
- Solution: Rebuild the project (`./gradlew clean build`)
- Check that `activity_winding_entry.xml` was updated

### Issue: "Unresolved reference" errors
- Solution: Sync Gradle (`./gradlew build`)
- Check that all files were created correctly

### Issue: Empty report despite having data
- Check the database table: `winding_entry_2`
- Verify `spell_mst` has spell names with A, B, C
- Check `spinning_quality_mst` has quality data
- Verify date format is YYYY-MM-DD

### Issue: API returns error
- Check backend logs for SQL errors
- Verify database connection in `app.py`
- Ensure tables exist: `winding_entry_2`, `spell_mst`, `spinning_quality_mst`

## Database Setup (if needed)

If the report shows no data, ensure your database has the required tables:

```sql
-- Check if data exists
SELECT 
    we.winding_date,
    q.quality_name,
    s.spell_name,
    we.net_weight
FROM winding_entry_2 we
LEFT JOIN spell_mst s ON we.spell_id = s.spell_id
LEFT JOIN spinning_quality_mst q ON we.quality_id = q.quality_id
WHERE we.winding_date = '2024-01-15'
  AND we.branch_id = 1
LIMIT 10;
```

## Success Criteria
✅ Report icon appears next to Summary heading  
✅ Icon is blue and clickable  
✅ Dialog opens on icon click  
✅ Report loads with correct data  
✅ Quality rows display properly  
✅ Shift columns show correct values  
✅ Grand total calculates accurately  
✅ Empty state works  
✅ Close button works  
✅ No crashes or errors  

## Notes
- The report filters by the selected date in the Winding Entry screen
- Shift detection is based on spell name containing A, B, or C
- All weights are in kg with 2 decimal precision
- Report is read-only (no edit/delete functionality)

