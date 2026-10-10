# =============================================================================
# EMS Agent - Production MSI Build Script
# Version: 1.2.4
# =============================================================================
#
# Prerequisites (all already installed on this machine):
#   - JDK 17  at: C:\Program Files\Java\jdk-17
#   - WiX 3.14 at: C:\Program Files (x86)\WiX Toolset v3.14\bin
#   - Maven on PATH
#
# Output:
#   packaging\output\EMS Agent-1.2.4.msi
#
# =============================================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------------------------

$SCRIPT_DIR   = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent $SCRIPT_DIR

$APP_NAME        = "EMS Agent"
$APP_VERSION     = "1.2.4"
$APP_VENDOR      = "Employee Monitoring Solutions"
$APP_DESCRIPTION = "Employee Monitoring Agent - Track work hours, attendance and productivity"
$APP_COPYRIGHT   = "Copyright (c) 2024 Employee Monitoring Solutions. All rights reserved."

# Fixed UUID -- must NEVER change across releases. Windows uses this to detect
# an existing installation and upgrade it (instead of installing side-by-side).
$UPGRADE_UUID    = "8B3D4A29-F5E7-4C1B-A2D9-6E8F0B1C2D3E"

$JDK17_HOME    = "C:\Program Files\Java\jdk-17"
$JPACKAGE      = "$JDK17_HOME\bin\jpackage.exe"
$WIX_BIN       = "C:\Program Files (x86)\WiX Toolset v3.14\bin"

$JAR_NAME      = "employee-agent-$APP_VERSION.jar"
$PNG_ICON      = "$PROJECT_ROOT\packaging\installer\icon\Ems-Agent.png"
$ICO_ICON      = "$PROJECT_ROOT\packaging\installer\icon\Ems-Agent.ico"
$OUTPUT_DIR    = "$PROJECT_ROOT\packaging\output"
$TARGET_DIR    = "$PROJECT_ROOT\target"

# ---------------------------------------------------------------------------
# BANNER
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  EMS Agent  |  Production MSI Build  |  v$APP_VERSION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# ---------------------------------------------------------------------------
# STEP 0: VALIDATE TOOLS
# ---------------------------------------------------------------------------

Write-Host "[0/5] Validating tools..." -ForegroundColor Yellow

if (-not (Test-Path $JPACKAGE)) {
    Write-Error "jpackage not found at: $JPACKAGE"
    exit 1
}

if (-not (Test-Path "$WIX_BIN\candle.exe")) {
    Write-Error "WiX candle.exe not found at: $WIX_BIN"
    exit 1
}

if (-not (Test-Path $PNG_ICON)) {
    Write-Error "App icon PNG not found at: $PNG_ICON"
    exit 1
}

# Add WiX to PATH so jpackage can find candle.exe / light.exe
$env:PATH = "$WIX_BIN;$env:PATH"

# Set JAVA_HOME to JDK 17 for Maven
$env:JAVA_HOME = $JDK17_HOME
$env:PATH      = "$JDK17_HOME\bin;$env:PATH"

$jpackageVersion = & $JPACKAGE --version
Write-Host "  jpackage : $jpackageVersion (JDK 17)" -ForegroundColor Green
Write-Host "  WiX      : v3.14 (candle + light)" -ForegroundColor Green
Write-Host ""

# ---------------------------------------------------------------------------
# STEP 1: CREATE MULTI-SIZE .ICO FROM PNG
# ---------------------------------------------------------------------------

Write-Host "[1/5] Converting PNG -> ICO (multi-size)..." -ForegroundColor Yellow

function ConvertTo-MultiSizeIco {
    param(
        [string]$PngPath,
        [string]$IcoPath
    )

    Add-Type -AssemblyName System.Drawing

    $sizes   = @(16, 32, 48, 64, 128, 256)
    $imgData = @()

    $src = [System.Drawing.Image]::FromFile($PngPath)

    foreach ($sz in $sizes) {
        $bmp = New-Object System.Drawing.Bitmap($sz, $sz,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.DrawImage($src, 0, 0, $sz, $sz)
        $g.Dispose()

        $ms = New-Object System.IO.MemoryStream
        $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $imgData += , ($ms.ToArray())
        $ms.Dispose()
        $bmp.Dispose()
    }
    $src.Dispose()

    # Write ICO binary (ICONDIR + ICONDIRENTRY[] + PNG blobs)
    $fs     = [System.IO.File]::OpenWrite($IcoPath)
    $writer = New-Object System.IO.BinaryWriter($fs)

    # ICONDIR header
    $writer.Write([uint16]0)                  # Reserved
    $writer.Write([uint16]1)                  # Type = ICO
    $writer.Write([uint16]$sizes.Count)       # Image count

    # Directory entries: each 16 bytes
    $dataOffset = 6 + ($sizes.Count * 16)
    for ($i = 0; $i -lt $sizes.Count; $i++) {
        $sz  = $sizes[$i]
        $len = $imgData[$i].Length
        $w   = if ($sz -eq 256) { [byte]0 } else { [byte]$sz }
        $h   = if ($sz -eq 256) { [byte]0 } else { [byte]$sz }

        $writer.Write($w)                     # Width  (0 = 256)
        $writer.Write($h)                     # Height (0 = 256)
        $writer.Write([byte]0)                # Color count
        $writer.Write([byte]0)                # Reserved
        $writer.Write([uint16]1)              # Planes
        $writer.Write([uint16]32)             # Bit count
        $writer.Write([uint32]$len)           # Size of image data
        $writer.Write([uint32]$dataOffset)    # Offset of image data

        $dataOffset += $len
    }

    # PNG image data blobs
    foreach ($blob in $imgData) {
        $writer.Write($blob)
    }

    $writer.Close()
    $fs.Close()

    Write-Host "  ICO created: $IcoPath ($($sizes -join ', ') px)" -ForegroundColor Green
}

ConvertTo-MultiSizeIco -PngPath $PNG_ICON -IcoPath $ICO_ICON
Write-Host ""

# ---------------------------------------------------------------------------
# STEP 2: MAVEN BUILD (JDK 17)
# ---------------------------------------------------------------------------

Write-Host "[2/5] Building JAR with Maven (JDK 17)..." -ForegroundColor Yellow

Push-Location $PROJECT_ROOT
try {
    & mvn.cmd clean package -DskipTests 2>&1 | ForEach-Object { Write-Host "  $_" }
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed (exit code $LASTEXITCODE)" }
} finally {
    Pop-Location
}

$jarPath = "$TARGET_DIR\$JAR_NAME"
if (-not (Test-Path $jarPath)) {
    Write-Error "Expected JAR not found: $jarPath"
    exit 1
}

Write-Host "  JAR: $jarPath" -ForegroundColor Green
Write-Host ""

# ---------------------------------------------------------------------------
# STEP 3: PREPARE OUTPUT DIRECTORY
# ---------------------------------------------------------------------------

Write-Host "[3/5] Preparing output directory..." -ForegroundColor Yellow

if (Test-Path $OUTPUT_DIR) {
    Remove-Item -Recurse -Force $OUTPUT_DIR
}
New-Item -ItemType Directory -Force $OUTPUT_DIR | Out-Null

Write-Host "  Output dir: $OUTPUT_DIR" -ForegroundColor Green
Write-Host ""

# ---------------------------------------------------------------------------
# STEP 4: JPACKAGE - CREATE MSI
# ---------------------------------------------------------------------------

Write-Host "[4/5] Running jpackage to produce MSI..." -ForegroundColor Yellow
Write-Host "  This may take 2-4 minutes while jpackage bundles the JDK 17 runtime..." -ForegroundColor DarkGray
Write-Host ""

$jpackageArgs = @(
    "--type",           "msi",
    "--name",           $APP_NAME,
    "--app-version",    $APP_VERSION,
    "--vendor",         $APP_VENDOR,
    "--description",    $APP_DESCRIPTION,
    "--copyright",      $APP_COPYRIGHT,
    "--input",          $TARGET_DIR,
    "--main-jar",       $JAR_NAME,
    "--main-class",     "org.example.AgentApplication",
    "--icon",           $ICO_ICON,
    "--dest",           $OUTPUT_DIR,

    # Windows-specific
    "--win-upgrade-uuid",   $UPGRADE_UUID,
    "--win-menu",
    "--win-shortcut",
    "--win-menu-group",     "EMS",
    "--win-dir-chooser",
    "--win-per-user-install",

    # JVM options
    "--java-options",   "-Dfile.encoding=UTF-8",
    "--java-options",   "-Dsun.java2d.uiScale.enabled=true",
    "--java-options",   "-Dawt.useSystemAAFontSettings=on",
    "--java-options",   "-Dswing.aatext=true"
)

& $JPACKAGE @jpackageArgs 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage failed with exit code $LASTEXITCODE"
    exit 1
}

# ---------------------------------------------------------------------------
# STEP 5: VERIFY OUTPUT
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "[5/5] Verifying output..." -ForegroundColor Yellow

$msiFile = Get-ChildItem -Path $OUTPUT_DIR -Filter "*.msi" | Select-Object -First 1

if ($null -eq $msiFile) {
    Write-Error "MSI file not found in output directory: $OUTPUT_DIR"
    exit 1
}

$msiSizeMB = [math]::Round($msiFile.Length / 1MB, 1)

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  MSI File  : $($msiFile.FullName)" -ForegroundColor White
Write-Host "  Size      : $msiSizeMB MB" -ForegroundColor White
Write-Host "  Version   : $APP_VERSION" -ForegroundColor White
Write-Host "  Vendor    : $APP_VENDOR" -ForegroundColor White
Write-Host "  Upgrade   : {$UPGRADE_UUID} (fixed - OTA compatible)" -ForegroundColor White
Write-Host ""
Write-Host "  Fresh install behavior:" -ForegroundColor DarkGray
Write-Host "    No saved credentials -> Login page shown -> Employee logs in" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  OTA upgrade behavior:" -ForegroundColor DarkGray
Write-Host "    Same UpgradeCode -> Windows REMOVES old version -> Credentials preserved" -ForegroundColor DarkGray
Write-Host "    -> New version starts automatically" -ForegroundColor DarkGray
Write-Host ""

# Open the output folder in Explorer
Start-Process "explorer.exe" $OUTPUT_DIR
