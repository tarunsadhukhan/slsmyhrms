# Copy Drawing Module to Actual Backend
# Run this script to copy the drawing module from reference to actual backend

Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "Drawing Module - Backend Setup" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

$SourcePath = "E:\sjm\MyHrms\src\drawing"
$DestPath = "e:\sjm\attendancesystem\src\drawing"

Write-Host "Source: $SourcePath" -ForegroundColor Yellow
Write-Host "Destination: $DestPath`n" -ForegroundColor Yellow

# Check if source exists
if (-not (Test-Path $SourcePath)) {
    Write-Host "ERROR: Source path not found!" -ForegroundColor Red
    Write-Host "Please ensure E:\sjm\MyHrms\src\drawing exists" -ForegroundColor Red
    exit 1
}

# Check if destination backend exists
if (-not (Test-Path "e:\sjm\attendancesystem")) {
    Write-Host "ERROR: Backend path not found!" -ForegroundColor Red
    Write-Host "Please ensure e:\sjm\attendancesystem exists" -ForegroundColor Red
    exit 1
}

# Create src folder if not exists
$SrcFolder = "e:\sjm\attendancesystem\src"
if (-not (Test-Path $SrcFolder)) {
    Write-Host "Creating src folder..." -ForegroundColor Yellow
    New-Item -Path $SrcFolder -ItemType Directory -Force | Out-Null
    Write-Host "Created: $SrcFolder" -ForegroundColor Green
}

# Create __init__.py in src if not exists
$SrcInit = "$SrcFolder\__init__.py"
if (-not (Test-Path $SrcInit)) {
    Write-Host "Creating src\__init__.py..." -ForegroundColor Yellow
    "" | Out-File -FilePath $SrcInit -Encoding UTF8
    Write-Host "Created: $SrcInit" -ForegroundColor Green
}

# Create drawing folder
if (-not (Test-Path $DestPath)) {
    Write-Host "Creating drawing folder..." -ForegroundColor Yellow
    New-Item -Path $DestPath -ItemType Directory -Force | Out-Null
    Write-Host "Created: $DestPath" -ForegroundColor Green
}

# Copy files
Write-Host "`nCopying files..." -ForegroundColor Yellow

$FilesToCopy = @("__init__.py", "routes.py")

foreach ($File in $FilesToCopy) {
    $SourceFile = Join-Path $SourcePath $File
    $DestFile = Join-Path $DestPath $File

    if (Test-Path $SourceFile) {
        Copy-Item -Path $SourceFile -Destination $DestFile -Force
        Write-Host "  Copied: $File" -ForegroundColor Green
    } else {
        Write-Host "  Not found: $File" -ForegroundColor Red
    }
}

Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "Files copied successfully!" -ForegroundColor Green
Write-Host "================================`n" -ForegroundColor Cyan

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Create database.py in e:\sjm\attendancesystem\src\" -ForegroundColor White
Write-Host "2. Register blueprint in e:\sjm\attendancesystem\app.py" -ForegroundColor White
Write-Host "3. Create database tables" -ForegroundColor White
Write-Host "4. Start backend server and test endpoints`n" -ForegroundColor White

Write-Host "See BACKEND_INTEGRATION_REFERENCE.md for detailed instructions" -ForegroundColor Cyan

