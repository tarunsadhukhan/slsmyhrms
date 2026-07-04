# Drawing Meter Entry - Updates Implementation Plan

## Overview

This plan implements the following changes to the Drawing Meter Entry feature:
1. **Shed selection**: Change from dropdown to button grid (like machines)
2. **All meters**: Change from decimal to integer values
3. **Default hours**: Fetch from `spell_mst.working_hours` based on selected spell
4. **Meter display**: Already shows `const_meter` from `tbl_drawing_mst` (verified)
5. **Machine name**: Use `short_name` directly, not "MC" + mc_id

---

## Changes Required

### 1. Update Spell Data Model
**File:** `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\DoffResponse.kt`

**Current Code (lines 7-12):**
```kotlin
data class Spell(
    @SerializedName("spell_id")   val spellId: Int,
    @SerializedName("spell_name") val spellName: String?
) {
    override fun toString() = spellName ?: ""
}
```

**New Code:**
```kotlin
data class Spell(
    @SerializedName("spell_id")   val spellId: Int,
    @SerializedName("spell_name") val spellName: String?,
    @SerializedName("working_hours") val workingHours: Double? = 8.0
) {
    override fun toString() = spellName ?: ""
}
```

---

### 2. Update Activity Layout XML

**File:** `e:\sjm\MyHrms\app\src\main\res\layout\activity_drawing_meter_entry.xml`

#### Change 2A: Replace Shed Dropdown with Button Grid

**Find and replace the shed dropdown section (around lines 134-156):**

**OLD CODE:**
```xml
<!-- Row: Shed Dropdown -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:layout_marginBottom="8dp">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Shed"
        android:textColor="#000000"
        android:textSize="12sp"
        android:textStyle="bold"
        android:layout_marginBottom="3dp"/>
    <Spinner
        android:id="@+id/spShed"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:background="@drawable/bg_input_rounded_light"
        android:paddingStart="10dp"
        android:paddingEnd="10dp"/>
</LinearLayout>
```

**NEW CODE:**
```xml
<!-- ROW: Shed Buttons -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Shed"
    android:textColor="#000000"
    android:textSize="12sp"
    android:textStyle="bold"
    android:layout_marginBottom="4dp"/>

<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:scrollbars="none"
    android:layout_marginBottom="4dp">
    <LinearLayout
        android:id="@+id/llShedButtons"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:paddingBottom="4dp"/>
</HorizontalScrollView>

<TextView
    android:id="@+id/tvShedStatus"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text=""
    android:textSize="12sp"
    android:textColor="#777777"
    android:layout_marginBottom="8dp"
    android:visibility="gone"/>
```

#### Change 2B: Change Opening Meter to Integer Input

**Find etOpening EditText (around line 212) and change inputType:**

**OLD CODE:**
```xml
<EditText
    android:id="@+id/etOpening"
    android:layout_width="match_parent"
    android:layout_height="44dp"
    android:background="@drawable/bg_input_rounded_light"
    android:paddingStart="10dp"
    android:paddingEnd="10dp"
    android:inputType="numberDecimal"
    android:textColor="#000000"
    android:textSize="14sp"
    android:hint="0.0"/>
```

**NEW CODE:**
```xml
<EditText
    android:id="@+id/etOpening"
    android:layout_width="match_parent"
    android:layout_height="44dp"
    android:background="@drawable/bg_input_rounded_light"
    android:paddingStart="10dp"
    android:paddingEnd="10dp"
    android:inputType="number"
    android:textColor="#000000"
    android:textSize="14sp"
    android:hint="0"/>
```

#### Change 2C: Change Closing Meter to Integer Input

**Find etClosing EditText (around line 240) and change inputType:**

**OLD CODE:**
```xml
<EditText
    android:id="@+id/etClosing"
    android:layout_width="match_parent"
    android:layout_height="44dp"
    android:background="@drawable/bg_input_rounded_light"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:inputType="numberDecimal"
    android:textColor="#000000"
    android:textSize="13sp"
    android:hint="0.0"/>
```

**NEW CODE:**
```xml
<EditText
    android:id="@+id/etClosing"
    android:layout_width="match_parent"
    android:layout_height="44dp"
    android:background="@drawable/bg_input_rounded_light"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:inputType="number"
    android:textColor="#000000"
    android:textSize="13sp"
    android:hint="0"/>
```

---

### 3. Update DrawingMeterEntryActivity.kt

**File:** `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\DrawingMeterEntryActivity.kt`

#### Change 3A: Update Class Variables (Lines 32-54)

**OLD CODE:**
```kotlin
private lateinit var tvEntryDate       : TextView
private lateinit var spEntrySpell      : Spinner
private lateinit var spShed            : Spinner
private lateinit var llMachineButtons  : LinearLayout
// ...existing code...

// Selected values
private var selectedMachine: DrawingMachine? = null
private val constValue = 100.0  // Efficiency constant - adjust as needed
```

**NEW CODE:**
```kotlin
private lateinit var tvEntryDate       : TextView
private lateinit var spEntrySpell      : Spinner
private lateinit var llShedButtons     : LinearLayout
private lateinit var tvShedStatus      : TextView
private lateinit var llMachineButtons  : LinearLayout
// ...existing code...

// Selected values
private var selectedShed: String? = null
private var selectedMachine: DrawingMachine? = null
private val constValue = 100.0  // Efficiency constant - adjust as needed
```

#### Change 3B: Update onCreate() - Initialize Shed Buttons (Lines 70-72)

**OLD CODE:**
```kotlin
tvEntryDate      = findViewById(R.id.tvEntryDate)
spEntrySpell     = findViewById(R.id.spEntrySpell)
spShed           = findViewById(R.id.spShed)
llMachineButtons = findViewById(R.id.llMachineButtons)
```

**NEW CODE:**
```kotlin
tvEntryDate      = findViewById(R.id.tvEntryDate)
spEntrySpell     = findViewById(R.id.spEntrySpell)
llShedButtons    = findViewById(R.id.llShedButtons)
tvShedStatus     = findViewById(R.id.tvShedStatus)
llMachineButtons = findViewById(R.id.llMachineButtons)
```

#### Change 3C: Update Spell Listener - Auto-fill Hours (Lines 105-111)

**OLD CODE:**
```kotlin
// Spell change listener
spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        clearForm()
        loadSummary()
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

**NEW CODE:**
```kotlin
// Spell change listener
spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // Set working hours from selected spell
        val spell = spellList.getOrNull(position)
        spell?.workingHours?.let { hours ->
            etHours.setText(hours.toInt().toString())
        }
        clearForm()
        loadSummary()
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

#### Change 3D: Remove Shed Spinner Listener (Lines 113-124)

**REMOVE THIS CODE:**
```kotlin
// Shed change listener
spShed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (position > 0) {
            loadMachines(shedList[position - 1])
        } else {
            machineList.clear()
            llMachineButtons.removeAllViews()
        }
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

#### Change 3E: Add Opening Meter Listener (After line 142)

**ADD THIS CODE:**
```kotlin
// Opening field watcher - recalculate when opening changes
etOpening.addTextChangedListener(object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        calculateUnitAndEff()
    }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
})
```

#### Change 3F: Update loadSpells() - Include working_hours (Lines 163-178)

**OLD CODE:**
```kotlin
private fun loadSpells() {
    RetrofitClient.getApiService(this).getShifts(branchId)
        .enqueue(object : Callback<ShiftResponse> {
            override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                spellList.clear()
                spellList.addAll(response.body()?.shifts?.map { Spell(it.id, it.name) } ?: emptyList())
                val adapter = ArrayAdapter(this@DrawingMeterEntryActivity, android.R.layout.simple_spinner_item, spellList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spEntrySpell.adapter = adapter
                if (spellList.isNotEmpty()) loadSummary()
            }
            override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load spells: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
```

**NEW CODE:**
```kotlin
private fun loadSpells() {
    RetrofitClient.getApiService(this).getShifts(branchId)
        .enqueue(object : Callback<ShiftResponse> {
            override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                spellList.clear()
                spellList.addAll(response.body()?.shifts?.map { 
                    Spell(it.id, it.name, it.workingHours) 
                } ?: emptyList())
                val adapter = ArrayAdapter(this@DrawingMeterEntryActivity, android.R.layout.simple_spinner_item, spellList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spEntrySpell.adapter = adapter
                if (spellList.isNotEmpty()) loadSummary()
            }
            override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load spells: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
```

#### Change 3G: Update loadSheds() - Render Buttons (Lines 180-196)

**OLD CODE:**
```kotlin
private fun loadSheds() {
    RetrofitClient.getApiService(this).getDrawingSheds(branchId)
        .enqueue(object : Callback<DrawingShedsResponse> {
            override fun onResponse(call: Call<DrawingShedsResponse>, response: Response<DrawingShedsResponse>) {
                shedList.clear()
                shedList.addAll(response.body()?.sheds ?: emptyList())
                val items = mutableListOf("-- Select Shed --")
                items.addAll(shedList)
                val adapter = ArrayAdapter(this@DrawingMeterEntryActivity, android.R.layout.simple_spinner_item, items)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spShed.adapter = adapter
            }
            override fun onFailure(call: Call<DrawingShedsResponse>, t: Throwable) {
                Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load sheds: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
```

**NEW CODE:**
```kotlin
private fun loadSheds() {
    RetrofitClient.getApiService(this).getDrawingSheds(branchId)
        .enqueue(object : Callback<DrawingShedsResponse> {
            override fun onResponse(call: Call<DrawingShedsResponse>, response: Response<DrawingShedsResponse>) {
                shedList.clear()
                shedList.addAll(response.body()?.sheds ?: emptyList())
                renderShedButtons()
            }
            override fun onFailure(call: Call<DrawingShedsResponse>, t: Throwable) {
                Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load sheds: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
}
```

#### Change 3H: Add renderShedButtons() Method (After loadSheds)

**ADD THIS NEW METHOD:**
```kotlin
private fun renderShedButtons() {
    llShedButtons.removeAllViews()
    if (shedList.isEmpty()) {
        tvShedStatus.text = "No sheds found"
        tvShedStatus.visibility = View.VISIBLE
        return
    }
    tvShedStatus.visibility = View.GONE

    for (shed in shedList) {
        val btn = Button(this).apply {
            text = shed
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            val dp80 = (80 * resources.displayMetrics.density).toInt()
            val dp36 = (36 * resources.displayMetrics.density).toInt()
            val dp6  = (6 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(dp80, dp36).apply { marginEnd = dp6 }
            setOnClickListener { selectShed(shed) }
        }
        llShedButtons.addView(btn)
    }
}
```

#### Change 3I: Add selectShed() Method (After renderShedButtons)

**ADD THIS NEW METHOD:**
```kotlin
private fun selectShed(shed: String) {
    selectedShed = shed
    // Update all button colors
    for (i in 0 until llShedButtons.childCount) {
        val btn = llShedButtons.getChildAt(i) as? Button ?: continue
        if (shedList.getOrNull(i) == shed) {
            btn.setBackgroundColor(Color.parseColor("#2E7D32"))  // Green
        } else {
            btn.setBackgroundColor(Color.parseColor("#1565C0"))  // Blue
        }
    }
    // Load machines for selected shed
    loadMachines(shed)
}
```

#### Change 3J: Update renderMachineButtons() - Fix Machine Name (Lines 221-235)

**OLD CODE:**
```kotlin
for (mc in machineList) {
    val btn = Button(this).apply {
        text = mc.mcShortName ?: "MC${mc.mcId}"
        textSize = 12f
        // ...existing code...
    }
    llMachineButtons.addView(btn)
}
```

**NEW CODE:**
```kotlin
for (mc in machineList) {
    val btn = Button(this).apply {
        text = mc.mcShortName ?: ""
        textSize = 12f
        // ...existing code...
    }
    llMachineButtons.addView(btn)
}
```

#### Change 3K: Update selectMachine() - Integer Meter Display (Line 251)

**OLD CODE:**
```kotlin
// Update meter display
tvMeter.text = String.format(Locale.getDefault(), "%.2f", mc.contMeter ?: 0.0)
```

**NEW CODE:**
```kotlin
// Update meter display
tvMeter.text = (mc.contMeter?.toInt() ?: 0).toString()
```

#### Change 3L: Update loadOpeningMeter() - Integer Display (Lines 262-263)

**OLD CODE:**
```kotlin
val opening = response.body()?.openingMeter ?: 0.0
etOpening.setText(String.format(Locale.getDefault(), "%.2f", opening))
```

**NEW CODE:**
```kotlin
val opening = response.body()?.openingMeter?.toInt() ?: 0
etOpening.setText(opening.toString())
```

#### Change 3M: Update calculateUnitAndEff() - Integer Calculations (Lines 271-284)

**OLD CODE:**
```kotlin
private fun calculateUnitAndEff() {
    val opening = etOpening.text.toString().toDoubleOrNull() ?: 0.0
    val closing = etClosing.text.toString().toDoubleOrNull() ?: 0.0
    val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0

    val unit = closing - opening
    tvUnit.text = String.format(Locale.getDefault(), "%.2f", unit)

    val eff = if (hours > 0 && constValue > 0) {
        ((unit / hours * 8) / constValue * 100)
    } else {
        0.0
    }
    tvEff.text = String.format(Locale.getDefault(), "%.2f", eff)
}
```

**NEW CODE:**
```kotlin
private fun calculateUnitAndEff() {
    val opening = etOpening.text.toString().toIntOrNull() ?: 0
    val closing = etClosing.text.toString().toIntOrNull() ?: 0
    val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0

    val unit = closing - opening
    tvUnit.text = unit.toString()

    val eff = if (hours > 0 && constValue > 0) {
        ((unit / hours * 8) / constValue * 100)
    } else {
        0.0
    }
    tvEff.text = String.format(Locale.getDefault(), "%.2f", eff)
}
```

#### Change 3N: Update saveEntry() - Use selectedShed (Lines 287-314)

**OLD CODE:**
```kotlin
private fun saveEntry() {
    val spellId = selectedSpellId()
    val shedPos = spShed.selectedItemPosition
    val mc = selectedMachine

    if (spellId == null) {
        Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show()
        return
    }
    if (shedPos == 0) {
        Toast.makeText(this, "Please select a shed", Toast.LENGTH_SHORT).show()
        return
    }
    if (mc == null) {
        Toast.makeText(this, "Please select a machine", Toast.LENGTH_SHORT).show()
        return
    }

    val opening = etOpening.text.toString().toDoubleOrNull() ?: 0.0
    val closing = etClosing.text.toString().toDoubleOrNull() ?: 0.0
    val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0

    if (closing == 0.0) {
        Toast.makeText(this, "Please enter closing meter", Toast.LENGTH_SHORT).show()
        return
    }

    val shedType = shedList[shedPos - 1]
    val req = DrawingEntrySaveRequest(
        // ...existing code...
    )
```

**NEW CODE:**
```kotlin
private fun saveEntry() {
    val spellId = selectedSpellId()
    val shed = selectedShed
    val mc = selectedMachine

    if (spellId == null) {
        Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show()
        return
    }
    if (shed == null) {
        Toast.makeText(this, "Please select a shed", Toast.LENGTH_SHORT).show()
        return
    }
    if (mc == null) {
        Toast.makeText(this, "Please select a machine", Toast.LENGTH_SHORT).show()
        return
    }

    val opening = etOpening.text.toString().toIntOrNull() ?: 0
    val closing = etClosing.text.toString().toIntOrNull() ?: 0
    val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0

    if (closing == 0) {
        Toast.makeText(this, "Please enter closing meter", Toast.LENGTH_SHORT).show()
        return
    }

    val req = DrawingEntrySaveRequest(
        date = entryDate,
        spellId = spellId,
        shedType = shed,
        mcId = mc.mcId,
        openingMeter = opening.toDouble(),
        closingMeter = closing.toDouble(),
        hours = hours,
        constValue = constValue,
        branchId = branchId,
        userId = userId
    )
    // ...existing code...
}
```

#### Change 3O: Update clearForm() - Reset Shed Selection (Lines 373-383)

**OLD CODE:**
```kotlin
private fun clearForm() {
    selectedMachine = null
    spShed.setSelection(0)
    llMachineButtons.removeAllViews()
    tvMeter.text = "—"
    etOpening.setText("")
    etClosing.setText("")
    etHours.setText("")
    tvUnit.text = "0.0"
    tvEff.text = "0.0"
}
```

**NEW CODE:**
```kotlin
private fun clearForm() {
    selectedMachine = null
    selectedShed = null
    
    // Reset shed button colors
    for (i in 0 until llShedButtons.childCount) {
        val btn = llShedButtons.getChildAt(i) as? Button ?: continue
        btn.setBackgroundColor(Color.parseColor("#1565C0"))
    }
    
    llMachineButtons.removeAllViews()
    tvMeter.text = "—"
    etOpening.setText("")
    etClosing.setText("")
    etHours.setText("")
    tvUnit.text = "0"
    tvEff.text = "0.0"
}
```

---

### 4. Backend Updates

**File:** `e:\sjm\attendancesystem\app.py` (or wherever /shifts endpoint is defined)

#### Update /shifts Endpoint to Return working_hours

**OLD CODE:**
```python
@app.route('/shifts', methods=['GET'])
def get_shifts():
    branch_id = request.args.get('branch_id', type=int)
    
    db = get_db()
    cursor = db.cursor(dictionary=True)
    
    query = """
        SELECT 
            id as spell_id, 
            name as spell_name
        FROM spell_mst 
        WHERE active = 1
    """
    
    if branch_id:
        query += " AND branch_id = %s"
        cursor.execute(query, (branch_id,))
    else:
        cursor.execute(query)
    
    shifts = cursor.fetchall()
    cursor.close()
    
    return jsonify({
        'status': 'success',
        'shifts': shifts
    })
```

**NEW CODE:**
```python
@app.route('/shifts', methods=['GET'])
def get_shifts():
    branch_id = request.args.get('branch_id', type=int)
    
    db = get_db()
    cursor = db.cursor(dictionary=True)
    
    query = """
        SELECT 
            id as spell_id, 
            name as spell_name,
            COALESCE(working_hours, 8.0) as working_hours
        FROM spell_mst 
        WHERE active = 1
    """
    
    if branch_id:
        query += " AND branch_id = %s"
        cursor.execute(query, (branch_id,))
    else:
        cursor.execute(query)
    
    shifts = cursor.fetchall()
    cursor.close()
    
    return jsonify({
        'status': 'success',
        'shifts': shifts
    })
```

---

## Testing Checklist

After implementing all changes:

### Build & Install
```powershell
cd E:\sjm\MyHrms
.\gradlew clean assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Functional Testing
- [ ] Shed buttons appear horizontally (not dropdown)
- [ ] Selected shed button turns green
- [ ] Other shed buttons are blue
- [ ] Tapping shed button loads machines
- [ ] Machine buttons display short_name correctly (no "MC" prefix)
- [ ] Selected machine button turns green
- [ ] Meter displays as integer (no decimal)
- [ ] Opening meter shows as integer
- [ ] Closing meter accepts only integers
- [ ] Hours field auto-fills when spell is selected
- [ ] Unit calculates correctly (closing - opening)
- [ ] Unit displays as integer
- [ ] Efficiency calculates correctly with integer meters
- [ ] Save button validates shed selection
- [ ] Save works with integer values
- [ ] Summary displays correctly
- [ ] Clear form resets shed button colors

### Edge Cases
- [ ] No sheds available - shows status message
- [ ] No machines for shed - shows status message
- [ ] Empty opening meter defaults to 0
- [ ] First entry of day - opening meter is 0
- [ ] Subsequent entries - opening meter auto-fills from previous closing
- [ ] Zero hours - efficiency shows 0.0
- [ ] Spell without working_hours - defaults to 8.0

---

## Summary of Changes

| Component | Change Type | Description |
|-----------|-------------|-------------|
| DoffResponse.kt | Data Model | Added working_hours to Spell |
| activity_drawing_meter_entry.xml | Layout | Replaced shed dropdown with button grid |
| activity_drawing_meter_entry.xml | Layout | Changed meter inputs from decimal to integer |
| DrawingMeterEntryActivity.kt | Variables | Added llShedButtons, tvShedStatus, selectedShed |
| DrawingMeterEntryActivity.kt | onCreate | Initialize shed button views |
| DrawingMeterEntryActivity.kt | Spell Listener | Auto-fill hours from spell.workingHours |
| DrawingMeterEntryActivity.kt | Shed Logic | Removed spinner listener, added button logic |
| DrawingMeterEntryActivity.kt | Opening Listener | Added text watcher for opening meter |
| DrawingMeterEntryActivity.kt | loadSpells | Include working_hours in Spell creation |
| DrawingMeterEntryActivity.kt | loadSheds | Call renderShedButtons instead of adapter |
| DrawingMeterEntryActivity.kt | renderShedButtons | NEW METHOD - Create shed buttons |
| DrawingMeterEntryActivity.kt | selectShed | NEW METHOD - Handle shed button click |
| DrawingMeterEntryActivity.kt | renderMachineButtons | Use short_name only (no "MC" prefix) |
| DrawingMeterEntryActivity.kt | selectMachine | Display meter as integer |
| DrawingMeterEntryActivity.kt | loadOpeningMeter | Parse and display as integer |
| DrawingMeterEntryActivity.kt | calculateUnitAndEff | Use integer parsing for meters |
| DrawingMeterEntryActivity.kt | saveEntry | Use selectedShed, parse integers |
| DrawingMeterEntryActivity.kt | clearForm | Reset shed button colors |
| app.py | Backend | Return working_hours from /shifts endpoint |

---

## Expected User Experience

### Before Changes
```
Date: [06-05-2026▼]  Spell: [Spell A▼]

Shed: [-- Select Shed --▼]

Machine: [D1] [D2] [D3]

Meter: 1500.00 | Opening: [500.00]
Closing: [700.00] Unit: 200.00
Hours: [8.0] Eff%: 16.67%
```

### After Changes
```
Date: [06-05-2026▼]  Spell: [Spell A▼]

Shed: [Shed A] [Shed B] [Shed C]
         ↑ green   blue    blue

Machine: [D1] [D2] [D3]
          ↑ green

Meter: 1500 | Opening: [500]
Closing: [700] Unit: 200
Hours: [8] Eff%: 16.67%
         ↑ auto-filled from spell
```

---

## Notes

- All meter values now integer (no decimals)
- Efficiency still shows with 2 decimal places (percentage)
- Shed buttons width: 80dp (wider than machine buttons at 52dp)
- Machine name fix: Use `mc.mcShortName` directly without "MC" prefix fallback
- Hours auto-fill from spell_mst.working_hours on spell selection
- Backend must return working_hours field in /shifts response
- If working_hours is NULL in database, defaults to 8.0
- Shed and machine buttons use same color scheme (blue/green)
- All calculations remain accurate with integer meters

---

## Files Modified

1. `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\api\DoffResponse.kt`
2. `e:\sjm\MyHrms\app\src\main\res\layout\activity_drawing_meter_entry.xml`
3. `e:\sjm\MyHrms\app\src\main\java\com\example\myhrms\DrawingMeterEntryActivity.kt`
4. `e:\sjm\attendancesystem\app.py` (or backend shifts endpoint file)

---

## Implementation Priority

1. **HIGH**: Update backend /shifts endpoint (needed for app to work)
2. **HIGH**: Update DoffResponse.kt (data model)
3. **HIGH**: Update activity_drawing_meter_entry.xml (UI)
4. **HIGH**: Update DrawingMeterEntryActivity.kt (logic)
5. **MEDIUM**: Test all functionality
6. **LOW**: Update documentation

---

## Rollback Plan

If issues occur:
1. Revert DrawingMeterEntryActivity.kt to previous version
2. Revert activity_drawing_meter_entry.xml to previous version
3. Revert DoffResponse.kt to previous version
4. Keep backend change (working_hours won't break anything if not used)

---

**Plan Created:** May 6, 2026  
**Feature:** Drawing Meter Entry Updates  
**Status:** Ready for Implementation

