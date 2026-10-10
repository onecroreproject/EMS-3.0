# Employee Monitoring System (EMS 3.0)

Welcome to the Employee Monitoring System (EMS 3.0), a comprehensive, enterprise-grade solution designed to track, manage, and optimize employee productivity across distributed teams.

## Project Structure

This monorepo contains all three major components of the EMS platform:

1. **`admin-ui/` (React Frontend)**
   A beautiful, modern single-page application built with React, Vite, and Tailwind CSS. It provides a centralized dashboard for administrators and HR managers to oversee organization structure, view real-time tracking metrics, approve timesheets, and review employee screenshots.

2. **`employee/` (Spring Boot Backend)**
   The core REST API server and business logic engine built with Java and Spring Boot. It uses MongoDB for flexible, high-performance data storage. It handles authentication, real-time agent ingestion, hierarchy management (Departments & Teams), and complex metric aggregations.

3. **`employee-agent/` (Desktop Client)**
   A lightweight, resilient Java desktop application installed on employee machines. It runs seamlessly in the background to capture application activity, measure idle time, take configurable screenshots, and report data securely back to the backend. It includes a robust offline queue and Over-The-Air (OTA) update capabilities.

## Quick Start

### Backend (`employee/`)
Requirements: Java 17+, Maven, MongoDB.
```bash
cd employee
mvn clean install
mvn spring-boot:run
```
Make sure MongoDB is running locally or provide connection details in `application.properties`.

### Frontend (`admin-ui/`)
Requirements: Node.js 18+.
```bash
cd admin-ui
npm install
npm run dev
```

### Desktop Agent (`employee-agent/`)
Requirements: Java 17+.
```bash
cd employee-agent
mvn clean install
# The packaged application and MSI builder are available in the packaging/ folder.
```

## Core Features
- **Organization Hierarchy**: Manage Departments, Teams, and Employee assignments.
- **Real-time Monitoring**: Live dashboards showing active users, idle users, and offline agents.
- **Productivity Analysis**: Deep dive into application usage and daily timesheets.
- **Automated OTA Updates**: Push agent updates to thousands of devices instantly via the backend.
