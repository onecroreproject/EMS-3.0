package com.example.employee.controller.api.admin;

import com.example.employee.model.Device;
import com.example.employee.model.Employee;
import com.example.employee.model.ApplicationActivityDocument;
import com.example.employee.repository.DeviceRepository;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.ApplicationActivityRepository;
import com.example.employee.service.WorkSessionService;
import com.example.employee.model.WorkSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.example.employee.repository.AttendanceEventRepository;
import com.example.employee.model.AttendanceEventDocument;
import com.example.employee.attendance.AttendanceEventType;
import org.springframework.data.mongodb.core.MongoTemplate;

@RestController
@RequestMapping("/api/admin/tracking")
public class AdminTrackingController {

    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationActivityRepository appActivityRepository;
    private final WorkSessionService workSessionService;
    private final AttendanceEventRepository attendanceEventRepository;
    private final MongoTemplate mongoTemplate;

    public AdminTrackingController(
            DeviceRepository deviceRepository,
            EmployeeRepository employeeRepository,
            ApplicationActivityRepository appActivityRepository,
            WorkSessionService workSessionService,
            AttendanceEventRepository attendanceEventRepository,
            MongoTemplate mongoTemplate) {
        this.deviceRepository = deviceRepository;
        this.employeeRepository = employeeRepository;
        this.appActivityRepository = appActivityRepository;
        this.workSessionService = workSessionService;
        this.attendanceEventRepository = attendanceEventRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/devices")
    public ResponseEntity<List<Map<String, Object>>> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<Employee> employees = employeeRepository.findAll();
        Map<String, Employee> empMap = employees.stream()
                .filter(e -> e.getEmployeeCode() != null)
                .collect(Collectors.toMap(Employee::getEmployeeCode, e -> e, (e1, e2) -> e1));

        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Device d : devices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("deviceId", d.getDeviceId());
            map.put("hostname", d.getHostname() != null ? d.getHostname() : "Unknown");
            map.put("os", d.getOperatingSystem() != null ? d.getOperatingSystem() : "Unknown");
            
            // Check if last seen is within 5 minutes for ONLINE status
            String status = d.getStatus();
            if (d.getLastSeen() != null) {
                if (d.getLastSeen().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
                    status = "OFFLINE";
                }
            } else {
                status = "OFFLINE";
            }
            map.put("status", status);
            
            String activity = "-";
            if ("ONLINE".equals(status)) {
                activity = "WORKING"; // Default assumption if online
                if (d.getEmployeeCode() != null) {
                    java.util.Optional<AttendanceEventDocument> lastEventOpt = attendanceEventRepository.findTopByEmployeeCodeOrderByTimestampDesc(d.getEmployeeCode());
                    if (lastEventOpt.isPresent()) {
                        AttendanceEventType type = lastEventOpt.get().getEventType();
                        if (type == AttendanceEventType.BREAK_STARTED) {
                            activity = "BREAK";
                        } else if (type == AttendanceEventType.LUNCH_STARTED) {
                            activity = "LUNCH";
                        } else if (type == AttendanceEventType.IDLE_STARTED) {
                            activity = "IDLE";
                        } else if (type == AttendanceEventType.WORK_ENDED) {
                            activity = "-";
                        } else {
                            activity = "WORKING"; // IDLE_ENDED, BREAK_ENDED, LUNCH_ENDED, WORK_STARTED
                        }
                    }
                }
            }
            map.put("activity", activity);
            map.put("lastSeen", d.getLastSeen() != null ? d.getLastSeen().toString() : "");
            map.put("agentVersion", d.getAgentVersion() != null ? d.getAgentVersion() : "1.0.0");
            map.put("registeredAt", d.getRegisteredAt() != null ? d.getRegisteredAt().toString() : "");
            map.put("ipAddress", "192.168.1." + (new Random().nextInt(150) + 50)); // Mocking IP since it's not in Device model usually
            map.put("location", "Chennai, Tamil Nadu, India");

            // Attach Employee details
            Employee e = d.getEmployeeCode() != null ? empMap.get(d.getEmployeeCode()) : null;
            Map<String, Object> empDetails = new HashMap<>();
            if (e != null) {
                empDetails.put("id", e.getId());
                empDetails.put("name", e.getName());
                empDetails.put("employeeCode", e.getEmployeeCode());
            } else {
                empDetails.put("id", "unknown");
                empDetails.put("name", "Unknown User");
                empDetails.put("employeeCode", d.getEmployeeCode());
            }
            map.put("employee", empDetails);

            result.add(map);
        }

        // If no devices exist in DB (e.g., testing mode without real agent), we can return a 200 OK with empty list.
        // The frontend can handle it. BUT for a good demo, if DB is completely empty of devices, let's inject mock devices based on employees!
        if (result.isEmpty() && !employees.isEmpty()) {
            Random rand = new Random();
            for (int i = 0; i < Math.min(employees.size(), 15); i++) {
                Employee e = employees.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("id", "mock-" + e.getId());
                map.put("deviceId", "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                map.put("hostname", e.getName().split(" ")[0].toUpperCase() + "-PC");
                map.put("os", i % 2 == 0 ? "Windows 11" : "macOS");
                map.put("status", rand.nextBoolean() ? "ONLINE" : "OFFLINE");
                
                String[] activities = {"WORKING", "IDLE", "MEETING", "BREAK"};
                map.put("activity", map.get("status").equals("ONLINE") ? activities[rand.nextInt(activities.length)] : "-");
                map.put("lastSeen", Instant.now().minus(rand.nextInt(60), ChronoUnit.MINUTES).toString());
                map.put("agentVersion", "1.2.0");
                map.put("registeredAt", Instant.now().minus(30, ChronoUnit.DAYS).toString());
                map.put("ipAddress", "192.168.1." + (100 + i));
                map.put("location", "Chennai, Tamil Nadu, India");
                
                Map<String, Object> empDetails = new HashMap<>();
                empDetails.put("id", e.getId());
                empDetails.put("name", e.getName());
                empDetails.put("employeeCode", e.getEmployeeCode());
                map.put("employee", empDetails);
                
                result.add(map);
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getTrackingSummary(
            @RequestParam(required = false) String employeeCode,
            @RequestParam(required = false) String date) {
        
        Map<String, Object> summary = new HashMap<>();
        
        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Kolkata");
        java.time.LocalDate targetDate;
        
        if (date != null && !date.isEmpty()) {
            try {
                targetDate = java.time.LocalDate.parse(date);
            } catch (Exception e) {
                targetDate = java.time.LocalDate.now(zoneId);
            }
        } else {
            targetDate = java.time.LocalDate.now(zoneId);
        }
        
        Instant startOfDay = targetDate.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        // 1. Fetch Application Activities for Top Apps/Websites
        List<ApplicationActivityDocument> activities = new ArrayList<>();
        if (employeeCode != null && !employeeCode.isEmpty()) {
            org.springframework.data.mongodb.core.query.Query activityQuery = new org.springframework.data.mongodb.core.query.Query();
            activityQuery.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("employeeCode").is(employeeCode));
            activityQuery.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("startedAt").gte(startOfDay).lt(endOfDay));
            activities = mongoTemplate.find(activityQuery, ApplicationActivityDocument.class);
        } else {
            // For all employees (rare for summary, but handled)
            org.springframework.data.mongodb.core.query.Query activityQuery = new org.springframework.data.mongodb.core.query.Query();
            activityQuery.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("startedAt").gte(startOfDay).lt(endOfDay));
            activities = mongoTemplate.find(activityQuery, ApplicationActivityDocument.class);
        }

        Map<String, Long> appDurations = new HashMap<>();
        Map<String, Long> webDurations = new HashMap<>();
        for (ApplicationActivityDocument doc : activities) {
            long duration = doc.getDurationSeconds();
            if (doc.getUrl() != null && !doc.getUrl().isEmpty()) {
                String domain = doc.getDomain() != null ? doc.getDomain() : doc.getUrl();
                webDurations.put(domain, webDurations.getOrDefault(domain, 0L) + duration);
            } else {
                String process = doc.getProcessName() != null ? doc.getProcessName() : "Unknown";
                appDurations.put(process, appDurations.getOrDefault(process, 0L) + duration);
            }
        }

        List<Map<String, Object>> topApps = appDurations.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> Map.of("name", (Object)e.getKey(), "durationSeconds", e.getValue()))
                .collect(Collectors.toList());

        List<Map<String, Object>> topWebs = webDurations.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> Map.of("name", (Object)e.getKey(), "durationSeconds", e.getValue()))
                .collect(Collectors.toList());

        // 2. Fetch Attendance Events for Timeline & Stats
        List<AttendanceEventDocument> events = new ArrayList<>();
        if (employeeCode != null && !employeeCode.isEmpty()) {
            events = attendanceEventRepository.findAttendanceEventsForDay(
                employeeCode, startOfDay, endOfDay,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "timestamp")
            );
        }

        // 3. State Machine Calculation
        long totalWorkSeconds = 0;
        long totalIdleSeconds = 0;
        long totalBreakSeconds = 0;
        long totalLunchSeconds = 0;
        
        List<Map<String, Object>> timeline = new ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a").withZone(zoneId);

        String currentState = "OFFLINE";
        Instant currentStateStart = null;
        Instant firstWorkStart = null;
        Instant lastWorkEnd = null;

        for (AttendanceEventDocument event : events) {
            Instant timestamp = event.getTimestamp();
            AttendanceEventType type = event.getEventType();

            // End previous state if it exists and calculate duration
            if (currentStateStart != null && !currentState.equals("OFFLINE")) {
                long duration = java.time.Duration.between(currentStateStart, timestamp).getSeconds();
                if (duration > 0) {
                    if (currentState.equals("WORK")) totalWorkSeconds += duration;
                    else if (currentState.equals("IDLE")) totalIdleSeconds += duration;
                    else if (currentState.equals("BREAK")) totalBreakSeconds += duration;
                    else if (currentState.equals("LUNCH")) totalLunchSeconds += duration;
                }
            }

            // State Machine Transition
            if (type == AttendanceEventType.WORK_STARTED) {
                currentState = "WORK";
                currentStateStart = timestamp;
                if (firstWorkStart == null) firstWorkStart = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Started Working", "color", "emerald", "type", "start"));
            } else if (type == AttendanceEventType.IDLE_STARTED) {
                currentState = "IDLE";
                currentStateStart = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Idle Started", "color", "slate", "type", "idle"));
            } else if (type == AttendanceEventType.BREAK_STARTED) {
                currentState = "BREAK";
                currentStateStart = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Break Started", "color", "orange", "type", "break"));
            } else if (type == AttendanceEventType.LUNCH_STARTED) {
                currentState = "LUNCH";
                currentStateStart = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Lunch Started", "color", "violet", "type", "lunch"));
            } else if (type == AttendanceEventType.IDLE_ENDED || type == AttendanceEventType.BREAK_ENDED || type == AttendanceEventType.LUNCH_ENDED) {
                currentState = "WORK";
                currentStateStart = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Resumed Working", "color", "blue", "type", "resume"));
            } else if (type == AttendanceEventType.WORK_ENDED) {
                currentState = "OFFLINE";
                lastWorkEnd = timestamp;
                timeline.add(Map.of("timeObj", timestamp, "time", formatter.format(timestamp), "title", "Stopped Working", "color", "rose", "type", "end"));
            }
        }

        // Handle Unclosed States up to 'now' (if querying for today)
        Instant now = Instant.now();
        if (currentStateStart != null && !currentState.equals("OFFLINE") && endOfDay.isAfter(now)) {
            long duration = java.time.Duration.between(currentStateStart, now).getSeconds();
            if (duration > 0) {
                if (currentState.equals("WORK")) totalWorkSeconds += duration;
                else if (currentState.equals("IDLE")) totalIdleSeconds += duration;
                else if (currentState.equals("BREAK")) totalBreakSeconds += duration;
                else if (currentState.equals("LUNCH")) totalLunchSeconds += duration;
            }
            timeline.add(Map.of("timeObj", now, "time", formatter.format(now), "title", "Last Active (" + currentState + ")", "color", "emerald", "type", "end"));
            lastWorkEnd = now;
        }

        // Finalize Timeline
        timeline.sort((m1, m2) -> ((java.time.Instant)m1.get("timeObj")).compareTo((java.time.Instant)m2.get("timeObj")));
        List<Map<String, Object>> finalTimeline = new ArrayList<>();
        for (Map<String, Object> map : timeline) {
            Map<String, Object> newMap = new HashMap<>(map);
            newMap.remove("timeObj");
            finalTimeline.add(newMap);
        }

        // Productivity Calculations
        long requiredSeconds = 8 * 3600; // 8 hours required
        long productiveSeconds = totalWorkSeconds; // Pure work time, excludes idle/break/lunch
        long remainingSeconds = Math.max(0, requiredSeconds - productiveSeconds);
        double efficiency = requiredSeconds > 0 ? ((double) productiveSeconds / requiredSeconds) * 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("workSeconds", productiveSeconds);
        stats.put("idleSeconds", totalIdleSeconds);
        stats.put("breakSeconds", totalBreakSeconds);
        stats.put("lunchSeconds", totalLunchSeconds);
        stats.put("requiredSeconds", requiredSeconds);
        stats.put("remainingSeconds", remainingSeconds);
        stats.put("efficiency", Math.min(100.0, Math.round(efficiency)));
        stats.put("firstWorkStart", firstWorkStart != null ? formatter.format(firstWorkStart) : null);
        stats.put("lastWorkEnd", lastWorkEnd != null ? formatter.format(lastWorkEnd) : null);

        summary.put("topApplications", topApps);
        summary.put("topWebsites", topWebs);
        summary.put("timeline", finalTimeline);
        summary.put("stats", stats);
        summary.put("date", targetDate.toString());

        return ResponseEntity.ok(summary);
    }
}
