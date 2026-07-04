# 📱 DRAWING METER ENTRY - VISUAL UI GUIDE

## What Users Will See on Their Phone

---

## 🎨 SCREEN 1: MAIN ENTRY FORM

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ ← Drawing Meter Entry              ⋮   ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
┃                                           ┃
┃ ╔═══════════════════════════════════════╗ ┃
┃ ║ Drawing Meter Entry                   ║ ┃
┃ ║                                       ║ ┃
┃ ║ Date                Spell      Shed   ║ ┃
┃ ║ ┌────────────┐ ┌─────────┐ ┌───────┐ ║ ┃
┃ ║ │📅06-05-2026│ │ Spell A▼│ │Shed A▼│ ║ ┃
┃ ║ └────────────┘ └─────────┘ └───────┘ ║ ┃
┃ ║                                       ║ ┃
┃ ║ Select Machine:                       ║ ┃
┃ ║ ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐       ║ ┃
┃ ║ │D1 │ │D2 │ │D3 │ │D4 │ │D5 │       ║ ┃
┃ ║ └───┘ └───┘ └───┘ └───┘ └───┘       ║ ┃
┃ ║  Blue   Blue  Green Blue  Blue       ║ ┃
┃ ║        (D3 is selected)               ║ ┃
┃ ║                                       ║ ┃
┃ ║ Meter              Opening            ║ ┃
┃ ║ ┌───────────┐     ┌───────────┐      ║ ┃
┃ ║ │ 1500.00   │     │ 500.00    │      ║ ┃
┃ ║ └───────────┘     └───────────┘      ║ ┃
┃ ║  (read-only)      (auto-filled)      ║ ┃
┃ ║                                       ║ ┃
┃ ║ Closing   Unit     Hours   Eff%      ║ ┃
┃ ║ ┌─────┐ ┌─────┐  ┌─────┐ ┌──────┐   ║ ┃
┃ ║ │700.00│ │200.00│ │ 8.0 │ │16.67%│   ║ ┃
┃ ║ └─────┘ └─────┘  └─────┘ └──────┘   ║ ┃
┃ ║  (input) (calc)   (input)  (calc)    ║ ┃
┃ ║                                       ║ ┃
┃ ║        ┌─────────────────┐            ║ ┃
┃ ║        │   💾 SAVE       │            ║ ┃
┃ ║        └─────────────────┘            ║ ┃
┃ ╚═══════════════════════════════════════╝ ┃
┃                                           ┃
┃ ╔═══════════════════════════════════════╗ ┃
┃ ║ Summary                               ║ ┃
┃ ║ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ║ ┃
┃ ║ Machine │ Unit    │ Eff%              ║ ┃
┃ ║ ────────┼─────────┼──────             ║ ┃
┃ ║ D1      │ 100.50  │ 85.50%            ║ ┃
┃ ║ D2      │ 120.75  │ 90.25%            ║ ┃
┃ ║ D3      │ 200.00  │ 16.67%            ║ ┃
┃ ╚═══════════════════════════════════════╝ ┃
┃                                           ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## 🎯 KEY UI ELEMENTS

### **1. Top Bar**
- **Back Arrow (←)** - Return to dashboard
- **Title** - "Drawing Meter Entry"
- **Menu (⋮)** - Additional options (if implemented)

### **2. Entry Card (White background)**

#### **Row 1: Date, Spell, Shed**
- **📅 Date Picker** - Tap to open calendar
  - Default: Today's date
  - Format: DD-MM-YYYY (06-05-2026)
- **Spell Dropdown** - Select shift
  - Options: Spell A, Spell B, Spell C, General
  - Loaded from backend API
- **Shed Dropdown** - Select shed type
  - Options: "-- Select Shed --", Shed A, Shed B, Shed C
  - Loaded from backend API

#### **Row 2: Machine Buttons**
- **Dynamic Button Grid**
  - Shows after selecting shed
  - Each button: Machine short name (D1, D2, D3...)
  - **Blue buttons** - Available machines
  - **Green button** - Currently selected machine
  - Tap to select machine

#### **Row 3: Meter & Opening**
- **Meter (Read-only)**
  - Gray background
  - Shows machine's constant meter value
  - Format: 1500.00
- **Opening (Auto-filled)**
  - Light gray background
  - Auto-filled from previous entry's closing
  - Can be manually edited if needed
  - Format: 500.00

#### **Row 4: Closing, Unit, Hours, Eff%**
- **Closing (Input)**
  - White background
  - User enters current meter reading
  - Format: Decimal number
- **Unit (Calculated - Blue)**
  - Blue text, gray background
  - Auto-calculated: Closing - Opening
  - Updates in real-time as user types
- **Hours (Input)**
  - White background
  - User enters working hours
  - Default: 8.0
- **Eff% (Calculated - Green)**
  - Green text, gray background
  - Auto-calculated: ((Unit/Hours×8)/100×100)%
  - Updates in real-time

#### **Save Button**
- **Blue background**
- **White text**: "SAVE"
- Validates form before saving
- Shows success/error toast messages

### **3. Summary Card (White background)**

#### **Header Row (Blue background)**
- **Machine** - Machine name column
- **Unit** - Production unit column
- **Eff%** - Efficiency percentage column

#### **Data Rows**
- Shows all saved entries for selected date+spell
- Each row: Machine name, Unit produced, Efficiency %
- Updates automatically after save
- Empty state: "No entries for this date and spell"

---

## 🎨 COLOR SCHEME

```
┌─────────────────────────────────────────────┐
│ COLORS USED IN UI                           │
├─────────────────────────────────────────────┤
│ Background:       #F2F2F2 (Light Gray)      │
│ Card Background:  #FFFFFF (White)           │
│ Toolbar:          #1E1E1E (Dark Gray)       │
│ Primary Blue:     #1565C0                   │
│ Success Green:    #2E7D32                   │
│ Input Background: #F5F5F5 (Light Gray)      │
│ Text Primary:     #000000 (Black)           │
│ Text Secondary:   #777777 (Gray)            │
│ Header Blue:      #E3F2FD (Light Blue)      │
│ Selected Machine: #2E7D32 (Green)           │
│ Normal Machine:   #1565C0 (Blue)            │
└─────────────────────────────────────────────┘
```

---

## 📱 RESPONSIVE DESIGN

### **Portrait Mode (Recommended)**
```
All fields visible
Grid layout: 2-4 columns
Machine buttons: Multiple rows
Summary: Full width table
```

### **Landscape Mode**
```
Horizontal scroll enabled
More machine buttons per row
Summary beside form (optional)
```

---

## 🔄 USER INTERACTIONS

### **Date Selection**
```
Tap 📅 button → Calendar popup → Select date → Date updates
```

### **Spell Selection**
```
Tap dropdown → List appears → Select spell → Summary refreshes
```

### **Shed Selection**
```
Tap dropdown → List appears → Select shed → Machines load
```

### **Machine Selection**
```
Tap machine button → Button turns GREEN → Meter shows → Opening fetches
```

### **Entering Data**
```
Type in Closing → Unit calculates instantly
Type in Hours → Efficiency calculates instantly
```

### **Saving Entry**
```
Tap SAVE → Validation → API call → Toast message → Summary updates → Form clears
```

---

## 💡 USER EXPERIENCE HIGHLIGHTS

### **Auto-Calculations**
```
✅ Unit = Closing - Opening (instant)
✅ Eff% = ((Unit/Hours×8)/100×100)% (instant)
✅ No need to use calculator
```

### **Smart Opening Meter**
```
✅ Auto-fetches previous entry's closing
✅ First entry of day: Shows 0.00
✅ Reduces data entry errors
```

### **Visual Feedback**
```
✅ Selected machine: GREEN button
✅ Normal machines: BLUE buttons
✅ Calculated fields: Colored text
✅ Loading: Progress spinner
✅ Empty: "No entries" message
```

### **Validation Messages**
```
⚠️ "Please select a spell"
⚠️ "Please select a shed"
⚠️ "Please select a machine"
⚠️ "Please enter closing meter"
✅ "Entry saved successfully"
❌ "Failed to save: [error]"
```

---

## 📊 SUMMARY TABLE EXAMPLE

```
╔════════════════════════════════════════╗
║ Summary                                ║
╠════════════════════════════════════════╣
║ Machine │ Unit    │ Eff%               ║
║─────────┼─────────┼──────────────────  ║
║ D1      │ 100.50  │ 85.50%             ║
║ D2      │ 120.75  │ 90.25%             ║
║ D3      │ 200.00  │ 16.67%             ║
║ D4      │  95.25  │ 78.40%             ║
║ D5      │ 150.00  │ 92.30%             ║
╚════════════════════════════════════════╝

• Shows all entries for selected date+spell
• Updates in real-time after save
• Sorted by entry order
• Scrollable if many entries
```

---

## 🎓 EXAMPLE USER SESSION

### **Scenario: User enters production data for D3 machine**

1. **Open App** → Login → Production Dashboard
2. **Tap** "Drawing Meter Entry"
3. **Select Date**: 06-05-2026 (today, already selected)
4. **Select Spell**: Spell A
5. **Select Shed**: Shed B
6. **Machine buttons appear**: [D1] [D2] [D3] [D4] [D5]
7. **Tap D3** → Button turns GREEN
8. **See**:
   - Meter: 1500.00 (machine constant)
   - Opening: 500.00 (auto-filled from yesterday)
9. **Enter Closing**: 700.00
10. **See Unit**: 200.00 (auto-calculated)
11. **Enter Hours**: 8.0
12. **See Eff%**: 16.67% (auto-calculated)
13. **Tap SAVE**
14. **Toast**: "Entry saved successfully"
15. **Summary updates**: Shows D3 with 200.00 unit, 16.67% efficiency
16. **Form clears**: Ready for next entry

---

## 🎨 ANIMATION & TRANSITIONS

### **Smooth Transitions**
```
✅ Button color change: Instant
✅ Dropdown open/close: Smooth slide
✅ Calendar popup: Fade in
✅ Summary update: Fade & slide
✅ Loading spinner: Rotate
✅ Toast message: Slide up from bottom
```

### **User Feedback**
```
✅ Button press: Ripple effect
✅ Input focus: Border highlight
✅ Save button: Disable during save
✅ Loading: Gray overlay + spinner
```

---

## 📐 LAYOUT MEASUREMENTS

### **Button Sizes**
```
Machine Button:   52dp × 36dp
Save Button:      Match parent × 48dp
Dropdown:         Match parent × 44dp
Input Field:      Match parent × 44dp
```

### **Text Sizes**
```
Title:           18sp (Bold)
Label:           12sp (Bold)
Input Text:      14sp
Button Text:     12sp (Bold)
Summary Text:    14sp
```

### **Spacing**
```
Card Padding:    14dp
Field Spacing:   8dp
Card Margin:     10dp
Button Gap:      6dp
```

---

## ✅ ACCESSIBILITY FEATURES

```
✅ Large touch targets (44dp minimum)
✅ High contrast colors
✅ Clear labels for all inputs
✅ Content descriptions for icons
✅ Toast messages for feedback
✅ Error messages in red
✅ Success messages in green
✅ Read-only fields clearly distinguished
```

---

## 🎉 FINAL UI IMPRESSION

**Users will experience:**
- ✨ Clean, modern interface
- ✨ Intuitive form flow
- ✨ Minimal data entry (auto-calculations)
- ✨ Instant visual feedback
- ✨ Clear validation messages
- ✨ Real-time summary updates
- ✨ Professional look and feel

**Users will say:**
- "Easy to use!"
- "Fast data entry"
- "No calculations needed"
- "Clear and simple"
- "Looks professional"

---

**Created:** May 6, 2026  
**Feature:** Drawing Meter Entry  
**Platform:** Android  
**Status:** Production Ready ✅

