-- Verify and Update working_hours in spell_mst table
-- Date: May 6, 2026

-- 1. Check current working_hours data
SELECT
    spell_id,
    spell_name,
    starting_time,
    end_time,
    working_hours,
    CASE
        WHEN working_hours IS NULL THEN 'NULL - Will use default 8.0'
        ELSE 'OK'
    END AS status
FROM spell_mst
ORDER BY spell_name;

-- 2. Count spells with NULL working_hours
SELECT
    COUNT(*) AS total_spells,
    SUM(CASE WHEN working_hours IS NULL THEN 1 ELSE 0 END) AS null_hours,
    SUM(CASE WHEN working_hours IS NOT NULL THEN 1 ELSE 0 END) AS has_hours
FROM spell_mst;

-- 3. OPTIONAL: Update NULL working_hours to 8.0 (default)
-- Uncomment the line below if you want to set default hours for all NULL spells
-- UPDATE spell_mst SET working_hours = 8.0 WHERE working_hours IS NULL;

-- 4. OPTIONAL: Set specific hours for specific spells
-- Examples (adjust as needed):
-- UPDATE spell_mst SET working_hours = 8.0 WHERE spell_name = 'A1';
-- UPDATE spell_mst SET working_hours = 12.0 WHERE spell_name = 'B1';
-- UPDATE spell_mst SET working_hours = 6.0 WHERE spell_name = 'C';

-- 5. Verify the updates
-- SELECT spell_name, working_hours FROM spell_mst ORDER BY spell_name;

