package com.example.employee.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.example.employee.model.Employee;
import com.example.employee.model.WorkSession;
import com.example.employee.model.WorkSession.BreakPeriod;
import com.example.employee.repository.WorkSessionRepository;

@Service
public class WorkSessionService {

    private final WorkSessionRepository repo;
    private final MongoTemplate mongoTemplate;
    private final EmployeeService employeeService;

    @Autowired
    private MailService mailService;

    public WorkSessionService(WorkSessionRepository repo,
            MongoTemplate mongoTemplate,
            EmployeeService employeeService) {
        this.repo = repo;
        this.mongoTemplate = mongoTemplate;
        this.employeeService = employeeService;
    }

   public WorkSession clockIn(String employeeCode, String employeeEmail) {
    LocalDate today = LocalDate.now();
    WorkSession session = repo.findByEmployeeCodeAndDate(employeeCode, today);

    // If session already exists, return it (no duplicate clock-ins)
    if (session != null) {
        return session;
    }

    // Fetch employee info
    Employee emp = employeeService.findByEmployeeCode(employeeCode);
    if (emp == null) {
        throw new RuntimeException("Employee not found with code: " + employeeCode);
    }

    // Create new session
    session = new WorkSession();
    session.setEmployeeCode(emp.getEmployeeCode());
    session.setEmail(emp.getEmail());
    session.setDate(today);
    session.setClockIn(LocalDateTime.now());

    // Add extended metadata
    session.setDepartmentId(emp.getDepartmentId());
    session.setDepartmentName(emp.getDepartmentName());
    session.setTeamId(emp.getTeamId());
    session.setTeamName(emp.getTeamName());
    session.setPhone(emp.getPhone());
    session.setDesignation(emp.getDesignation());

    repo.save(session);

    // Late arrival check (after 09:15 AM)
    LocalTime now = LocalTime.now();
    LocalTime expectedTime = LocalTime.of(9, 15);

    if (now.isAfter(expectedTime)) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a"); // e.g., 09:43 AM
        String formattedTime = now.format(timeFormatter);

        String subject = "Late Arrival Notification";
        String body = "Dear " + emp.getName() + ",\n\n"
                + "You have clocked in late at " + formattedTime + ". "
                + "Please ensure timely attendance in the future.\n\n"
                + "Regards,\nHR Department";

        if (emp.getEmail() != null && !emp.getEmail().isEmpty()) {
            mailService.sendMail(emp.getEmail(), subject, body);
        }
    }

    return session;
}

    public WorkSession clockOut(String employeeCode) {
        WorkSession session = getTodaySession(employeeCode);
        if (session != null && session.getClockOut() == null) {
            session.setClockOut(LocalDateTime.now());
            repo.save(session);
        }
        return session;
    }

    public WorkSession startBreak(String employeeCode) {
        WorkSession session = getTodaySession(employeeCode);
        if (session != null) {
            BreakPeriod bp = new BreakPeriod();
            bp.setBreakStart(LocalDateTime.now());
            session.getBreaks().add(bp);
            repo.save(session);
        }
        return session;
    }

    public WorkSession endBreak(String employeeCode) {
        WorkSession session = getTodaySession(employeeCode);
        if (session != null && !session.getBreaks().isEmpty()) {
            BreakPeriod lastBreak = session.getBreaks().get(session.getBreaks().size() - 1);
            if (lastBreak.getBreakEnd() == null) {
                lastBreak.setBreakEnd(LocalDateTime.now());
                repo.save(session);
            }
        }
        return session;
    }

    private WorkSession getTodaySession(String employeeCode) {
        return repo.findByEmployeeCodeAndDate(employeeCode, LocalDate.now());
    }

    public List<WorkSession> getWorkHistory(String employeeCode) {
        return repo.findByEmployeeCodeOrderByDateDesc(employeeCode);
    }

    public boolean isClockedIn(String employeeCode) {
        WorkSession session = getTodaySession(employeeCode);
        return session != null && session.getClockOut() == null;
    }

    public Duration getTodayWorkDuration(String employeeCode) {
        WorkSession session = getTodaySession(employeeCode);
        if (session != null && session.getClockIn() != null) {
            return Duration.between(session.getClockIn(), session.getClockOut() != null ? session.getClockOut() : LocalDateTime.now());
        }
        return Duration.ZERO;
    }

    public List<WorkSession> findAllSessions() {
        return repo.findAll();
    }

    public List<WorkSession> findAllLateArrivals() {
        return repo.findAll().stream()
                .filter(ws -> ws.getClockIn() != null && ws.getClockIn().toLocalTime().isAfter(LocalDateTime.of(0, 0, 0, 9, 15).toLocalTime()))
                .collect(Collectors.toList());
    }

    public List<WorkSession> findSessionsFiltered(String departmentId, String teamId, String employeeCode,
            LocalDate startDate, LocalDate endDate) {
        Query query = new Query();

        if (departmentId != null && !departmentId.isEmpty()) {
            query.addCriteria(Criteria.where("departmentId").is(departmentId));
        }

        if (teamId != null && !teamId.isEmpty()) {
            query.addCriteria(Criteria.where("teamId").is(teamId));
        }

        if (employeeCode != null && !employeeCode.isEmpty()) {
            query.addCriteria(Criteria.where("employeeCode").is(employeeCode));
        }

        if (startDate != null || endDate != null) {
            Criteria dateCriteria = Criteria.where("date");
            if (startDate != null && endDate != null) {
                dateCriteria.gte(startDate).lte(endDate);
            } else if (startDate != null) {
                dateCriteria.gte(startDate);
            } else {
                dateCriteria.lte(endDate);
            }
            query.addCriteria(dateCriteria);
        }

        return mongoTemplate.find(query, WorkSession.class);
    }

     public WorkSession save(WorkSession session) {
        return repo.save(session);
    }
}
