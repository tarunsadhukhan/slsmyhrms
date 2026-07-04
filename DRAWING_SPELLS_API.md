# Drawing Module API - Spells Endpoint Documentation

## GET /drawing/spells

### Description
Fetches spells (shifts) with their working hours for the Drawing Meter Entry module.

### Endpoint
```
GET /drawing/spells
```

### Query Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| branch_id | integer | No | Filter spells by branch ID |

### Response Format
```json
{
  "status": "success",
  "spells": [
    {
      "id": 91,
      "name": "A1",
      "start_time": "11:00:00",
      "end_time": "06:00:00",
      "working_hours": 5.0
    },
    {
      "id": 92,
      "name": "A2",
      "start_time": "17:00:00",
      "end_time": "14:00:00",
      "working_hours": 3.0
    },
    {
      "id": 95,
      "name": "C",
      "start_time": "06:00:00",
      "end_time": "22:00:00",
      "working_hours": 8.0
    }
  ],
  "total": 3
}
```

### Field Descriptions

#### Response Fields
- **status** (string): "success" or "error"
- **spells** (array): List of spell objects
- **total** (integer): Total number of spells returned

#### Spell Object Fields
- **id** (integer): Spell ID from spell_mst table
- **name** (string): Spell name (e.g., "A1", "B2", "C")
- **start_time** (string): Shift start time in HH:MM:SS format
- **end_time** (string): Shift end time in HH:MM:SS format
- **working_hours** (float): Number of working hours for this spell
  - Uses COALESCE to default to 8.0 if NULL in database
  - This value is used to auto-fill the hours field in Drawing Meter Entry

### Example Requests

#### Get All Spells
```bash
curl http://localhost:5051/drawing/spells
```

#### Get Spells for Specific Branch
```bash
curl "http://localhost:5051/drawing/spells?branch_id=29"
```

### Example Response

**Success:**
```json
{
  "status": "success",
  "spells": [
    {
      "id": 91,
      "name": "A1",
      "start_time": "11:00:00",
      "end_time": "06:00:00",
      "working_hours": 5.0
    }
  ],
  "total": 1
}
```

**Error:**
```json
{
  "status": "error",
  "message": "Database connection failed"
}
```

### Database Schema

#### Tables Used
**spell_mst** - Main spell/shift master table
```sql
CREATE TABLE spell_mst (
    spell_id INT PRIMARY KEY,
    spell_name VARCHAR(50),
    starting_time TIME,
    end_time TIME,
    working_hours DECIMAL(5,2),
    shift_id INT,
    ...
);
```

**shift_mst** - Shift master table (for branch filtering)
```sql
CREATE TABLE shift_mst (
    shift_id INT PRIMARY KEY,
    branch_id INT,
    ...
);
```

### SQL Query

#### With Branch Filter:
```sql
SELECT sm.spell_id AS id, sm.spell_name AS name,
       sm.starting_time AS start_time, sm.end_time,
       COALESCE(sm.working_hours, 8.0) AS working_hours
FROM spell_mst sm
JOIN shift_mst sh ON sm.shift_id = sh.shift_id
WHERE sh.branch_id = ?
ORDER BY sm.spell_name
```

#### Without Branch Filter:
```sql
SELECT spell_id AS id, spell_name AS name,
       starting_time AS start_time, end_time,
       COALESCE(working_hours, 8.0) AS working_hours
FROM spell_mst
ORDER BY spell_name
```

### Usage in Frontend

The frontend `DrawingMeterEntryActivity.kt` uses this endpoint to:

1. **Load spells dropdown** - Populate the spell selection spinner
2. **Auto-fill hours** - When a spell is selected, the working_hours value is used to automatically populate the hours input field
3. **Update hours on spell change** - Hours field updates when user selects a different spell

**Example Frontend Code:**
```kotlin
RetrofitClient.getApiService(this).getDrawingSpells(branchId)
    .enqueue(object : Callback<SpellResponse> {
        override fun onResponse(call: Call<SpellResponse>, response: Response<SpellResponse>) {
            spellList.clear()
            spellList.addAll(response.body()?.spells ?: emptyList())
            
            // Auto-fill hours from first spell
            if (spellList.isNotEmpty()) {
                val firstSpell = spellList[0]
                firstSpell.workingHours?.let { hours ->
                    etHours.setText(hours.toInt().toString())
                }
            }
        }
    })
```

### Notes

1. **Default Hours**: If `working_hours` is NULL in the database, the query returns 8.0 as the default value using `COALESCE(working_hours, 8.0)`

2. **Branch Filtering**: When branch_id is provided, the query joins with shift_mst to filter spells for that specific branch

3. **Ordering**: Results are always ordered by spell_name alphabetically

4. **Working Hours Purpose**: The working_hours field is critical for:
   - Auto-filling the hours input in Drawing Meter Entry
   - Calculating efficiency: `eff = ((unit / hours * 8) / const_value * 100)`
   - Improving data entry speed by eliminating manual hours entry

### Related Endpoints

- **GET /shifts** - Legacy endpoint in app.py (returns same data with 'shifts' key)
- **POST /drawing/entry** - Save drawing meter entry (uses hours for efficiency calculation)
- **GET /drawing/summary** - Get summary showing efficiency calculated from hours

### Error Handling

**Common Errors:**
- Database connection failure
- Invalid branch_id parameter
- Empty result set (not an error, returns empty array)

**HTTP Status Codes:**
- 200 OK - Success
- 500 Internal Server Error - Database or server error

### Testing

**Test with curl:**
```bash
# Test without branch filter
curl -X GET "http://localhost:5051/drawing/spells"

# Test with branch filter
curl -X GET "http://localhost:5051/drawing/spells?branch_id=29"

# Test response format
curl -X GET "http://localhost:5051/drawing/spells" | jq .
```

**Expected Test Results:**
- Should return JSON with 'spells' array
- Each spell should have working_hours field
- working_hours should never be null (minimum 8.0)
- Spells should be sorted by name

### Version History

- **v1.0** (May 6, 2026) - Initial implementation with working_hours support
  - Added COALESCE for default 8.0 hours
  - Added branch filtering
  - Integrated with DrawingMeterEntryActivity

---

**Location:** `E:\sjm\MyHrms\src\drawing\routes.py`  
**Blueprint:** `drawing_bp`  
**Full Path:** `/drawing/spells`

