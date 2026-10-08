package com.example.employee.controller;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.employee.model.Employee;
import com.example.employee.model.Screenshot;
import com.example.employee.model.ScreenshotMonitor;
import com.example.employee.repository.EmployeeRepository;
import com.example.employee.repository.ScreenshotMonitorRepository;
import com.example.employee.repository.ScreenshotRepository;
import com.example.employee.service.EmployeeService;

@Controller
@RequestMapping("/admin")
public class ScreenshotMonitorController {

    @Autowired
    private ScreenshotMonitorRepository screenshotRepo;

    @Autowired
    private ScreenshotRepository screenshotRepository;

    @Autowired
    private EmployeeService employeeService;

      @Autowired
    private EmployeeRepository employeeRepository;

    // Start monitoring
    @PostMapping("/screenshot/start")
    public String startMonitoring(@RequestParam String employeeId,
                                  @RequestParam(defaultValue = "10") int interval) {
        ScreenshotMonitor monitor = new ScreenshotMonitor(employeeId, true, interval);
        screenshotRepo.save(monitor);
        return "redirect:/admin/screenshots";
    }

    // Stop monitoring
    @PostMapping("/screenshot/stop")
    public String stopMonitoring(@RequestParam String employeeId) {
        screenshotRepo.deleteByEmployeeId(employeeId);
        return "redirect:/admin/screenshots";
    }

    // Agent polling API
    @GetMapping("/screenshot/status")
    public ResponseEntity<Map<String, Object>> getMonitorStatus(@RequestParam String employeeId) {
        Map<String, Object> response = new HashMap<>();
        if (employeeId == null || employeeId.trim().isEmpty()) {
            response.put("active", false);
            response.put("interval", 0);
            response.put("error", "Invalid employeeId");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<ScreenshotMonitor> monitorOpt = screenshotRepo.findByEmployeeId(employeeId);
        if (monitorOpt.isPresent()) {
            ScreenshotMonitor monitor = monitorOpt.get();
            response.put("active", monitor.isActive());
            response.put("interval", monitor.getIntervalMinutes());
        } else {
            response.put("active", false);
            response.put("interval", 0);
        }

        return ResponseEntity.ok(response);
    }

    // Upload screenshot
    @PostMapping("/screenshot/upload")
    public ResponseEntity<String> handleScreenshotUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("employeeId") String employeeId) {
        try {
            byte[] imageData = file.getBytes();
            Screenshot screenshot = new Screenshot(employeeId, imageData, new Date());
            screenshotRepository.save(screenshot);
            return ResponseEntity.ok("✅ Screenshot uploaded for employeeId: " + employeeId);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("❌ Upload failed: " + e.getMessage());
        }
    }

    // View all monitors
    @GetMapping("/screenshot/all")
    public List<ScreenshotMonitor> getAllMonitored() {
        return screenshotRepo.findAll();
    }

    // Admin screenshot UI page
    @GetMapping("/screenshots")
    public String showScreenshotPage(Model model) {
        List<Employee> allEmployees = employeeService.findAll();
        List<ScreenshotMonitor> activeMonitors = screenshotRepo.findAll();

        Set<String> monitoredIds = activeMonitors.stream()
                .filter(ScreenshotMonitor::isActive)
                .map(ScreenshotMonitor::getEmployeeId)
                .collect(Collectors.toSet());

        List<Employee> availableEmployees = allEmployees.stream()
                .filter(emp -> !monitoredIds.contains(emp.getId()))
                .collect(Collectors.toList());

        Map<String, String> employeeNamesMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));

        Map<String, Employee> employeeById = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getId, emp -> emp));

        model.addAttribute("employeeById", employeeById);
        model.addAttribute("employees", availableEmployees);
        model.addAttribute("activeMonitors", activeMonitors);
        model.addAttribute("employeeNamesMap", employeeNamesMap);

        return "admin/screenshot";
    }

    // View screenshots for employee
  @GetMapping("/screenshot/view")
public String viewScreenshots(@RequestParam("employeeId") String employeeId, Model model) {
    List<Screenshot> screenshots = screenshotRepository.findByEmployeeId(employeeId);

    List<Map<String, String>> base64Screenshots = new ArrayList<>();
    for (Screenshot shot : screenshots) {
        Map<String, String> entry = new HashMap<>();
        entry.put("image", Base64.getEncoder().encodeToString(shot.getImageData()));

        // ✅ Optional: Format timestamp nicely
        String formattedTime = new java.text.SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss z")
                .format(shot.getTimestamp());

        entry.put("time", formattedTime);
        entry.put("employeeId", shot.getEmployeeId()); // 🔁 Add this too
        base64Screenshots.add(entry);
    }

    Employee employee = employeeService.findById(employeeId);

    model.addAttribute("employeeId", employeeId);
    model.addAttribute("employeeName", employee != null ? employee.getName() : "Unknown");
    model.addAttribute("screenshots", base64Screenshots);
    return "admin/screenshot_view";
}
@GetMapping("/api/employee/id-by-code")
public ResponseEntity<String> getEmployeeIdByCode(@RequestParam String code) {
    Optional<Employee> emp = employeeRepository.findByEmployeeCode(code);
    return emp.map(employee -> ResponseEntity.ok(employee.getId()))
              .orElseGet(() -> ResponseEntity.status(404).body("Employee not found"));
}


}
