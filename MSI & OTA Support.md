# EMS Agent — Production MSI & Complete OTA Update Guide

> **Document Version:** 1.0.0  
> **Target Audience:** Core Developers, DevOps Engineers, System Administrators  
> **Product:** Employee Monitoring System (EMS) Desktop Agent  
> **Supported Platform:** Windows 10 / 11 (x64)

---

## 1. Executive Summary & Architecture Overview

The **EMS Desktop Agent** is a lightweight, background-resilient workstation client built with **Java 17 (LTS)** and **Java Swing**, backed by a **Spring Boot 3.x** microservice with **MongoDB**.

To provide a modern, seamless enterprise software experience:
1. **Self-Contained MSI Package:** The agent is packaged as a standalone Windows Installer (`.msi`) using `jpackage` and **WiX Toolset v3.14**. End users do **not** need Java or any runtime pre-installed.
2. **True In-Place Over-The-Air (OTA) Updates:** The agent automatically discovers new versions published to the backend, downloads the package in the background, verifies cryptographic SHA-256 integrity, silently applies the upgrade, preserves existing session credentials, and relaunches without requiring administrative intervention.

```
+---------------------------------------------------------------------------------------+
|                                    DEVELOPMENT & BUILD                                |
|  [Java 17 Source]  --->  [Maven Shade Fat JAR]  --->  [jpackage + WiX Toolset v3.14]  |
|                                                                 |                     |
|                                                                 v                     |
|                                                      [EMS Agent-X.Y.Z.msi]            |
+---------------------------------------------------------+-----------------------------+
                                                          |
                                                          v POST /api/v1/agent-updates/publish
+---------------------------------------------------------+-----------------------------+
|                                   BACKEND CLOUD / ON-PREM                             |
|  - Spring Boot API (/api/v1/agent-updates/latest, /download/{version})                |
|  - MongoDB (Metadata, SHA-256, Release Notes, Active Release Flag)                    |
|  - Storage Engine (Stores production MSI packages)                                   |
+---------------------------------------------------------+-----------------------------+
                                                          |
                 GET /latest (Every 30 mins)              | In-Place Download (TLS 1.3)
                                                          v
+---------------------------------------------------------+-----------------------------+
|                                  CLIENT DESKTOP WORKSTATION                           |
|  1. Periodic Check  --->  Update Found (v1.2.5 -> v2.0.2)                             |
|  2. UI Banner       --->  Dynamic auto-expand banner in Workspace Window              |
|  3. Background DL   --->  Downloaded to C:\ProgramData\EmployeeAgent\updates          |
|  4. Integrity Hash  --->  SHA-256 verification against database signature             |
|  5. Silent Upgrade  --->  msiexec.exe /i "..." /qn (Windows removes old, keeps data) |
|  6. Relaunch        --->  Session preserved, auto-starts on boot                     |
+---------------------------------------------------------------------------------------+
```

---

## 2. Core Pillars of Production-Grade MSI Packaging

Creating an installer that supports reliable silent upgrades without breaking existing user data requires strict adherence to Windows Installer best practices.

### 2.1 The Fixed `UpgradeCode` UUID (Crucial for OTA)
In Windows Installer technology, every package has two identity GUIDs:
* **ProductCode:** Changes with *every* new version.
* **UpgradeCode:** **MUST NEVER CHANGE** throughout the entire lifetime of the application.

```powershell
$UPGRADE_UUID = "8B3D4A29-F5E7-4C1B-A2D9-6E8F0B1C2D3E"
```

#### Why is this critical?
When Windows executes an update MSI:
1. It reads the new MSI's `UpgradeCode`.
2. It detects that an older version with the exact same `UpgradeCode` is currently installed.
3. It performs a **Major Upgrade**: silently uninstalls the old executable binaries, installs the new binaries, and leaves the configuration and data folders untouched.
4. **If this UUID ever changes**, Windows treats the new version as an unrelated app and installs it side-by-side, creating duplicate shortcuts and data conflicts.

---

### 2.2 Per-User Installation (`--win-per-user-install`)
* **Standard corporate setup:** Employees often work on enterprise workstations without local administrator (UAC) privileges.
* By using `--win-per-user-install`:
  * Installation target: `C:\Users\<Username>\AppData\Local\EMS Agent\`
  * Does **not** require elevation or administrator prompts during fresh install or background OTA upgrades.
  * Allows unattended silent upgrades via `msiexec /i <file> /qn`.

---

### 2.3 Bundled JDK Runtime
* `jpackage` extracts only the necessary modules of the JDK 17 runtime into `runtime/` inside the installation folder.
* **Benefits:**
  * Zero external dependency: Client PCs need neither Java nor environment variables configured.
  * Isolation: The agent's Java version is insulated from any other Java installations on the client's PC.

---

### 2.4 Pixel-Perfect High-DPI Multi-Size Icon (.ico)
Windows displays icons in multiple resolutions across the operating system (16px in taskbar, 32px/48px on desktop, 64px/128px/256px in high-DPI scaling and Alt+Tab).

If a non-square source PNG is forcibly stretched into an icon, Windows distorts and squishes the logo. Our build script implements an **Aspect-Ratio Preserving Converter**:

```powershell
# Excerpt from packaging/build-msi.ps1
$sizes = @(16, 24, 32, 48, 64, 128, 256)

foreach ($sz in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($sz, $sz, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

    # Calculate proportional aspect ratio without stretching
    $ratio = [Math]::Min([double]$sz / $src.Width, [double]$sz / $src.Height)
    $destW = [int][Math]::Round($src.Width * $ratio)
    $destH = [int][Math]::Round($src.Height * $ratio)
    $destX = [int][Math]::Round(($sz - $destW) / 2.0)
    $destY = [int][Math]::Round(($sz - $destH) / 2.0)

    $g.DrawImage($src, $destX, $destY, $destW, $destH)
    ...
}
```

---

### 2.5 Integrated Corporate End-User License Agreement (EULA)
To satisfy legal and organizational compliance for workplace monitoring:
* A standardized, clean agreement is embedded directly into the WiX setup sequence via `--license-file packaging\installer\terms-and-conditions.txt`.
* The installer requires employees to check **"I accept the terms in the License Agreement"** before proceeding.
* It explicitly discloses:
  * Active window tracking & productivity telemetry
  * Automated periodic desktop screen captures
  * Clear guarantees: **No keylogging**, **No audio/webcam access**, **Strict TLS encryption**

---

### 2.6 Automatic Launch on System Startup
To guarantee workstation attendance accuracy, the agent registers itself in the current user's Windows registry:
* **Registry Key:** `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
* **Entry Name:** `EMSAgent`
* **Target:** Path to installed `EMS Agent.exe`
* The application automatically pops up the login window whenever the PC reboots or powers on.

---

## 3. End-to-End OTA Update Architecture

### 3.1 Backend Data Model & Endpoints

#### MongoDB Collection: `agent_updates`
```json
{
  "_id": ObjectId("6709088..."),
  "version": "2.0.2",
  "platform": "windows",
  "fileName": "EmployeeMonitoringAgent-2.0.2.msi",
  "storagePath": "D:\\EMS 3.0\\employee\\agent-updates\\EmployeeMonitoringAgent-2.0.2.msi",
  "sha256": "4b9e27c089...34259a",
  "mandatory": true,
  "releaseNotes": "EMS 2.0.2: Proportional logo icon, complete layout fix, and single-session conflict fix.",
  "active": true,
  "createdAt": ISODate("2026-10-11T13:47:37Z")
}
```

#### REST Endpoints:
| Method | Endpoint | Description | Auth |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/agent-updates/publish` | Uploads and publishes an MSI with release notes | Admin / Permitted |
| `GET` | `/api/v1/agent-updates/latest?currentVersion=...&platform=windows` | Checks if a newer version is available | Public / Agent |
| `GET` | `/api/v1/agent-updates/download/{version}` | Downloads the binary stream of the MSI | Public / Agent |

---

### 3.2 Semantic Version Comparison Engine
In `org.example.ota.VersionComparator`:
* Versions are split into numerical components (e.g., `2.0.2` -> `[2, 0, 2]`).
* Handles varying depths (`1.2.5` vs `2.0.2` vs `2.0.2.1`).
* Only triggers update if:
  $$\text{RemoteVersion} > \text{CurrentVersion}$$
* Equal or lower versions return `204 No Content` (no prompt shown to user).

---

### 3.3 Dynamic Workspace UI Banner
When an update is detected, `EmployeeWorkspaceWindow` displays an update banner:
* **Dynamic Window Height Adjustment:** The window expands its height from `550px` to `630px` dynamically (`setSize` + `setShape`), ensuring **Start Break** and **Start Lunch** action buttons are never pushed off-screen or clipped.
* **Modern Design:** Glassmorphism gradient styling, accent ribbon, and clear version progression (`v1.2.5 ➔ v2.0.2`).
* **Update Action:** Clicking **"Update Now"** initiates the background updater without blocking work timers.

---

### 3.4 Cryptographic SHA-256 Validation
Before running any downloaded installer, `OtaUpdateService` computes the local SHA-256 checksum:
```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(Files.readAllBytes(downloadedFile));
String localSha256 = HexFormat.of().formatHex(hash);

if (!localSha256.equalsIgnoreCase(metadata.getSha256())) {
    Files.deleteIfExists(downloadedFile);
    throw new SecurityException("OTA integrity validation failed. Hash mismatch!");
}
```
* **Security:** Protects against corrupted downloads, incomplete transfers, and man-in-the-middle tampering.

---

### 3.5 Silent In-Place Windows Upgrade Execution
Once verified, the update process executes the Windows Installer silently:
```java
ProcessBuilder pb = new ProcessBuilder(
    "msiexec.exe",
    "/i", downloadedMsi.toAbsolutePath().toString(),
    "/qn",               // Completely silent (no UI wizards)
    "/norestart"         // Do not reboot the machine
);
pb.start();
System.exit(0);          // Release file locks so installer can replace binaries
```
* Windows Installer replaces `EMS Agent.exe` and JAR dependencies.
* The application restarts automatically.
* All configuration (`C:\ProgramData\EmployeeAgent\config.properties`) and tokens remain intact.

---

## 4. Step-by-Step Developer Guide: Creating & Publishing a Release

### Prerequisites (Installed on build machine)
1. **JDK 17:** `C:\Program Files\Java\jdk-17`
2. **WiX Toolset v3.14:** `C:\Program Files (x86)\WiX Toolset v3.14\bin`
3. **Apache Maven:** Configured on system PATH

---

### Step 1: Bump the Version Number
When releasing a new build (e.g., from `2.0.2` to `2.0.3`), update the version in three files:

1. **`employee-agent/pom.xml`**:
   ```xml
   <groupId>org.example</groupId>
   <artifactId>employee-agent</artifactId>
   <version>2.0.3</version>
   ```

2. **`employee-agent/src/main/java/org/example/AgentVersion.java`**:
   ```java
   public static final String VERSION = "2.0.3";
   ```

3. **`employee-agent/packaging/build-msi.ps1`**:
   ```powershell
   $APP_VERSION = "2.0.3"
   ```

---

### Step 2: Run the Automated Build Script
Open PowerShell in `employee-agent\packaging\` and run:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\build-msi.ps1
```

#### What this script does automatically:
1. Validates `jpackage` (JDK 17) and WiX (`candle.exe`, `light.exe`).
2. Converts `packaging\installer\icon\Ems-Agent.png` into a high-DPI multi-size `.ico` with preserved aspect ratio.
3. Compiles the Maven fat shaded JAR (`employee-agent-X.Y.Z.jar`).
4. Invokes `jpackage` with the fixed `UpgradeCode`, EULA license file, and Windows shortcut parameters.
5. Outputs the final production installer to:
   📁 **`packaging\output\EMS Agent-X.Y.Z.msi`** (~53 MB)

---

### Step 3: Publish the Release via Postman / REST API
To make this release available to all installed desktop clients via OTA:

* **HTTP Method:** `POST`
* **URL:** `http://<SERVER_HOST>:8083/api/v1/agent-updates/publish`
* **Body Type:** `form-data`

| Key | Type | Value |
| :--- | :--- | :--- |
| **`version`** | Text | `2.0.3` |
| **`platform`** | Text | `windows` |
| **`mandatory`** | Text | `true` |
| **`releaseNotes`** | Text | Description of new features and fixes |
| **`file`** | File | Select `EMS Agent-2.0.3.msi` from `packaging\output\` |

#### Expected Response (`200 OK`):
```json
{
  "version": "2.0.3",
  "downloadUrl": "http://<SERVER_HOST>:8083/api/v1/agent-updates/download/2.0.3",
  "sha256": "4b9e27c089...34259a",
  "mandatory": true,
  "releaseNotes": "...",
  "active": true
}
```

---

### Step 4: Verify the Update on Client Machines
1. Any running desktop agent with an older version (`< 2.0.3`) will detect the update on its next scheduled check (or upon login).
2. The update banner will display with the release notes.
3. Clicking **"Update Now"** downloads and silently applies the update.
4. The client relaunches running the new version with the employee still logged in.

---

## 5. Troubleshooting & Critical Developer Checklists

| Symptom / Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| **`Update file already exists: ...msi` (HTTP 500)** | An MSI with the exact same version name was already uploaded to the server's storage folder. | Publish under a bumped version (e.g., `2.0.3`), or remove the existing file from `agent-updates` folder. |
| **Old and new versions installed side-by-side** | The `UPGRADE_UUID` was changed between builds. | **Never modify** `$UPGRADE_UUID = "8B3D4A29-F5E7-4C1B-A2D9-6E8F0B1C2D3E"`. Keeping it identical guarantees Windows replaces the old version cleanly. |
| **Desktop icon looks stretched or distorted** | The icon generator forced a non-square PNG into a square canvas without aspect preservation. | Use the proportional aspect-fit logic implemented in `build-msi.ps1`. |
| **Break and Lunch buttons cut off when banner appears** | Window height was fixed and could not accommodate the update banner. | `EmployeeWorkspaceWindow` has base height set to `630px` and automatically re-evaluates bounds when the banner becomes visible. |
| **OTA download fails with HTTP 401 Unauthorized** | The update download endpoint was blocked by Spring Security. | Ensure `/api/v1/agent-updates/**` is permitted in `SecurityConfig.java`. |

---

## 6. Directory Structure Reference

```
EMS 3.0/
├── README.md                              <-- Project Overview
├── MSI & OTA Support.md                   <-- THIS DOCUMENT
│
├── employee/                              <-- Spring Boot Backend (Port 8083)
│   ├── src/main/java/com/example/employee/
│   │   ├── config/SecurityConfig.java     <-- Permitted update routes
│   │   ├── controller/AgentUpdateController.java <-- Publish & Download APIs
│   │   ├── service/AgentUpdateFileService.java   <-- Storage & SHA-256 calculation
│   │   └── service/AgentUpdateService.java       <-- MongoDB Version tracking
│   └── agent-updates/                     <-- Server storage for published MSIs
│
└── employee-agent/                        <-- Swing Desktop Client
    ├── pom.xml                            <-- Maven Shade packaging configuration
    ├── src/main/java/org/example/
    │   ├── AgentVersion.java              <-- Canonical agent version string
    │   ├── AgentApplication.java          <-- Entry point, Registry auto-start
    │   ├── ota/
    │   │   ├── UpdateChecker.java         <-- Version discovery client
    │   │   ├── OtaUpdateService.java      <-- Secure downloader & SHA-256 verifier
    │   │   └── VersionComparator.java     <-- Semantic version comparison
    │   └── ui/
    │       ├── EmployeeWorkspaceWindow.java <-- Dynamic layout, update banner
    │       └── AppIcon.java               <-- Multi-size window icon loader
    └── packaging/
        ├── build-msi.ps1                  <-- Production WiX MSI build script
        ├── installer/
        │   ├── terms-and-conditions.txt   <-- Clean corporate EULA agreement
        │   └── icon/
        │       ├── Ems-Agent.png          <-- High-res transparent 3D logo
        │       └── Ems-Agent.ico          <-- Generated multi-size Windows icon
        └── output/
            └── EMS Agent-2.0.2.msi        <-- Final production-ready MSI
```

---

## 7. Complete Production Build Script Reference (`build-msi.ps1`)

For total reproducibility, here is the full, standalone production build script located at `employee-agent/packaging/build-msi.ps1`:

```powershell
# =============================================================================
# EMS Agent - Production MSI Build Script
# =============================================================================
#
# Prerequisites (installed on build machine):
#   - JDK 17   at: C:\Program Files\Java\jdk-17
#   - WiX 3.14 at: C:\Program Files (x86)\WiX Toolset v3.14\bin
#   - Maven on PATH
#
# Output:
#   packaging\output\EMS Agent-<VERSION>.msi
# =============================================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------------------------

$SCRIPT_DIR   = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = Split-Path -Parent $SCRIPT_DIR

$APP_NAME        = "EMS Agent"
$APP_VERSION     = "2.0.2"
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
$LICENSE_FILE  = "$PROJECT_ROOT\packaging\installer\terms-and-conditions.txt"
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
    Write-Error "jpackage.exe not found at: $JPACKAGE"
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
# STEP 1: CREATE MULTI-SIZE .ICO FROM PNG (ASPECT RATIO PRESERVED)
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
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

        $ratio = [Math]::Min([double]$sz / $src.Width, [double]$sz / $src.Height)
        $destW = [int][Math]::Round($src.Width * $ratio)
        $destH = [int][Math]::Round($src.Height * $ratio)
        $destX = [int][Math]::Round(($sz - $destW) / 2.0)
        $destY = [int][Math]::Round(($sz - $destH) / 2.0)

        $g.DrawImage($src, $destX, $destY, $destW, $destH)
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
    "--license-file",       $LICENSE_FILE,

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

Start-Process "explorer.exe" $OUTPUT_DIR
```

