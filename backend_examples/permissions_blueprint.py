"""
permissions_blueprint.py
========================
Drop this file into your backend (next to your other blueprints) and register
it on your Flask app:

    from permissions_blueprint import permissions_bp
    app.register_blueprint(permissions_bp)

It exposes two endpoints that the Android app calls:

    GET  /menu-permissions?user_id=<id>
        -> returns the full menu tree the user is allowed to SEE,
           together with action flags for each leaf menu.

    GET  /menus            (optional / admin only)
        -> returns the full menu master so an admin screen can
           pick a menu to edit permissions for.

The shape of the JSON is exactly what `PermissionManager` on the
Android side expects.
"""

from flask import Blueprint, jsonify, request
import pymysql                          # or your existing DB helper

permissions_bp = Blueprint("permissions", __name__)


# -------------------------------------------------------------------------
# Helper – open a DB connection.  Replace with your existing pattern.
# -------------------------------------------------------------------------
def _db():
    return pymysql.connect(
        host="localhost", user="root", password="", database="myhrms",
        cursorclass=pymysql.cursors.DictCursor,
    )


# =========================================================================
# GET /menu-permissions?user_id=<id>
# =========================================================================
@permissions_bp.route("/menu-permissions", methods=["GET"])
def menu_permissions():
    user_id = request.args.get("user_id", type=int)
    if not user_id:
        return jsonify(status="error", message="user_id is required"), 400

    sql = """
        SELECT  m.id            AS menu_id,
                m.menu_key,
                m.menu_name,
                m.parent_id,
                m.menu_order,
                m.icon,
                m.activity_class,
                m.is_group,
                COALESCE(p.can_view  , 0) AS can_view,
                COALESCE(p.can_add   , 0) AS can_add,
                COALESCE(p.can_modify, 0) AS can_modify,
                COALESCE(p.can_delete, 0) AS can_delete,
                COALESCE(p.can_print , 0) AS can_print,
                COALESCE(p.can_all   , 0) AS can_all
        FROM    menus m
        LEFT JOIN v_user_effective_permissions p
               ON p.menu_id = m.id AND p.user_id = %s
        WHERE   m.is_active = 1
        ORDER BY m.parent_id IS NULL DESC, m.parent_id, m.menu_order
    """

    with _db() as cn, cn.cursor() as cur:
        cur.execute(sql, (user_id,))
        rows = cur.fetchall()

    # Promote can_all -> sets every other flag (matches the schema contract)
    for r in rows:
        if r["can_all"]:
            r["can_view"]   = 1
            r["can_add"]    = 1
            r["can_modify"] = 1
            r["can_delete"] = 1
            r["can_print"]  = 1

    # Keep only menus the user can VIEW.  This is what the Android app needs.
    visible = [r for r in rows if r["can_view"] == 1]

    return jsonify(status="success", menus=visible)


# =========================================================================
# GET /menus     (admin-only – full master, no permission filter)
# =========================================================================
@permissions_bp.route("/menus", methods=["GET"])
def menus_master():
    with _db() as cn, cn.cursor() as cur:
        cur.execute("SELECT * FROM menus WHERE is_active = 1 "
                    "ORDER BY parent_id IS NULL DESC, parent_id, menu_order")
        return jsonify(status="success", menus=cur.fetchall())


# =========================================================================
# POST /role-menu-permissions   (admin-only – set permissions for a role)
#   Body: { role_id, menu_id, can_view, can_add, can_modify,
#           can_delete, can_print, can_all }
# =========================================================================
@permissions_bp.route("/role-menu-permissions", methods=["POST"])
def upsert_role_menu_permission():
    body = request.get_json(force=True) or {}
    required = ("role_id", "menu_id")
    if not all(k in body for k in required):
        return jsonify(status="error",
                       message="role_id and menu_id are required"), 400

    flags = {k: int(body.get(k, 0)) for k in
             ("can_view", "can_add", "can_modify",
              "can_delete", "can_print", "can_all")}

    sql = """
        INSERT INTO role_menu_permissions
                (role_id, menu_id, can_view, can_add,
                 can_modify, can_delete, can_print, can_all)
        VALUES  (%(role_id)s, %(menu_id)s, %(can_view)s, %(can_add)s,
                 %(can_modify)s, %(can_delete)s, %(can_print)s, %(can_all)s)
        ON DUPLICATE KEY UPDATE
                can_view   = VALUES(can_view),
                can_add    = VALUES(can_add),
                can_modify = VALUES(can_modify),
                can_delete = VALUES(can_delete),
                can_print  = VALUES(can_print),
                can_all    = VALUES(can_all)
    """
    with _db() as cn, cn.cursor() as cur:
        cur.execute(sql, {**body, **flags})
        cn.commit()
    return jsonify(status="success")


# =========================================================================
# POST /user-menu-permissions   (admin-only – per-user override)
# =========================================================================
@permissions_bp.route("/user-menu-permissions", methods=["POST"])
def upsert_user_menu_permission():
    body = request.get_json(force=True) or {}
    required = ("user_id", "menu_id")
    if not all(k in body for k in required):
        return jsonify(status="error",
                       message="user_id and menu_id are required"), 400

    flags = {k: int(body.get(k, 0)) for k in
             ("can_view", "can_add", "can_modify",
              "can_delete", "can_print", "can_all")}

    sql = """
        INSERT INTO user_menu_permissions
                (user_id, menu_id, can_view, can_add,
                 can_modify, can_delete, can_print, can_all)
        VALUES  (%(user_id)s, %(menu_id)s, %(can_view)s, %(can_add)s,
                 %(can_modify)s, %(can_delete)s, %(can_print)s, %(can_all)s)
        ON DUPLICATE KEY UPDATE
                can_view   = VALUES(can_view),
                can_add    = VALUES(can_add),
                can_modify = VALUES(can_modify),
                can_delete = VALUES(can_delete),
                can_print  = VALUES(can_print),
                can_all    = VALUES(can_all)
    """
    with _db() as cn, cn.cursor() as cur:
        cur.execute(sql, {**body, **flags})
        cn.commit()
    return jsonify(status="success")
