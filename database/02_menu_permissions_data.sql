-- =========================================================================
-- MyHrms — Seed data for menus & default role permissions
-- =========================================================================
-- Run this AFTER 01_menu_permissions_schema.sql.
-- The menu_key values map 1-to-1 with the IDs already used in
-- res/layout/activity_dashboard.xml so the mobile app can look up
-- permissions by key without renaming anything.
-- =========================================================================

-- ------------------------------------------------------------------ ROLES
INSERT INTO roles (role_name, description) VALUES
    ('Admin'     , 'Full access to all modules'),
    ('Manager'   , 'View / Add / Edit, no delete, can print reports'),
    ('Supervisor', 'View and Add production / attendance entries'),
    ('Operator'  , 'Limited entry – attendance & basic production'),
    ('Viewer'    , 'Read-only across the system');

-- ------------------------------------------------------------------ MENUS
-- ===== TOP-LEVEL DASHBOARD QUICK-ACCESS CARDS =============================
-- These map 1:1 to the cardXxx views in activity_dashboard.xml.
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('card_present', 'Present (Dept-wise)', NULL, 1, 'ic_check'     , NULL                          , 0),
    ('card_jute'   , 'Jute (Quick)'       , NULL, 2, 'ic_employee'  , 'JuteReceivedActivity'        , 0),
    ('card_spg'    , 'SPG (Quick)'        , NULL, 3, 'ic_employee'  , 'SpinningDoffActivity'        , 0),
    ('card_winding', 'Winding (Quick)'    , NULL, 4, 'ic_employee'  , 'WindingEntryActivity'        , 0),
    ('card_others' , 'Others (Quick)'     , NULL, 5, 'ic_employee'  , 'OtherEntriesActivity'        , 0),
    ('card_bales'  , 'Bales (Quick)'      , NULL, 6, 'ic_employee'  , 'BalesProductionEntryActivity', 0);

-- ===== ATTENDANCE GROUP ===================================================
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_attendance', 'Attendance', NULL, 10, 'ic_attendance', NULL, 1);

SET @grp_att = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_attendance_dashboard', 'Attendance Dashboard', @grp_att, 1, 'ic_report'        , 'AttendanceDashboardActivity', 0),
    ('menu_onboarding'          , 'Face Registration'   , @grp_att, 2, 'ic_face_register' , 'OnBoardingActivity'         , 0),
    ('menu_attendance_entry'    , 'Attendance Entry'    , @grp_att, 3, 'ic_face'          , 'AttendanceActivity'         , 0),
    ('menu_attendance_reports'  , 'Attendance Reports'  , @grp_att, 4, 'ic_report'        , 'AttendanceReportsActivity'  , 0),
    ('grp_other_entries'        , 'Other Entries'       , @grp_att, 5, 'ic_masters'       , NULL                         , 1);

SET @grp_other = (SELECT id FROM menus WHERE menu_key = 'grp_other_entries');

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_leave_entries', 'Leave Entries', @grp_other, 1, 'ic_edit', 'LeaveEntryActivity', 0);

-- ===== PRODUCTION GROUP ===================================================
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_production', 'Production', NULL, 20, 'ic_masters', NULL, 1);

SET @grp_prod = LAST_INSERT_ID();

-- Jute -------------------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_jute', 'Jute', @grp_prod, 1, 'ic_masters', NULL, 1);
SET @grp_jute = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_jute_received'  , 'Jute Received'  , @grp_jute, 1, 'ic_add', 'JuteReceivedActivity'  , 0),
    ('menu_assorting_entry', 'Assorting Entry', @grp_jute, 2, 'ic_add', 'AssortingEntryActivity', 0);

-- Spreader Entry ---------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_spreader_entry', 'Spreader Entry', @grp_prod, 2, 'ic_masters', NULL, 1);
SET @grp_sprd = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_production_entry', 'Spreader Production', @grp_sprd, 1, 'ic_add', 'SpreaderProdEntryActivity' , 0),
    ('menu_issue_entry'     , 'Spreader Issue'     , @grp_sprd, 2, 'ic_add', 'SpreaderIssueEntryActivity', 0);

-- Drawing & Spinning -----------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_drawing_meter_entry', 'Drawing Meter Entry', @grp_prod, 3, 'ic_add', 'DrawingMeterEntryActivity', 0),
    ('menu_spinning_doff_entry', 'Spinning Doff'      , @grp_prod, 4, 'ic_add', 'SpinningDoffActivity'     , 0);

-- Doff Entry -------------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_doff_entry', 'Doff Entry', @grp_prod, 5, 'ic_masters', NULL, 1);
SET @grp_doff = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_spellwise_frame_entry', 'Spell-wise Frame Entry', @grp_doff, 1, 'ic_add', 'SpellWiseFrameEntryActivity', 0),
    ('menu_spg_doff_entry'       , 'Spg Doff Entry'        , @grp_doff, 2, 'ic_add', 'NewDoffEntryActivity'       , 0),
    ('menu_spg_doff_entry1'      , 'Spg Doff Entry 1'      , @grp_doff, 3, 'ic_add', 'SpgDoffEntry1Activity'      , 0),
    ('menu_spg_running_hours'    , 'SPG Running Hours'     , @grp_doff, 4, 'ic_add', 'SpgRunningHoursActivity'    , 0);

-- Winding / Weaving ------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_winding_entry', 'Winding Entry', @grp_prod, 6, 'ic_masters', NULL, 1);
SET @grp_wdg = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_winding_entry',      'Winding Entry',            @grp_wdg , 1, 'ic_add', 'WindingEntryActivity'     , 0),
    ('menu_cont_winding_entry', 'Continuous Winding Entry', @grp_wdg , 2, 'ic_add', 'ContWindingEntryActivity' , 0),
    ('menu_weaving_entry',      'Weaving Entry',            @grp_prod, 7, 'ic_add', 'MenuPlaceholderActivity'  , 0);

-- Finishing --------------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_finishing_entry', 'Finishing', @grp_prod, 8, 'ic_masters', NULL, 1);
SET @grp_fin = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_other_entries'         , 'Other Entries'          , @grp_fin, 1, 'ic_add', 'OtherEntriesActivity'         , 0),
    ('menu_bales_production_entry', 'Bales Production Entry' , @grp_fin, 2, 'ic_add', 'BalesProductionEntryActivity' , 0),
    ('menu_bales_issue_entry'     , 'Bales Issue Entry'      , @grp_fin, 3, 'ic_add', 'BalesIssueEntryActivity'      , 0);

-- Stocks -----------------------------------------------------------------
INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('grp_stocks', 'Stocks', @grp_prod, 9, 'ic_masters', NULL, 1);
SET @grp_stk = LAST_INSERT_ID();

INSERT INTO menus (menu_key, menu_name, parent_id, menu_order, icon, activity_class, is_group) VALUES
    ('menu_roll_stock'  , 'Roll Stock'   , @grp_stk , 1, 'ic_report', 'RollStockActivity'   , 0),
    ('menu_weight_entry', 'Weight Entry' , @grp_prod, 10, 'ic_add'  , 'WeightEntryActivity' , 0);

-- =========================================================================
-- DEFAULT ROLE  ×  MENU  PERMISSIONS
-- =========================================================================
-- Admin  -> can_all = 1 on every menu
INSERT INTO role_menu_permissions
       (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT  r.id,    m.id,    1,        1,       1,          1,          1,         1
FROM roles r, menus m
WHERE r.role_name = 'Admin';

-- Manager -> view/add/modify/print on every menu, no delete
INSERT INTO role_menu_permissions
       (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT  r.id,    m.id,    1,        1,       1,          0,          1,         0
FROM roles r, menus m
WHERE r.role_name = 'Manager';

-- Supervisor -> view/add only on Attendance + Production menus
INSERT INTO role_menu_permissions
       (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT  r.id,    m.id,    1,        1,       0,          0,          1,         0
FROM roles r, menus m
WHERE r.role_name = 'Supervisor'
  AND m.menu_key IN (
      'card_present','card_jute','card_spg','card_winding','card_others','card_bales',
      'grp_attendance','menu_attendance_dashboard','menu_onboarding',
      'menu_attendance_entry','menu_attendance_reports',
      'grp_other_entries','menu_leave_entries',
      'grp_production','grp_jute','menu_jute_received','menu_assorting_entry',
      'grp_spreader_entry','menu_production_entry','menu_issue_entry',
      'menu_drawing_meter_entry','menu_spinning_doff_entry',
      'grp_doff_entry','menu_spellwise_frame_entry','menu_spg_doff_entry',
      'menu_spg_doff_entry1','menu_spg_running_hours',
      'grp_winding_entry','menu_winding_entry','menu_cont_winding_entry','menu_weaving_entry',
      'grp_finishing_entry','menu_other_entries','menu_bales_production_entry',
      'menu_bales_issue_entry','grp_stocks','menu_roll_stock','menu_weight_entry'
  );

-- Operator -> only attendance + a few entries, view + add only
INSERT INTO role_menu_permissions
       (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT  r.id,    m.id,    1,        1,       0,          0,          0,         0
FROM roles r, menus m
WHERE r.role_name = 'Operator'
  AND m.menu_key IN (
      'card_present',
      'grp_attendance','menu_attendance_entry','menu_onboarding',
      'grp_production','grp_jute','menu_jute_received','menu_assorting_entry',
      'menu_drawing_meter_entry','menu_spinning_doff_entry',
      'grp_winding_entry','menu_winding_entry','menu_cont_winding_entry','menu_weight_entry'
  );

-- Viewer -> read-only + print on every menu
INSERT INTO role_menu_permissions
       (role_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
SELECT  r.id,    m.id,    1,        0,       0,          0,          1,         0
FROM roles r, menus m
WHERE r.role_name = 'Viewer';

-- =========================================================================
-- EXAMPLE :  assign roles to existing users
-- =========================================================================
-- Replace user_id values with real IDs from your `users` table.
-- INSERT INTO user_roles (user_id, role_id)
-- SELECT 1, id FROM roles WHERE role_name = 'Admin';
-- INSERT INTO user_roles (user_id, role_id)
-- SELECT 2, id FROM roles WHERE role_name = 'Manager';

-- =========================================================================
-- EXAMPLE :  per-user override (give user 2 delete rights only on Employees)
-- =========================================================================
-- INSERT INTO user_menu_permissions
--        (user_id, menu_id, can_view, can_add, can_modify, can_delete, can_print, can_all)
-- SELECT  2, id, 1, 1, 1, 1, 1, 0
-- FROM   menus WHERE menu_key = 'card_employees';
