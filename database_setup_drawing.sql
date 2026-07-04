-- ================================================================
-- Drawing Meter Entry - Database Setup Script
-- ================================================================
-- Database: sjm
-- Host: 13.126.47.172
-- Run this script to create required tables for Drawing Meter Entry
-- ================================================================

USE sjm;

-- ================================================================
-- 1. CREATE MASTER TABLE: tbl_drawing_mst
-- ================================================================

CREATE TABLE IF NOT EXISTS tbl_drawing_mst (
    mc_id INT PRIMARY KEY AUTO_INCREMENT,
    mc_short_name VARCHAR(50) NOT NULL COMMENT 'Machine short name (e.g., D1, D2)',
    shed_type VARCHAR(50) NOT NULL COMMENT 'Shed type (e.g., Shed A, Shed B)',
    cont_meter DECIMAL(10,2) DEFAULT 0 COMMENT 'Continuous meter value',
    branch_id INT COMMENT 'Branch ID',
    active TINYINT DEFAULT 1 COMMENT '1=Active, 0=Inactive',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_shed_type (shed_type),
    INDEX idx_branch (branch_id),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Drawing Machine Master Table';

-- ================================================================
-- 2. CREATE TRANSACTION TABLE: tbl_daily_drawing
-- ================================================================

CREATE TABLE IF NOT EXISTS tbl_daily_drawing (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL COMMENT 'Entry date',
    spell_id INT NOT NULL COMMENT 'Shift/Spell ID',
    shed_type VARCHAR(50) COMMENT 'Shed type',
    mc_id INT NOT NULL COMMENT 'Machine ID from tbl_drawing_mst',
    opening_meter DECIMAL(10,2) DEFAULT 0 COMMENT 'Opening meter reading',
    closing_meter DECIMAL(10,2) DEFAULT 0 COMMENT 'Closing meter reading',
    unit DECIMAL(10,2) DEFAULT 0 COMMENT 'Calculated: closing - opening',
    hours DECIMAL(5,2) DEFAULT 0 COMMENT 'Working hours',
    eff DECIMAL(5,2) DEFAULT 0 COMMENT 'Efficiency percentage',
    branch_id INT COMMENT 'Branch ID',
    user_id INT COMMENT 'User who created entry',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_entry (date, spell_id, mc_id) COMMENT 'One entry per date+spell+machine',
    FOREIGN KEY (mc_id) REFERENCES tbl_drawing_mst(mc_id) ON DELETE RESTRICT,
    INDEX idx_date_spell (date, spell_id),
    INDEX idx_branch (branch_id),
    INDEX idx_mc_date (mc_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily Drawing Meter Entry Table';

-- ================================================================
-- 3. INSERT SAMPLE DATA (OPTIONAL - for testing)
-- ================================================================

-- Sample Machines for Shed A
INSERT IGNORE INTO tbl_drawing_mst (mc_short_name, shed_type, cont_meter, branch_id, active) VALUES
('D1', 'Shed A', 1000.00, 29, 1),
('D2', 'Shed A', 1200.00, 29, 1),
('D3', 'Shed A', 1500.00, 29, 1);

-- Sample Machines for Shed B
INSERT IGNORE INTO tbl_drawing_mst (mc_short_name, shed_type, cont_meter, branch_id, active) VALUES
('D4', 'Shed B', 1800.00, 29, 1),
('D5', 'Shed B', 2000.00, 29, 1),
('D6', 'Shed B', 2200.00, 29, 1);

-- Sample Machines for Shed C
INSERT IGNORE INTO tbl_drawing_mst (mc_short_name, shed_type, cont_meter, branch_id, active) VALUES
('D7', 'Shed C', 2500.00, 29, 1),
('D8', 'Shed C', 2800.00, 29, 1);

-- ================================================================
-- 4. VERIFY TABLES
-- ================================================================

-- Show table structure
DESCRIBE tbl_drawing_mst;
DESCRIBE tbl_daily_drawing;

-- Show machine count by shed
SELECT shed_type, COUNT(*) as machine_count
FROM tbl_drawing_mst
WHERE active = 1
GROUP BY shed_type;

-- Show total machines
SELECT COUNT(*) as total_machines
FROM tbl_drawing_mst
WHERE active = 1;

-- ================================================================
-- 5. USEFUL QUERIES FOR TESTING
-- ================================================================

-- Get all active machines
-- SELECT * FROM tbl_drawing_mst WHERE active = 1 ORDER BY shed_type, mc_short_name;

-- Get unique shed types
-- SELECT DISTINCT shed_type FROM tbl_drawing_mst WHERE active = 1 ORDER BY shed_type;

-- Get machines for specific shed
-- SELECT mc_id, mc_short_name, cont_meter FROM tbl_drawing_mst WHERE shed_type = 'Shed A' AND active = 1;

-- Get all entries for a date
-- SELECT * FROM tbl_daily_drawing WHERE date = '2026-05-06';

-- Get summary with machine names
-- SELECT
--     d.date,
--     d.spell_id,
--     m.mc_short_name,
--     d.opening_meter,
--     d.closing_meter,
--     d.unit,
--     d.hours,
--     d.eff
-- FROM tbl_daily_drawing d
-- JOIN tbl_drawing_mst m ON d.mc_id = m.mc_id
-- WHERE d.date = '2026-05-06'
-- ORDER BY m.mc_short_name;

-- ================================================================
-- NOTES:
-- ================================================================
-- 1. Efficiency Formula: ((unit / hours * 8) / const * 100)
-- 2. Unit Calculation: closing_meter - opening_meter
-- 3. Unique constraint prevents duplicate entries for same date+spell+machine
-- 4. Foreign key ensures mc_id exists in master table
-- 5. Use branch_id to filter data by branch
-- ================================================================

SELECT '✓ Database setup complete!' as status;

