
        package org.example.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public class AgentStateManager {

    private static final String DATA_DIRECTORY =
            "C:\\ProgramData\\EmployeeAgent\\data";

    private static final String STATE_FILE =
            "agent-state.json";

    private final Path dataDirectory;
    private final Path stateFile;

    private final ObjectMapper objectMapper;

    private AgentState state;

    public AgentStateManager() {

        dataDirectory =
                Path.of(DATA_DIRECTORY);

        stateFile =
                dataDirectory.resolve(
                        STATE_FILE
                );

        objectMapper = new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        // Store Instant values as ISO-8601 strings
        // instead of numeric epoch timestamps.
        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT
        );

        loadState();
    }

    /**
     * Load the last saved state from disk.
     */
    private synchronized void loadState() {

        try {

            Files.createDirectories(
                    dataDirectory
            );

            if (!Files.exists(stateFile)) {

                state =
                        new AgentState();

                saveState();

                return;
            }

            state =
                    objectMapper.readValue(
                            stateFile.toFile(),
                            AgentState.class
                    );

        } catch (Exception e) {

            System.err.println(
                    "Unable to load agent state: "
                            + e.getMessage()
            );

            state =
                    new AgentState();
        }
    }

    /**
     * Save current state safely.
     *
     * The state is first written to a temporary
     * file and then moved to the real state file.
     */
    public synchronized void saveState() {

        try {

            Files.createDirectories(
                    dataDirectory
            );

            Path temporaryFile =
                    dataDirectory.resolve(
                            "agent-state.tmp"
                    );

            objectMapper.writeValue(
                    temporaryFile.toFile(),
                    state
            );

            try {

                Files.move(
                        temporaryFile,
                        stateFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (
                    java.nio.file.AtomicMoveNotSupportedException e
            ) {

                Files.move(
                        temporaryFile,
                        stateFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException e) {

            System.err.println(
                    "Unable to save agent state: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Return the current local state.
     */
    public synchronized AgentState getState() {

        return state;
    }

    /**
     * Reset the persisted session when a different employee/device logs in.
     *
     * This prevents an ACTIVE session belonging to another employee
     * from being recovered as the current employee's session.
     */
    public synchronized void resetSessionForNewIdentity() {

        state.setSessionStatus("ENDED");

        state.setCurrentState(
                AttendanceState.CLOCKED_OUT.name()
        );

        state.setWorkStartedAt(null);
        state.setLastActivityAt(null);
        state.setLastStateChangeAt(null);

        state.setIdleStartedAt(null);
        state.setBreakStartedAt(null);
        state.setLunchStartedAt(null);

        state.setLastHeartbeatAt(null);

        saveState();
    }

    /**
     * Initialize employee/device information.
     */
    public synchronized void initialize(
            String employeeId,
            String employeeCode,
            String deviceId
    ) {

        state.setEmployeeId(
                employeeId
        );

        state.setEmployeeCode(
                employeeCode
        );

        state.setDeviceId(
                deviceId
        );

        saveState();
    }

    // =========================================================
    // WORK START
    // =========================================================

    /**
     * Backward-compatible method.
     *
     * Uses the current time.
     */
    public synchronized void startWork() {

        startWork(Instant.now());
    }

    /**
     * Persist WORK_STARTED using the supplied event timestamp.
     */
    public synchronized void startWork(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setSessionStatus(
                "ACTIVE"
        );

        state.setCurrentState(
                AttendanceState.WORKING.name()
        );

        state.setWorkStartedAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        state.setLastStateChangeAt(
                now
        );

        state.setIdleStartedAt(
                null
        );

        state.setBreakStartedAt(
                null
        );

        state.setLunchStartedAt(
                null
        );

        saveState();
    }

    // =========================================================
    // IDLE
    // =========================================================

    /**
     * Backward-compatible method.
     */
    public synchronized void startIdle() {

        startIdle(Instant.now());
    }

    /**
     * Persist IDLE_STARTED using the supplied timestamp.
     */
    public synchronized void startIdle(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.IDLE.name()
        );

        state.setIdleStartedAt(
                now
        );

        state.setLastStateChangeAt(
                now
        );

        saveState();
    }

    /**
     * Backward-compatible method.
     */
    public synchronized void endIdle() {

        endIdle(Instant.now());
    }

    /**
     * Persist IDLE_ENDED using the supplied timestamp.
     */
    public synchronized void endIdle(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.WORKING.name()
        );

        state.setIdleStartedAt(
                null
        );

        state.setLastStateChangeAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        saveState();
    }

    // =========================================================
    // BREAK
    // =========================================================

    /**
     * Backward-compatible method.
     */
    public synchronized void startBreak() {

        startBreak(Instant.now());
    }

    /**
     * Persist BREAK_STARTED using the supplied timestamp.
     */
    public synchronized void startBreak(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.BREAK.name()
        );

        state.setBreakStartedAt(
                now
        );

        state.setIdleStartedAt(
                null
        );

        state.setLastActivityAt(
                now
        );

        state.setLastStateChangeAt(
                now
        );

        saveState();
    }

    /**
     * Backward-compatible method.
     */
    public synchronized void endBreak() {

        endBreak(Instant.now());
    }

    /**
     * Persist BREAK_ENDED using the supplied timestamp.
     */
    public synchronized void endBreak(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.WORKING.name()
        );

        state.setBreakStartedAt(
                null
        );

        state.setLastStateChangeAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        saveState();
    }

    // =========================================================
    // LUNCH
    // =========================================================

    /**
     * Backward-compatible method.
     */
    public synchronized void startLunch() {

        startLunch(Instant.now());
    }

    /**
     * Persist LUNCH_STARTED using the supplied timestamp.
     */
    public synchronized void startLunch(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.LUNCH.name()
        );

        state.setLunchStartedAt(
                now
        );

        state.setIdleStartedAt(
                null
        );

        state.setLastActivityAt(
                now
        );

        state.setLastStateChangeAt(
                now
        );

        saveState();
    }

    /**
     * Backward-compatible method.
     */
    public synchronized void endLunch() {

        endLunch(Instant.now());
    }

    /**
     * Persist LUNCH_ENDED using the supplied timestamp.
     */
    public synchronized void endLunch(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.WORKING.name()
        );

        state.setLunchStartedAt(
                null
        );

        state.setLastStateChangeAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        saveState();
    }

    // =========================================================
    // MEETING
    // =========================================================

    /**
     * Backward-compatible method.
     */
    public synchronized void startMeeting() {

        startMeeting(Instant.now());
    }

    /**
     * Persist MEETING_STARTED using the supplied timestamp.
     */
    public synchronized void startMeeting(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.MEETING.name()
        );

        state.setIdleStartedAt(
                null
        );

        state.setLastActivityAt(
                now
        );

        state.setLastStateChangeAt(
                now
        );

        saveState();
    }

    /**
     * Backward-compatible method.
     */
    public synchronized void endMeeting() {

        endMeeting(Instant.now());
    }

    /**
     * Persist MEETING_ENDED using the supplied timestamp.
     */
    public synchronized void endMeeting(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setCurrentState(
                AttendanceState.WORKING.name()
        );

        state.setLastStateChangeAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        saveState();
    }

    // =========================================================
    // HEARTBEAT
    // =========================================================

    /**
     * Persist the latest heartbeat timestamp.
     */
    public synchronized void updateHeartbeat() {

        state.setLastHeartbeatAt(
                Instant.now()
        );

        saveState();
    }

    /**
     * Updates the last real user activity timestamp.
     *
     * This is intentionally separate from heartbeat.
     */
    public synchronized void updateLastActivity(
            Instant timestamp
    ) {

        if (timestamp == null) {
            timestamp = Instant.now();
        }

        state.setLastActivityAt(timestamp);

        saveState();
    }

    // =========================================================
    // ATTENDANCE EVENT -> STATE
    // =========================================================

    /**
     * Persist the state transition represented by an already-created
     * attendance event.
     *
     * The original event timestamp is preserved.
     */
    public synchronized void persistAttendanceEvent(
            AttendanceEvent event
    ) {

        if (event == null ||
                event.getEventType() == null) {

            return;
        }

        Instant timestamp =
                event.getTimestamp()
                        .atZone(
                                java.time.ZoneId.systemDefault()
                        )
                        .toInstant();

        switch (event.getEventType()) {

            case WORK_STARTED ->
                    startWork(timestamp);

            case WORK_ENDED ->
                    endWork(timestamp);

            case IDLE_STARTED ->
                    startIdle(timestamp);

            case IDLE_ENDED ->
                    endIdle(timestamp);

            case BREAK_STARTED ->
                    startBreak(timestamp);

            case BREAK_ENDED ->
                    endBreak(timestamp);

            case LUNCH_STARTED ->
                    startLunch(timestamp);

            case LUNCH_ENDED ->
                    endLunch(timestamp);

            case MEETING_STARTED ->
                    startMeeting(timestamp);

            case MEETING_ENDED ->
                    endMeeting(timestamp);

            case CLOCK_IN ->
                    startWork(timestamp);

            case CLOCK_OUT ->
                    endWork(timestamp);
        }
    }

    // =========================================================
    // WORK END
    // =========================================================

    /**
     * Backward-compatible method.
     */
    public synchronized void endWork() {

        endWork(Instant.now());
    }

    /**
     * Persist WORK_ENDED using the supplied event timestamp.
     */
    public synchronized void endWork(
            Instant timestamp
    ) {

        Instant now =
                timestamp != null
                        ? timestamp
                        : Instant.now();

        state.setSessionStatus(
                "ENDED"
        );

        state.setCurrentState(
                AttendanceState.CLOCKED_OUT.name()
        );

        state.setLastStateChangeAt(
                now
        );

        state.setLastActivityAt(
                now
        );

        state.setIdleStartedAt(
                null
        );

        state.setBreakStartedAt(
                null
        );

        state.setLunchStartedAt(
                null
        );

        saveState();
    }

    // =========================================================
    // RECOVERY
    // =========================================================

    /**
     * Check whether an unfinished session exists.
     */
    public synchronized boolean hasUnfinishedSession() {

        if (state == null) {
            return false;
        }

        String sessionStatus = state.getSessionStatus();

        return "ACTIVE".equalsIgnoreCase(sessionStatus)
                || "RECOVERY_REQUIRED".equalsIgnoreCase(sessionStatus);
    }

    /**
     * Mark the current session as requiring recovery.
     */
    public synchronized void markRecoveryRequired() {

        state.setSessionStatus(
                "RECOVERY_REQUIRED"
        );

        saveState();
    }
}

