package com.example.employee.service;

import com.example.employee.model.EmployeeSession;
import com.example.employee.repository.EmployeeSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class EmployeeSessionService {

    private static final long SESSION_TIMEOUT_SECONDS = 60;

    private final EmployeeSessionRepository sessionRepository;

    public EmployeeSessionService(
            EmployeeSessionRepository sessionRepository) {

        this.sessionRepository = sessionRepository;
    }

    public Optional<EmployeeSession> findActiveSession(
            String employeeId) {

        Optional<EmployeeSession> session =
                sessionRepository.findFirstByEmployeeIdAndStatus(
                        employeeId,
                        "ACTIVE"
                );

        if (session.isEmpty()) {
            return Optional.empty();
        }

        EmployeeSession existingSession = session.get();

        if (isSessionExpired(existingSession)) {

            existingSession.setStatus("EXPIRED");

            sessionRepository.save(existingSession);

            return Optional.empty();
        }

        return Optional.of(existingSession);
    }

    public EmployeeSession createSession(
            String employeeId,
            String employeeCode,
            String deviceId) {

        Instant now = Instant.now();

        EmployeeSession session = new EmployeeSession();

        session.setEmployeeId(employeeId);
        session.setEmployeeCode(employeeCode);
        session.setDeviceId(deviceId);
        session.setLoginAt(now);
        session.setLastHeartbeatAt(now);
        session.setStatus("ACTIVE");

        return sessionRepository.save(session);
    }

    public void updateHeartbeat(String deviceId) {

        Optional<EmployeeSession> session =
                sessionRepository.findByDeviceIdAndStatus(
                        deviceId,
                        "ACTIVE"
                );

        if (session.isPresent()) {

            EmployeeSession activeSession = session.get();

            activeSession.setLastHeartbeatAt(
                    Instant.now()
            );

            sessionRepository.save(activeSession);
        }
    }

    public void logout(String deviceId) {

        Optional<EmployeeSession> session =
                sessionRepository.findByDeviceIdAndStatus(
                        deviceId,
                        "ACTIVE"
                );

        if (session.isPresent()) {

            EmployeeSession activeSession = session.get();

            activeSession.setStatus("LOGGED_OUT");

            sessionRepository.save(activeSession);
        }
    }

    private boolean isSessionExpired(
            EmployeeSession session) {

        if (session.getLastHeartbeatAt() == null) {
            return true;
        }

        long inactiveSeconds =
                Duration.between(
                        session.getLastHeartbeatAt(),
                        Instant.now()
                ).getSeconds();

        return inactiveSeconds > SESSION_TIMEOUT_SECONDS;
    }
}