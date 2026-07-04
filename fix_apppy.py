"""
Script to remove dead code from app.py between line 2415 and if __name__ == '__main__':
"""
with open(r'E:\sjm\MyHrms\app.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()

first_except_end = None
main_block_idx = None

for i, line in enumerate(lines):
    if i > 2400 and first_except_end is None:
        if "        return jsonify({'status': 'error', 'message': str(e)}), 500" in line:
            first_except_end = i
    if "if __name__ == '__main__':" in line:
        main_block_idx = i

print(f"first_except_end (0-based): {first_except_end} => line {first_except_end+1 if first_except_end else None}")
print(f"main_block_idx   (0-based): {main_block_idx} => line {main_block_idx+1 if main_block_idx else None}")

if first_except_end and main_block_idx:
    keep_before = lines[:first_except_end + 1]
    run_server_comment = [
        '\n',
        '\n',
        '# ══════════════════════════════════════════════════════════════════\n',
        '# RUN SERVER\n',
        '# ══════════════════════════════════════════════════════════════════\n',
        '\n',
    ]
    keep_after = lines[main_block_idx:]

    new_lines = keep_before + run_server_comment + keep_after

    with open(r'E:\sjm\MyHrms\app.py', 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    print(f"Done! New file has {len(new_lines)} lines")
else:
    print("ERROR: Could not find markers!")
