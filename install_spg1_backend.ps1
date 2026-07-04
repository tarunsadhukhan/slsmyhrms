# SPG1 Quality-Shift Report Backend Installer
$backendPath = "e:\sjm\attendancesystem"
Write-Host "SPG1 Quality-Shift Report Endpoint Installer" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

if (-not (Test-Path $backendPath)) {
    Write-Host "ERROR: Backend not found at $backendPath" -ForegroundColor Red
    exit 1
}

$doffPy = Join-Path $backendPath "src\doff\doff.py"
$appPy = Join-Path $backendPath "app.py"

$target = $null
if (Test-Path $doffPy) {
    $target = $doffPy
    Write-Host "Found: $doffPy" -ForegroundColor Green
} elseif (Test-Path $appPy) {
    $target = $appPy
    Write-Host "Found: $appPy" -ForegroundColor Green
} else {
    Write-Host "ERROR: No doff.py or app.py found" -ForegroundColor Red
    exit 1
}

$content = Get-Content $target -Raw
if ($content -match "spg1-quality-shift-report") {
    Write-Host "Endpoint already exists!" -ForegroundColor Yellow
    exit 0
}

$code = @'

# SPG DOFF ENTRY 1 - QUALITY-WISE SHIFT-WISE REPORT
@doff_bp.route('/doff/spg1-quality-shift-report', methods=['GET'])
def get_spg1_quality_shift_report():
    try:
        date_str = request.args.get('date')
        branch_id = request.args.get('branch_id', type=int)
        if not date_str:
            return jsonify({'status': 'error', 'message': 'date is required'}), 400
        if not branch_id:
            return jsonify({'status': 'error', 'message': 'branch_id is required'}), 400
        db = get_db()
        cursor = db.cursor(dictionary=True)
        query = """
            SELECT
                COALESCE(q.quality_name, 'Unknown') AS quality_name,
                COALESCE(SUM(CASE WHEN s.spell_name LIKE '%A%' THEN d.net_weight ELSE 0 END), 0) AS shift_a,
                COALESCE(SUM(CASE WHEN s.spell_name LIKE '%B%' THEN d.net_weight ELSE 0 END), 0) AS shift_b,
                COALESCE(SUM(CASE WHEN s.spell_name LIKE '%C%' THEN d.net_weight ELSE 0 END), 0) AS shift_c,
                COALESCE(SUM(d.net_weight), 0) AS total
            FROM daily_doff_tbl d
            LEFT JOIN spell_mst s ON d.spell = s.spell_id
            LEFT JOIN spinning_quality_mst q ON d.quality_id = q.quality_id
            WHERE d.doff_date = %s AND d.branch_id = %s
              AND d.weight_type = 'SPG1' AND (d.active IS NULL OR d.active = 1)
            GROUP BY q.quality_name ORDER BY q.quality_name
        """
        cursor.execute(query, (date_str, branch_id))
        report_rows = cursor.fetchall()
        grand_total_a = sum(row['shift_a'] for row in report_rows)
        grand_total_b = sum(row['shift_b'] for row in report_rows)
        grand_total_c = sum(row['shift_c'] for row in report_rows)
        grand_total = sum(row['total'] for row in report_rows)
        cursor.close()
        db.close()
        return jsonify({
            'status': 'success',
            'message': 'Quality-wise shift-wise report generated',
            'report': report_rows,
            'grand_total': {'shift_a': grand_total_a, 'shift_b': grand_total_b, 'shift_c': grand_total_c, 'total': grand_total}
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500

'@

if ($target -like "*app.py") {
    $code = $code -replace '@doff_bp', '@app'
}

$backup = "$target.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Copy-Item $target $backup
Write-Host "Backup: $backup" -ForegroundColor Green

Add-Content -Path $target -Value $code
Write-Host "SUCCESS: Endpoint added!" -ForegroundColor Green
Write-Host ""
Write-Host "Next: Restart backend, then test at:" -ForegroundColor Yellow
Write-Host "http://localhost:5051/doff/spg1-quality-shift-report?date=2026-05-06&branch_id=1" -ForegroundColor Gray

