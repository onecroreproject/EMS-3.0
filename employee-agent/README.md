# 🛡️ EMS 3.0 Desktop Agent

The **EMS Agent** is the secure, lightweight client application installed on employee workstations. It is responsible for silently tracking work activity, ensuring accurate timesheets, and providing management with productivity insights.

## 🌟 Core Capabilities

### 1. ⏱️ Automated Timesheets & Idle Detection
The agent hooks into native OS events to monitor keyboard and mouse activity. 
- Automatically pauses the work timer if the user is idle for a configurable period (e.g., 5 minutes).
- Accurately tracks "Active Time" vs "Idle Time" for precise payroll and timesheet generation.

### 2. 📊 Application & Window Tracking
It continuously monitors the active foreground window.
- Logs the application name and window title.
- Enables the backend to categorize time spent into "Productive", "Neutral", or "Unproductive" categories based on company policy.

### 3. 📸 Secure Screen Capture
Takes periodic screenshots of the employee's desktop based on admin configurations.
- Screenshots are compressed and securely transmitted over HTTPS.
- Protects against tampering or local deletion.

### 4. 📴 Offline Resilience Architecture
Network drops do not cause data loss.
- All tracking data and screenshots are cached in a **Persistent Local Queue** when offline.
- Upon reconnection, the agent seamlessly synchronizes all cached data to the backend.

### 5. 🔄 Over-The-Air (OTA) Updates
Zero-touch IT administration.
- The agent polls the backend for new versions.
- Silently downloads and executes MSI updates in the background, ensuring all employees are always running the latest security patches.

---

## ⚙️ Architecture & Workflow

```mermaid
flowchart TD
    subgraph Client Workstation
        A[User Input (KB/Mouse)] --> B{Idle Detector}
        B -- Active --> C[Activity Logger]
        B -- Idle --> D[Pause Tracking]
        
        C --> E[Data Aggregator]
        F[Screen Capture Task] --> E
        
        E --> G{Network Available?}
        G -- Yes --> H[Transmit to Server via REST]
        G -- No --> I[(Local SQLite / File Queue)]
        I -. Retries when online .-> H
    end
    
    subgraph Server
        H --> J[(MongoDB)]
    end
```

---

## 🛠️ Development & Build Instructions

### Prerequisites
- Java 17 JDK
- Maven
- WiX Toolset (Required for building the `.msi` Windows installer)

### Configuration
Before building, ensure the agent points to your production or staging backend.
Modify `src/main/resources/config.properties`:
```properties
server.url=https://api.yourcompany.com
agent.version=1.2.4
update.check.interval.minutes=60
```

### Building the Native Installer (MSI)
We use the modern `jpackage` tool combined with WiX to bundle the Java runtime and application into a standalone Windows installer.

1. Compile the JAR file:
```bash
mvn clean package
```
2. Run the packaging script:
```powershell
cd packaging
.\build-msi.ps1
```
The final installer will be generated in `packaging/output/EMS Agent-1.2.4.msi`.
