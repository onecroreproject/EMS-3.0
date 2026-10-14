# 📋 EMS 3.0 — Server Deployment & Operations Requirements Guide

This document is prepared for the **Server Administration & DevOps Team** to safely and accurately deploy the latest updates for **EMS 3.0** (Backend, Admin UI, and Desktop Agent v2.0.4) on the production server (`/var/www/ems`).

---

## 📌 Release Information
* **Release Version:** `EMS 3.0`
* **Agent Version:** `v2.0.4`
* **Target Environment:** Production (`srv1322628` / `ems.dlksoftwaresolutions.co.in`)
* **Git Branch:** `main`

---

## 1. 🛠️ Server Environment Prerequisites

Ensure the following tools and services are installed and active on the server:

| Tool / Service | Minimum Required Version | Purpose |
| :--- | :--- | :--- |
| **Git** | Any modern version | Code synchronization from GitHub |
| **Node.js** | v18+ (Recommended v20 LTS) | Frontend package management & bundling |
| **npm** | v9+ | Installing dependencies (`react-toastify`, etc.) |
| **Java JDK** | OpenJDK 17 | Running the Spring Boot application |
| **Maven** | v3.8+ | Compiling the backend `.jar` |
| **Nginx** | v1.18+ | Reverse proxy serving frontend `dist/` & routing `/api` |
| **MongoDB** | Cloud Atlas Cluster | Database storage (Atlas URI in properties/env) |

---

## 2. 🚀 Step-by-Step Deployment Instructions

Run the following commands in order on the server terminal as `root`:

### Step 2.1: Update Repository Code
```bash
cd /var/www/ems
git fetch origin
git pull origin main
```

---

### Step 2.2: Deploy Frontend (`admin-ui`)
> ⚠️ **IMPORTANT**: You **MUST run `npm install`** before `npm run build` because new packages (`react-toastify`) were added. Running build without `npm install` will throw `TS2307: Cannot find module 'react-toastify'`.

```bash
cd /var/www/ems/admin-ui

# 1. Install required packages into node_modules
npm install

# 2. Compile production bundle into dist/
npm run build

# 3. Verify Nginx configuration and reload
nginx -t && systemctl reload nginx
```

---

### Step 2.3: Deploy Backend (`employee`)
> The backend introduces the new Dynamic Notification System, MongoDB collections, and real-time login/logout hooks.

```bash
cd /var/www/ems/employee

# 1. Compile and package the updated Spring Boot JAR
mvn clean package -DskipTests

# 2. Restart the Spring Boot service
# (Use whichever service name is configured on this server):
systemctl restart ems-backend
# OR:
# systemctl restart ems

# 3. Verify that the service started cleanly
systemctl status ems-backend --no-pager
```

To view live backend logs and ensure MongoDB connection is healthy:
```bash
journalctl -u ems-backend -f -n 50
```

---

### Step 2.4: Deploy Desktop Agent Installer (`v2.0.4`) for OTA
The desktop agent uses Over-The-Air (OTA) updates configured via:
`agent.update.storage-directory=/var/lib/ems/agent-updates`

1. Ensure the directory exists on the server with write permissions:
   ```bash
   mkdir -p /var/lib/ems/agent-updates
   chmod 755 /var/lib/ems/agent-updates
   ```
2. Upload / copy the newly built **`EMS Agent-2.0.4.msi`** to:
   ```bash
   /var/lib/ems/agent-updates/EMS Agent-2.0.4.msi
   ```

---

## 3. 🛡️ Verification & Health Checklist

After completing the steps above, verify the deployment:

- [ ] **Admin Portal Check:** Open `https://ems.dlksoftwaresolutions.co.in/login` in your browser.
- [ ] **MFA / Session Conflict:** Verify that the 2-step verification code logs in properly. If an older session exists, verify the "Terminate other session & Login" button displays and functions.
- [ ] **Notification Bell:** In the top header, verify the notification bell displays an unread count badge and opens the dynamic notifications drawer.
- [ ] **Desktop Agent Heartbeat:** Launch the EMS Agent on an employee PC. Verify it connects to `https://ems.dlksoftwaresolutions.co.in/api/agent/login` with HTTP 200 and begins activity tracking.

---

## 4. 🔧 Troubleshooting & Common Issues

### Issue 1: `error TS2307: Cannot find module 'react-toastify'`
* **Cause:** `npm run build` was executed without running `npm install` first.
* **Fix:**
  ```bash
  cd /var/www/ems/admin-ui
  npm install
  npm run build
  ```

### Issue 2: Backend returns `409 Conflict` during login
* **Cause:** The account already has an active session in MongoDB (`active_sessions` collection) due to single-session security.
* **Fix:** The new frontend UI now provides a **"Force Login"** button on both the credential step and the MFA step to terminate the older session. Alternatively, clear the document in MongoDB `active_sessions`.

### Issue 3: Port 8082 / 8083 In Use Error on Backend Restart
* **Cause:** The old java process did not terminate before restarting.
* **Fix:**
  ```bash
  # Find PID using port
  fuser -k 8082/tcp || fuser -k 8083/tcp
  systemctl restart ems-backend
  ```

---

*Document prepared for EMS 3.0 Production Maintenance.*
