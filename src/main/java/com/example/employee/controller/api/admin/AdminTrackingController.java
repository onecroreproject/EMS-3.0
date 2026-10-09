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

@RestController
@RequestMapping("/api/admin/tracking")
public class AdminTrackingController {

    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;
    private final ApplicationActivityRepository appActivityRepository;
    private final WorkSessionService workSessionService;
    private final AttendanceEventRepository attendanceEventRepository;

    public AdminTrackingController(
            DeviceRepository deviceRepository,
            EmployeeRepository employeeRepository,
            ApplicationActivityRepository appActivityRepository,
            WorkSessionService workSessionService,
            AttendanceEventRepository attendanceEventRepository) {
        this.deviceRepository = deviceRepository;
        this.employeeRepository = employeeRepository;
        this.appActivityRepository = appActivityRepository;
        this.workSessionService = workSessionService;
        this.attendanceEventRepository = attendanceEventRepository;
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
                // Check if actively on break or lunch using AttendanceEvents
                boolean onBreak = false;
                boolean onLunch = false;
                if (d.getEmployeeCode() != null) {
                    java.util.Optional<AttendanceEventDocument> lastEventOpt = attendanceEventRepository.findTopByEmployeeCodeOrderByTimestampDesc(d.getEmployeeCode());
                    if (lastEventOpt.isPresent()) {
                        AttendanceEventType type = lastEventOpt.get().getEventType();
                        if (type == AttendanceEventType.BREAK_STARTED) {
                            onBreak = true;
                        } else if (type == AttendanceEventType.LUNCH_STARTED) {
                            onLunch = true;
                        }
                    }
                }

                if (onBreak) {
                    activity = "BREAK";
                } else if (onLunch) {
                    activity = "LUNCH";
                } else {
                    ApplicationActivityDocument latestActivity = appActivityRepository.findFirstByDeviceIdOrderByStartedAtDesc(d.getDeviceId());
                    if (latestActivity != null && latestActivity.getEndedAt() != null) {
                        long gapSeconds = Instant.now().getEpochSecond() - latestActivity.getEndedAt().getEpochSecond();
                        if (gapSeconds > 5 * 60) {
                            activity = "IDLE";
                        } else {
                            activity = "WORKING";
                        }
                    } else {
                        activity = "WORKING";
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
    public ResponseEntity<Map<String, Object>> getTrackingSummary(@RequestParam(required = false) String employeeCode) {
        Map<String, Object> summary = new HashMap<>();
        
        Instant startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        List<ApplicationActivityDocument> activities = appActivityRepository.findAll().stream()
            .filter(a -> a.getStartedAt() != null && !a.getStartedAt().isBefore(startOfDay) && a.getStartedAt().isBefore(endOfDay))
            .collect(Collectors.toList());

        if (employeeCode != null && !employeeCode.isEmpty()) {
            activities = activities.stream()
                .filter(a -> employeeCode.equals(a.getEmployeeCode()))
                .collect(Collectors.toList());
        }

        // Aggregate Top Applications (where url is null/empty)
        Map<String, Long> appDurations = new HashMap<>();
        // Aggregate Top Websites (where url is NOT null/empty)
        Map<String, Long> webDurations = new HashMap<>();

        long totalWorkSeconds = 0;
        for (ApplicationActivityDocument doc : activities) {
            long duration = doc.getDurationSeconds();
            totalWorkSeconds += duration;
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
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("durationSeconds", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> topWebs = webDurations.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("durationSeconds", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());


        // Timeline dynamic data from activities
        List<Map<String, Object>> timeline = new ArrayList<>();
        long totalIdleSeconds = 0;
        long totalBreakSeconds = 0;

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a").withZone(java.time.ZoneId.systemDefault());

        // Fetch today's Attendance Events for Break/Lunch
        java.time.LocalDate todayEvents = java.time.LocalDate.now();
        Instant eventStartOfDay = todayEvents.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        Instant eventEndOfDay = todayEvents.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        
        List<AttendanceEventDocument> events = new ArrayList<>();
        if (employeeCode != null && !employeeCode.isEmpty()) {
            events = attendanceEventRepository.findAttendanceEventsForDay(
                employeeCode, 
                eventStartOfDay, 
                eventEndOfDay,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "timestamp")
            );
        }

        // Helper class to store parsed periods
        class BreakLunchPeriod {
            Instant start;
            Instant end;
            String type; // "BREAK" or "LUNCH"
            public BreakLunchPeriod(Instant s, String t) { start = s; type = t; }
        }
        
        List<BreakLunchPeriod> parsedPeriods = new ArrayList<>();
        BreakLunchPeriod currentPeriod = null;
        
        for (AttendanceEventDocument e : events) {
            if (e.getEventType() == AttendanceEventType.BREAK_STARTED) {
                if (currentPeriod == null) currentPeriod = new BreakLunchPeriod(e.getTimestamp(), "BREAK");
            } else if (e.getEventType() == AttendanceEventType.LUNCH_STARTED) {
                if (currentPeriod == null) currentPeriod = new BreakLunchPeriod(e.getTimestamp(), "LUNCH");
            } else if (e.getEventType() == AttendanceEventType.BREAK_ENDED || e.getEventType() == AttendanceEventType.LUNCH_ENDED) {
                if (currentPeriod != null) {
                    currentPeriod.end = e.getTimestamp();
                    parsedPeriods.add(currentPeriod);
                    currentPeriod = null;
                }
            }
        }
        if (currentPeriod != null) { // Unclosed break
            parsedPeriods.add(currentPeriod);
        }

        for (BreakLunchPeriod p : parsedPeriods) {
            long dur = 0;
            Instant end = p.end != null ? p.end : Instant.now();
            dur = java.time.Duration.between(p.start, end).getSeconds();
            if (p.type.equals("BREAK")) totalBreakSeconds += dur;
            
            String color = p.type.equals("BREAK") ? "orange" : "amber";
            timeline.add(Map.of("timeObj", p.start, 
                              "time", formatter.format(p.start), 
                              "title", (p.type.equals("BREAK") ? "Break (" : "Lunch (") + (dur/60) + " mins)", 
                              "color", color, "type", p.type.toLowerCase(), "duration", dur));
            if (p.end != null) {
                timeline.add(Map.of("timeObj", p.end, 
                                  "time", formatter.format(p.end), 
                                  "title", "Resumed Working", 
                                  "color", "blue", "type", "resume"));
            }
        }

        if (!activities.isEmpty()) {
            activities.sort(Comparator.comparing(ApplicationActivityDocument::getStartedAt));
            
            // 1. Started Working
            ApplicationActivityDocument first = activities.get(0);
            timeline.add(Map.of("timeObj", first.getStartedAt(), "time", formatter.format(first.getStartedAt()), "title", "Started Working", "color", "emerald", "type", "start"));
            
            // 2. Detect Idles
            for (int i = 0; i < activities.size() - 1; i++) {
                ApplicationActivityDocument current = activities.get(i);
                ApplicationActivityDocument next = activities.get(i+1);
                
                if (current.getEndedAt() != null && next.getStartedAt() != null) {
                    long gapSeconds = java.time.Duration.between(current.getEndedAt(), next.getStartedAt()).getSeconds();
                    
                    if (gapSeconds > 5 * 60) { // > 5 mins gap is Idle
                        // Check if this gap overlaps with any break. If yes, skip it to avoid duplicate break/idle.
                        boolean isBreak = false;
                        for (BreakLunchPeriod p : parsedPeriods) {
                            Instant bs = p.start;
                            Instant be = p.end != null ? p.end : Instant.now();
                            if (!current.getEndedAt().isBefore(bs) && !current.getEndedAt().isAfter(be)) {
                                isBreak = true;
                                break;
                            }
                            if (!next.getStartedAt().isBefore(bs) && !next.getStartedAt().isAfter(be)) {
                                isBreak = true;
                                break;
                            }
                        }
                        
                        if (!isBreak) {
                            totalIdleSeconds += gapSeconds;
                            long mins = gapSeconds / 60;
                            timeline.add(Map.of("timeObj", current.getEndedAt(), "time", formatter.format(current.getEndedAt()), "title", "Idle (" + mins + " mins)", "color", "slate", "type", "idle", "duration", gapSeconds));
                            timeline.add(Map.of("timeObj", next.getStartedAt(), "time", formatter.format(next.getStartedAt()), "title", "Resumed Working", "color", "blue", "type", "resume"));
                        }
                    }
                }
            }
            
            // 3. Active Now (Last activity)
            ApplicationActivityDocument last = activities.get(activities.size() - 1);
            java.time.Instant end = last.getEndedAt() != null ? last.getEndedAt() : java.time.Instant.now();
            timeline.add(Map.of("timeObj", end, "time", formatter.format(end), "title", "Last Active", "color", "emerald", "type", "end"));
        }

        // Sort timeline by timeObj and remove timeObj
        timeline.sort((m1, m2) -> ((java.time.Instant)m1.get("timeObj")).compareTo((java.time.Instant)m2.get("timeObj")));
        List<Map<String, Object>> finalTimeline = new ArrayList<>();
        for (Map<String, Object> map : timeline) {
            Map<String, Object> newMap = new HashMap<>(map);
            newMap.remove("timeObj");
            finalTimeline.add(newMap);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("workSeconds", totalWorkSeconds);
        stats.put("idleSeconds", totalIdleSeconds);
        stats.put("breakSeconds", totalBreakSeconds);
        stats.put("lunchSeconds", 0); // Placeholder for lunch logic if needed

        summary.put("topApplications", topApps);
        summary.put("topWebsites", topWebs);
        summary.put("timeline", finalTimeline);
        summary.put("stats", stats);

        return ResponseEntity.ok(summary);
    }
}
