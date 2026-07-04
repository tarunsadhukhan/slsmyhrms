-- =========================================================================
-- MyHrms — Dynamic Menu & Permission Schema
-- =========================================================================
-- Works on MySQL / MariaDB. Adjust AUTO_INCREMENT / data types for PostgreSQL
-- (SERIAL / BOOLEAN) or SQL Server (IDENTITY / BIT) if needed.
--
-- Idea
-- ----
--  * roles                  : master list of roles (Admin, Manager, …)
--  * menus                  : every screen / sub-menu in the mobile app
--  * role_menu_permissions  : DEFAULT permission for a role on a menu
--  * user_menu_permissions  : OPTIONAL per-user override
--  * user_roles             : a user can have one or more roles
--
-- Effective permission for a user on a menu =
--     COALESCE(user_menu_permissions, role_menu_permissions of any of his
--     roles aggregated with OR).
--
-- Permission flags
-- ----------------
--   can_view    -> can see the menu at all
--   can_add     -> can use the (+) / "Add" button
--   can_modify  -> can edit / update
--   can_delete  -> can delete
--   can_print   -> can print / export
--   can_all     -> shortcut: implies every flag above
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1) ROLES
-- -------------------------------------------------------------------------
DROP TABLE IF EXISTS user_menu_permissions;
DROP TABLE IF EXISTS role_menu_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS menus;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    role_name    VARCHAR(50)  NOT NULL UNIQUE,
    description  VARCHAR(255) NULL,
    is_active    TINYINT(1)   NOT NULL DEFAULT 1,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------------
-- 2) MENUS  (the dynamic menu tree shown on the dashboard)
-- -------------------------------------------------------------------------
CREATE TABLE menus (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    menu_key        VARCHAR(80)  NOT NULL UNIQUE,   -- stable key used by the app
    menu_name       VARCHAR(100) NOT NULL,          -- display label
    parent_id       INT          NULL,              -- NULL = top-level
    menu_order      INT          NOT NULL DEFAULT 0,
    icon            VARCHAR(80)  NULL,              -- drawable name in /res
    activity_class  VARCHAR(120) NULL,              -- e.g. DepartmentMasterActivity
    is_group        TINYINT(1)   NOT NULL DEFAULT 0,-- 1 = header / expandable, 0 = clickable leaf
    is_active       TINYINT(1)   NOT NULL DEFAULT 1,
    FOREIGN KEY (parent_id) REFERENCES menus(id) ON DELETE CASCADE
);

CREATE INDEX idx_menus_parent ON menus(parent_id);

-- -------------------------------------------------------------------------
-- 3) ROLE  ×  MENU  PERMISSIONS  (defaults)
-- -------------------------------------------------------------------------
CREATE TABLE role_menu_permissions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    role_id     INT NOT NULL,
    menu_id     INT NOT NULL,
    can_view    TINYINT(1) NOT NULL DEFAULT 0,
    can_add     TINYINT(1) NOT NULL DEFAULT 0,
    can_modify  TINYINT(1) NOT NULL DEFAULT 0,
    can_delete  TINYINT(1) NOT NULL DEFAULT 0,
    can_print   TINYINT(1) NOT NULL DEFAULT 0,
    can_all     TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_role_menu (role_id, menu_id),
    FOREIGN KEY (role_id) REFERENCES roles(id)  ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menus(id)  ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 4) USER  ×  MENU  PERMISSIONS  (per-user override – optional)
--    If a row exists here, it WINS over the role default for that menu.
-- -------------------------------------------------------------------------
CREATE TABLE user_menu_permissions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    menu_id     INT NOT NULL,
    can_view    TINYINT(1) NOT NULL DEFAULT 0,
    can_add     TINYINT(1) NOT NULL DEFAULT 0,
    can_modify  TINYINT(1) NOT NULL DEFAULT 0,
    can_delete  TINYINT(1) NOT NULL DEFAULT 0,
    can_print   TINYINT(1) NOT NULL DEFAULT 0,
    can_all     TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_user_menu (user_id, menu_id),
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 5) USER  ×  ROLE  (a user can hold several roles)
-- -------------------------------------------------------------------------
CREATE TABLE user_roles (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id   INT NOT NULL,
    role_id   INT NOT NULL,
    UNIQUE KEY uq_user_role (user_id, role_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 6) VIEW :  effective permissions per user × menu
--    Lets the backend resolve permissions with a single SELECT.
-- -------------------------------------------------------------------------
DROP VIEW IF EXISTS v_user_effective_permissions;

CREATE VIEW v_user_effective_permissions AS
SELECT
    ur.user_id                                      AS user_id,
    m.id                                            AS menu_id,
    m.menu_key                                      AS menu_key,
    MAX(COALESCE(ump.can_view  , rmp.can_view , 0)) AS can_view,
    MAX(COALESCE(ump.can_add   , rmp.can_add  , 0)) AS can_add,
    MAX(COALESCE(ump.can_modify, rmp.can_modify,0)) AS can_modify,
    MAX(COALESCE(ump.can_delete, rmp.can_delete,0)) AS can_delete,
    MAX(COALESCE(ump.can_print , rmp.can_print, 0)) AS can_print,
    MAX(COALESCE(ump.can_all   , rmp.can_all  , 0)) AS can_all
FROM user_roles ur
JOIN menus m
JOIN role_menu_permissions rmp
        ON rmp.role_id = ur.role_id AND rmp.menu_id = m.id
LEFT JOIN user_menu_permissions ump
        ON ump.user_id = ur.user_id AND ump.menu_id = m.id
GROUP BY ur.user_id, m.id, m.menu_key;
