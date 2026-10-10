# Production Deployment Guide (VPS)

This guide walks you through deploying the Spring Boot API backend to a Linux VPS (Ubuntu/Debian) and preparing the Desktop Agent for employees.

## Phase 1: Prepare the Desktop Agent (CRITICAL)

Before you hand the agent out to employees, you **must** update the server URL so it points to your live VPS, not your local computer.

1. Open `employee-agent/src/main/resources/config.properties`.
2. Change the `server.url` to your live VPS IP or Domain Name:
   ```properties
   server.url=http://<VPS_PUBLIC_IP>:8083
   ```
   *(Note: Based on your `env.properties`, your server runs on port 8083! Make sure the agent matches this port).*
3. Re-compile the agent:
   ```bash
   cd employee-agent
   mvn clean package
   ```
4. Distribute the resulting `employee-agent-1.2.3-shaded.jar` (or bundle it into an `.exe` using Launch4j) to your employees.

---

## Phase 2: VPS Server Setup

### 1. MongoDB Network Whitelist
Your MongoDB database requires connections to be whitelisted.
1. Log into MongoDB Atlas.
2. Go to **Network Access**.
3. Add the **Public IP Address of your VPS**.

### 2. Install Java on the VPS
Connect to your VPS via SSH and install Java 17:
```bash
sudo apt update
sudo apt install openjdk-17-jre-headless -y
```

### 3. Transfer Files
Create an application directory on your VPS:
```bash
mkdir -p /opt/ems
```
Use WinSCP, FileZilla, or `scp` to transfer the following files to `/opt/ems` on the VPS:
1. `employee-0.0.1-SNAPSHOT.jar` (from the `employee/target/` folder).
2. `env.properties` (from the `employee/src/main/resources/` folder).

---

## Phase 3: Running the Application Securely

To ensure your application runs continuously in the background (and restarts if the server reboots), configure a Systemd Service.

1. Create a service file:
   ```bash
   sudo nano /etc/systemd/system/ems.service
   ```

2. Paste the following configuration:
   ```ini
   [Unit]
   Description=EMS Spring Boot Application
   After=syslog.target network.target

   [Service]
   User=root
   WorkingDirectory=/opt/ems
   ExecStart=/usr/bin/java -jar /opt/ems/employee-0.0.1-SNAPSHOT.jar
   SuccessExitStatus=143
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

3. Enable and start the service:
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable ems.service
   sudo systemctl start ems.service
   ```

4. Check the logs to verify it started successfully:
   ```bash
   sudo journalctl -u ems.service -f
   ```

---

## Phase 4: Production Monitoring (Prometheus & Grafana)

If you want to view the Grafana dashboard on your live server:
1. Install Docker and Docker Compose on the VPS.
2. Transfer the `monitoring/` folder to the VPS.
3. Update `prometheus.yml` to scrape `host.docker.internal:8083` (matching your production port).
4. Run `docker-compose up -d`.
5. Access Grafana at `http://<VPS_PUBLIC_IP>:3000`.
