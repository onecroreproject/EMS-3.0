package org.example;

import org.example.activity.ActiveWindowService;
import org.example.activity.ApplicationActivitySender;
import org.example.activity.ApplicationActivityTracker;
import org.example.activity.ApplicationInfo;
import org.example.activity.UserActivityMonitor;
import org.example.activity.request.ApplicationActivityRequest;
import org.example.attendance.*;
import org.example.commucnication.DeviceRegistrationService;
import org.example.device.DeviceIdService;
import org.example.heartbeat.HeartbeatService;
import org.example.ui.EmployeeWorkspaceWindow;
import org.example.ui.LoginPopupManager;
import org.example.ui.LoginWindow;
import org.example.config.AgentConfig;
import org.example.commucnication.AgentLoginService;

// OTA support
import org.example.ota.OtaUpdateService;
import org.example.ota.UpdateChecker;
import org.example.ota.UpdateMetadata;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

import javax.swing.*;

public class AgentApplication {

    /*
     * =========================================================
     * GLOBAL SESSION STATE
     * =========================================================
     */

    private static volatile boolean sessionRunning = false;

    private static volatile boolean autoWorkEndTriggered = false;

    private static volatile boolean workEndedSent = false;

    private static volatile boolean breakRunning = false;

    private static volatile boolean lunchRunning = false;

    /** True only while a backend EmployeeTaskWork session is running. */
    private static volatile boolean taskWorkRunning = false;

    /** True when the agent is operating without EMS server connectivity. */
    private static volatile boolean offlineMode = false;

    /*
     * =========================================================
     * LOCAL STATE
     * =========================================================
     */

    private static volatile AgentStateManager agentStateManager;

    private static volatile AttendanceEventManager attendanceEventManager;

    /** Global attendance queue shared by the agent and workspace. */
    private static volatile AttendanceEventQueue attendanceEventQueue;

    private static volatile long lastActivityStatePersistMillis = 0L;

    /**
     * Last attendance state printed to the terminal.
     * Prevents continuous duplicate state logs.
     */
    private static volatile String lastLoggedActivityState = null;


    /*
     * =========================================================
     * ATTENDANCE STATE
     * =========================================================
     */

    public static void persistAttendanceState(
            AttendanceEvent event) {

        if (attendanceEventManager != null) {

            attendanceEventManager.persistAttendanceEvent(
                    event
            );
        }
    }


    /**
     * Queue the exact same attendance event for retry.
     * SUCCESS and REJECTED events must never call this method.
     */
    public static void queueAttendanceEvent(AttendanceEvent event) {

        if (event == null) {
            System.out.println("Cannot queue attendance event: event is null.");
            return;
        }

        if (attendanceEventQueue == null) {
            System.out.println(
                    "Cannot queue attendance event: attendance queue is not initialized."
            );
            return;
        }

        attendanceEventQueue.add(event);

        System.out.println("Attendance event queued for retry.");
    }


    /*
     * =========================================================
     * OFFLINE MODE
     * =========================================================
     */

    public static boolean isOfflineMode() {

        return offlineMode;
    }


    public static void setOfflineMode(
            boolean offlineMode) {

        AgentApplication.offlineMode =
                offlineMode;
    }


    /**
     * =========================================================
     * ACTIVITY STATE LOGGING
     * =========================================================
     *
     * AttendanceEventManager is the source of truth for the
     * attendance state shown in the terminal.
     *
     * The activity monitor must not continuously print the same
     * state every second.
     */
    private static void logActivityStateChange(
            String currentState) {

        if (currentState == null) {
            return;
        }

        if (!currentState.equals(lastLoggedActivityState)) {

            System.out.println(
                    "Activity state: "
                            + currentState
            );

            lastLoggedActivityState =
                    currentState;
        }
    }


    /*
     * =========================================================
     * MAIN
     * =========================================================
     */

    public static void main(String[] args) {

        System.out.println("=================================");
        System.out.println(" Employee Monitoring Agent");
        System.out.println(" Agent started successfully");
        System.out.println(
                " Agent Version: "
                        + AgentVersion.VERSION
        );
        System.out.println("=================================");

        /*
         * =========================
         * Single Instance Lock
         * =========================
         */
        try {
            java.io.File file = new java.io.File(System.getProperty("java.io.tmpdir"), "ems-agent.lock");
            java.io.RandomAccessFile randomAccessFile = new java.io.RandomAccessFile(file, "rw");
            java.nio.channels.FileChannel fileChannel = randomAccessFile.getChannel();
            java.nio.channels.FileLock lock = fileChannel.tryLock();

            if (lock == null) {
                System.out.println("Another instance of EMS Agent is already running. Exiting.");
                javax.swing.JOptionPane.showMessageDialog(null,
                        "EMS Agent is already running.",
                        "Agent Running",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println("Failed to acquire application lock: " + e.getMessage());
        }


        /*
         * =========================
         * Load Configuration
         * =========================
         */

        AgentConfig config =
                new AgentConfig();


        String serverUrl =
                config.getServerUrl();


        /*
         * OTA check is handled from
         * EmployeeWorkspaceWindow.
         */

        System.out.println(
                "Server URL: "
                        + serverUrl
        );

        /*
         * Fetch dynamic config from backend (e.g. idle tracking policies)
         */
        config.fetchDynamicConfig(serverUrl);


        /*
         * =========================
         * Generate Device ID
         * =========================
         */

        DeviceIdService deviceIdService =
                new DeviceIdService();


        String deviceId =
                deviceIdService.generateDeviceId();


        if (deviceId == null) {

            System.out.println(
                    "Unable to generate Device ID."
            );

            return;
        }


        System.out.println(
                "Device ID: "
                        + deviceId
        );


        /*
         * =========================
         * Install Type & Version Check
         * =========================
         */
        try {
            java.nio.file.Path otaMarker = java.nio.file.Path.of("C:\\ProgramData\\EmployeeAgent\\ota-update.marker");
            java.nio.file.Path versionFile = java.nio.file.Path.of("C:\\ProgramData\\EmployeeAgent\\installed.version");
            boolean isOta = java.nio.file.Files.exists(otaMarker);
            
            String lastVersion = "";
            if (java.nio.file.Files.exists(versionFile)) {
                lastVersion = java.nio.file.Files.readString(versionFile).trim();
            }
            
            String currentVersion = org.example.AgentVersion.VERSION;
            
            if (!currentVersion.equals(lastVersion)) {
                if (!isOta) {
                    System.out.println("Manual install or upgrade detected. Clearing saved credentials.");
                    org.example.security.WindowsCredentialManager.clearCredentials();
                } else {
                    System.out.println("OTA upgrade detected. Preserving credentials.");
                    java.nio.file.Files.deleteIfExists(otaMarker);
                }
                java.nio.file.Files.writeString(versionFile, currentVersion, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                if (isOta) {
                    java.nio.file.Files.deleteIfExists(otaMarker);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process installation version check: " + e.getMessage());
        }


        /*
         * =========================
         * Login Window
         * =========================
         */

        SwingUtilities.invokeLater(() -> {

            final LoginWindow[] loginWindowHolder =
                    new LoginWindow[1];


            /*
             * =========================================================
             * LOGIN REMINDER MANAGER
             * =========================================================
             */

            final LoginPopupManager loginPopupManager =
                    new LoginPopupManager(
                            deviceId
                    );
            loginPopupManager.setReminderIntervalMs(config.getLoginReminderIntervalSeconds() * 1000);


            /*
             * =========================================================
             * INITIAL LOGIN WINDOW
             * =========================================================
             */

            loginWindowHolder[0] =
                    new LoginWindow(
                            deviceId,
                            loginResult -> {

                                System.out.println("Login successful!");
                                org.example.config.AgentTokenHolder.setToken(loginResult.getToken());
                                org.example.config.AgentTokenHolder.setRefreshToken(loginResult.getRefreshToken());


                                /*
                                 * Stop all login reminders.
                                 */

                                loginPopupManager.stop();


                                String employeeId =
                                        loginResult.getEmployeeId();


                                String employeeCode =
                                        loginResult.getEmployeeCode();


                                System.out.println(
                                        "Employee ID: "
                                                + employeeId
                                );


                                System.out.println(
                                        "INFO Employee authenticated: "
                                                + employeeCode
                                );


                                /*
                                 * =========================================================
                                 * INITIALIZE LOCAL AGENT STATE
                                 * =========================================================
                                 */

                                AgentStateManager localStateManager =
                                        new AgentStateManager();


                                AgentState previousState =
                                        localStateManager.getState();


                                boolean sameIdentity =
                                        previousState != null
                                                && employeeCode != null
                                                && employeeCode.equals(
                                                previousState
                                                        .getEmployeeCode()
                                        )
                                                && deviceId != null
                                                && deviceId.equals(
                                                previousState
                                                        .getDeviceId()
                                        );


                                boolean recoveryRequired =
                                        sameIdentity
                                                && localStateManager
                                                .hasUnfinishedSession();


                                if (!sameIdentity) {

                                    localStateManager
                                            .resetSessionForNewIdentity();
                                }


                                localStateManager.initialize(
                                        employeeId,
                                        employeeCode,
                                        deviceId
                                );


                                if (recoveryRequired) {

                                    localStateManager
                                            .markRecoveryRequired();


                                    AgentState recoveryState =
                                            localStateManager.getState();


                                    String message =
                                            "A previous Employee Monitoring session "
                                                    + "was not closed normally.\n\n"
                                                    + "Last state: "
                                                    + recoveryState
                                                    .getCurrentState()
                                                    + "\n"
                                                    + "Last activity: "
                                                    + recoveryState
                                                    .getLastActivityAt()
                                                    + "\n\n"
                                                    + "Start a new work session?";


                                    int choice = JOptionPane.YES_OPTION;
                                    
                                    if (loginWindowHolder[0].isVisible()) {
                                        choice = JOptionPane.showConfirmDialog(
                                                null,
                                                message,
                                                "Previous Session Recovery",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.WARNING_MESSAGE
                                        );
                                    }

                                    if (choice != JOptionPane.YES_OPTION) {

                                        System.out.println(
                                                "Login cancelled by recovery prompt."
                                        );

                                        return;
                                    }
                                }


                                agentStateManager =
                                        localStateManager;


                                /*
                                 * =========================
                                 * Device Registration
                                 * =========================
                                 */

                                boolean offlineMode =
                                        loginResult.getStatus()
                                                == AgentLoginService
                                                .LoginStatus
                                                .OFFLINE_SUCCESS
                                                && loginResult
                                                .getEmployeeName()
                                                == null;


                                if (offlineMode) {

                                    AgentApplication
                                            .setOfflineMode(true);


                                    System.out.println(
                                            "Offline mode detected."
                                    );


                                    System.out.println(
                                            "Device registration skipped because EMS server is unavailable."
                                    );


                                    System.out.println(
                                            "Continuing in OFFLINE mode."
                                    );

                                } else {

                                    AgentApplication
                                            .setOfflineMode(false);


                                    DeviceRegistrationService
                                            registrationService =
                                            new DeviceRegistrationService(
                                                    serverUrl,
                                                    deviceId
                                            );


                                    boolean registered =
                                            registrationService
                                                    .registerDevice(
                                                            employeeId,
                                                            employeeCode,
                                                            loginResult.getToken()
                                                    );


                                    if (!registered) {

                                        System.out.println(
                                                "Device registration failed."
                                        );

                                        return;
                                    }


                                    System.out.println(
                                            "Device registration successful."
                                    );
                                }


                                /*
                                 * =========================
                                 * Attendance Manager
                                 * =========================
                                 */

                                AttendanceEventFactory
                                        eventFactory =
                                        new AttendanceEventFactory(
                                                employeeCode,
                                                deviceId
                                        );


                                AttendanceEventManager
                                        eventManager =
                                        new AttendanceEventManager(
                                                eventFactory,
                                                agentStateManager
                                        );


                                attendanceEventManager =
                                        eventManager;


                                /*
                                 * Hide Login Window Immediately for a smooth transition
                                 */
                                loginWindowHolder[0].dispose();

                                /*
                                 * =========================
                                 * Employee Workspace
                                 * =========================
                                 */
                                long workStartedAtMillis = System.currentTimeMillis();

                                EmployeeWorkspaceWindow workspaceWindow =
                                        new EmployeeWorkspaceWindow(
                                                loginResult,
                                                deviceId,
                                                workStartedAtMillis
                                        );

                                /*
                                 * Show Workspace
                                 */

                                workspaceWindow
                                        .setVisible(true);


                                /*
                                 * =========================
                                 * Application Tracking
                                 * =========================
                                 */

                                ActiveWindowService
                                        activeWindowService =
                                        new ActiveWindowService();


                                ApplicationActivitySender
                                        applicationActivitySender =
                                        new ApplicationActivitySender(
                                                serverUrl
                                        );


                                ApplicationActivityTracker
                                        applicationActivityTracker =
                                        new ApplicationActivityTracker(
                                                activeWindowService,
                                                applicationActivitySender,
                                                employeeCode,
                                                deviceId
                                        );


                                /*
                                 * =========================
                                 * Attendance Tracking
                                 * =========================
                                 */

                                attendanceEventQueue =
                                        new AttendanceEventQueue();


                                AttendanceEventSender
                                        eventSender =
                                        new AttendanceEventSender(
                                                serverUrl
                                        );


                                /*
                                 * =========================
                                 * Queue Processor
                                 * =========================
                                 */

                                AttendanceEventQueueProcessor
                                        queueProcessor =
                                        new AttendanceEventQueueProcessor(
                                                attendanceEventQueue,
                                                eventSender
                                        );


                                /*
                                 * =========================
                                 * WORK_STARTED
                                 * =========================
                                 */

                                autoWorkEndTriggered =
                                        false;

                                workEndedSent =
                                        false;


                                AttendanceEvent
                                        workStartedEvent =
                                        eventManager.startWork();


                                sessionRunning = true;

                                new Thread(() -> {
                                    AttendanceSendResult
                                            eventResult =
                                            eventSender.sendEvent(
                                                    workStartedEvent
                                            );


                                    if (
                                            eventResult
                                                    == AttendanceSendResult.SUCCESS
                                    ) {

                                        System.out.println(
                                                "WORK_STARTED event sent successfully."
                                        );

                                    } else if (
                                            eventResult
                                                    == AttendanceSendResult.REJECTED
                                    ) {

                                        System.out.println(
                                                "WORK_STARTED event was rejected by server. "
                                                        + "Event will NOT be queued."
                                        );
                                        
                                        sessionRunning = false;

                                    } else {

                                        System.out.println(
                                                "WORK_STARTED event failed to send. "
                                                        + "Event will be queued for retry."
                                        );


                                        attendanceEventQueue.add(
                                                workStartedEvent
                                        );
                                    }
                                }).start();


                                /*
                                 * =========================================================
                                 * APPLICATION ACTIVITY THREAD
                                 * =========================================================
                                 */

                                Thread applicationActivityThread =
                                        new Thread(() -> {

                                            while (sessionRunning) {

                                                try {

                                                    ApplicationInfo
                                                            completedActivity =
                                                            applicationActivityTracker
                                                                    .checkActivity();


                                                    if (
                                                            completedActivity
                                                                    != null
                                                    ) {

                                                        ApplicationActivityRequest
                                                                request =
                                                                new ApplicationActivityRequest(
                                                                        employeeCode,
                                                                        deviceId,
                                                                        completedActivity
                                                                );


                                                        boolean activitySent =
                                                                applicationActivitySender
                                                                        .sendActivity(
                                                                                request
                                                                        );


                                                        if (activitySent) {

                                                            System.out.println(
                                                                    "Application activity sent successfully."
                                                            );

                                                        } else {

                                                            System.out.println(
                                                                    "Application activity failed to send."
                                                            );
                                                        }
                                                    }


                                                    Thread.sleep(
                                                            1000
                                                    );


                                                } catch (
                                                        InterruptedException e) {

                                                    Thread.currentThread()
                                                            .interrupt();


                                                    System.out.println(
                                                            "Application activity monitor stopped."
                                                    );


                                                    break;


                                                } catch (Exception e) {

                                                    System.out.println(
                                                            "Application activity monitoring error: "
                                                                    + e.getMessage()
                                                    );
                                                }
                                            }


                                            System.out.println(
                                                    "Application activity monitor terminated."
                                            );

                                        });


                                applicationActivityThread.setName(
                                        "Application-Activity-Monitor"
                                );


                                applicationActivityThread.setDaemon(
                                        true
                                );


                                applicationActivityThread.start();


                                System.out.println(
                                        "Application activity monitoring started."
                                );


                                /*
                                 * =========================================================
                                 * ATTENDANCE QUEUE PROCESSOR
                                 * =========================================================
                                 */

                                Thread attendanceQueueThread =
                                        new Thread(() -> {

                                            while (sessionRunning) {

                                                try {

                                                    queueProcessor
                                                            .processQueue();


                                                    Thread.sleep(
                                                            10_000
                                                    );


                                                } catch (
                                                        InterruptedException e) {

                                                    Thread.currentThread()
                                                            .interrupt();


                                                    System.out.println(
                                                            "Attendance queue processor stopped."
                                                    );


                                                    break;


                                                } catch (Exception e) {

                                                    System.out.println(
                                                            "ERROR Attendance queue processor error: "
                                                                    + e.getMessage()
                                                    );
                                                }
                                            }


                                            System.out.println(
                                                    "Attendance queue processor terminated."
                                            );

                                        });


                                attendanceQueueThread.setName(
                                        "Attendance-Queue-Processor"
                                );


                                attendanceQueueThread.setDaemon(
                                        true
                                );


                                attendanceQueueThread.start();


                                System.out.println(
                                        "Attendance queue processor started."
                                );


                                /*
                                 * =========================================================
                                 * USER ACTIVITY MONITOR
                                 * =========================================================
                                 */

                                UserActivityMonitor
                                        activityMonitor =
                                        new UserActivityMonitor();


                                int idleGraceMinutes =
                                        config.getIdleGraceMinutes();


                                IdleDetectionService
                                        idleDetectionService =
                                        new IdleDetectionService(
                                                activityMonitor,
                                                idleGraceMinutes
                                        );


                                long autoWorkEndIdleSeconds =
                                        config.getAutoWorkEndIdleMinutes()
                                                * 60L;


                                Thread activityMonitorThread =
                                        new Thread(() -> {
                                        
                                            long activeWithoutTimerSeconds = 0;

                                            while (sessionRunning) {

                                                try {

                                                    Thread.sleep(
                                                            1000
                                                    );


                                                    long inactiveSeconds =
                                                            activityMonitor
                                                                    .getInactiveDurationSeconds();


                                                    AttendanceEventType
                                                            idleEvent =
                                                            idleDetectionService
                                                                    .checkIdleStatus();


                                                    boolean isIdle =
                                                            idleDetectionService
                                                                    .isIdle();


                                                    /*
                                                     * =================================================
                                                     * WORK TIMER REMINDER
                                                     * =================================================
                                                     */
                                                    if (!taskWorkRunning && !breakRunning && !lunchRunning && !offlineMode) {
                                                        if (!isIdle && inactiveSeconds < 60) {
                                                            activeWithoutTimerSeconds++;
                                                            if (activeWithoutTimerSeconds >= 60) {
                                                                org.example.ui.WorkTimerReminderManager.showReminder(null);
                                                                activeWithoutTimerSeconds = 0;
                                                            }
                                                        } else {
                                                            activeWithoutTimerSeconds = 0;
                                                        }
                                                    } else {
                                                        activeWithoutTimerSeconds = 0;
                                                        org.example.ui.WorkTimerReminderManager.dismissReminder();
                                                    }


                                                    /*
                                                     * =================================================
                                                     * IDLE EVENTS
                                                     * =================================================
                                                     *
                                                     * AttendanceEventManager is the source of truth.
                                                     *
                                                     * IMPORTANT:
                                                     * Do not generate duplicate IDLE_STARTED events
                                                     * when the workspace has already moved the state
                                                     * to IDLE after Break or Lunch.
                                                     */

                                                    AttendanceState currentAttendanceState =
                                                            eventManager.getCurrentState();


                                                    if (
                                                            idleEvent != null
                                                                    && taskWorkRunning
                                                                    && !breakRunning
                                                                    && !lunchRunning
                                                    ) {

                                                        /*
                                                         * -------------------------------------------------
                                                         * IDLE START
                                                         * -------------------------------------------------
                                                         *
                                                         * Only create IDLE_STARTED when the current
                                                         * attendance state is NOT already IDLE.
                                                         */

                                                        if (
                                                                idleEvent
                                                                        == AttendanceEventType.IDLE_STARTED
                                                                        && currentAttendanceState
                                                                        != AttendanceState.IDLE
                                                        ) {

                                                            AttendanceEvent
                                                                    idleAttendanceEvent =
                                                                    eventManager.startIdle();


                                                            AttendanceSendResult
                                                                    idleEventResult =
                                                                    eventSender.sendEvent(
                                                                            idleAttendanceEvent
                                                                    );


                                                            if (
                                                                    idleEventResult
                                                                            == AttendanceSendResult.SUCCESS
                                                            ) {

                                                                System.out.println(
                                                                        "IDLE_STARTED event sent successfully."
                                                                );

                                                            } else if (
                                                                    idleEventResult
                                                                            == AttendanceSendResult.REJECTED
                                                            ) {

                                                                System.out.println(
                                                                        "IDLE_STARTED event was rejected by server. "
                                                                                + "Event will NOT be queued."
                                                                );

                                                            } else {

                                                                System.out.println(
                                                                        "IDLE_STARTED event failed to send. "
                                                                                + "Event will be queued for retry."
                                                                );


                                                                attendanceEventQueue.add(
                                                                        idleAttendanceEvent
                                                                );
                                                            }


                                                            AttendanceState
                                                                    updatedAttendanceState =
                                                                    eventManager.getCurrentState();


                                                            String updatedState =
                                                                    updatedAttendanceState == null
                                                                            ? "UNKNOWN"
                                                                            : updatedAttendanceState.name();


                                                            logActivityStateChange(
                                                                    updatedState
                                                            );
                                                        }


                                                        /*
                                                         * -------------------------------------------------
                                                         * IDLE END
                                                         * -------------------------------------------------
                                                         *
                                                         * Only create IDLE_ENDED when the current
                                                         * attendance state is actually IDLE.
                                                         */

                                                        else if (
                                                                idleEvent
                                                                        == AttendanceEventType.IDLE_ENDED
                                                                        && currentAttendanceState
                                                                        == AttendanceState.IDLE
                                                        ) {

                                                            AttendanceEvent
                                                                    idleAttendanceEvent =
                                                                    eventManager.endIdle();


                                                            AttendanceSendResult
                                                                    idleEventResult =
                                                                    eventSender.sendEvent(
                                                                            idleAttendanceEvent
                                                                    );


                                                            if (
                                                                    idleEventResult
                                                                            == AttendanceSendResult.SUCCESS
                                                            ) {

                                                                System.out.println(
                                                                        "IDLE_ENDED event sent successfully."
                                                                );

                                                            } else if (
                                                                    idleEventResult
                                                                            == AttendanceSendResult.REJECTED
                                                            ) {

                                                                System.out.println(
                                                                        "IDLE_ENDED event was rejected by server. "
                                                                                + "Event will NOT be queued."
                                                                );

                                                            } else {

                                                                System.out.println(
                                                                        "IDLE_ENDED event failed to send. "
                                                                                + "Event will be queued for retry."
                                                                );


                                                                attendanceEventQueue.add(
                                                                        idleAttendanceEvent
                                                                );
                                                            }


                                                            AttendanceState
                                                                    updatedAttendanceState =
                                                                    eventManager.getCurrentState();


                                                            String updatedState =
                                                                    updatedAttendanceState == null
                                                                            ? "UNKNOWN"
                                                                            : updatedAttendanceState.name();


                                                            logActivityStateChange(
                                                                    updatedState
                                                            );
                                                        }


                                                        /*
                                                         * -------------------------------------------------
                                                         * DUPLICATE / INVALID IDLE EVENT
                                                         * -------------------------------------------------
                                                         *
                                                         * The event detector may report an event that
                                                         * is already reflected in AttendanceEventManager.
                                                         *
                                                         * Do not create another attendance event.
                                                         */

                                                        else {

                                                            AttendanceState
                                                                    state =
                                                                    eventManager.getCurrentState();


                                                            String currentState =
                                                                    state == null
                                                                            ? "UNKNOWN"
                                                                            : state.name();


                                                            logActivityStateChange(
                                                                    currentState
                                                            );
                                                        }

                                                    } else {

                                                        /*
                                                         * No automatic IDLE transition occurred.
                                                         *
                                                         * Still display the current real attendance state.
                                                         */

                                                        AttendanceState
                                                                state =
                                                                eventManager.getCurrentState();


                                                        String currentState =
                                                                state == null
                                                                        ? "UNKNOWN"
                                                                        : state.name();


                                                        logActivityStateChange(
                                                                currentState
                                                        );
                                                    }


                                                    /*
                                                     * =================================================
                                                     * PERSIST REAL ACTIVITY
                                                     * =================================================
                                                     */

                                                    if (
                                                            !isIdle
                                                                    && inactiveSeconds <= 1
                                                                    && eventManager
                                                                    .getCurrentState()
                                                                    == AttendanceState.WORKING
                                                    ) {

                                                        long nowMillis =
                                                                System.currentTimeMillis();


                                                        if (
                                                                nowMillis
                                                                        - lastActivityStatePersistMillis
                                                                        >= 30_000L
                                                        ) {

                                                            agentStateManager
                                                                    .updateLastActivity(
                                                                            java.time.Instant
                                                                                    .now()
                                                                                    .minusSeconds(
                                                                                            inactiveSeconds
                                                                                    )
                                                                    );


                                                            lastActivityStatePersistMillis =
                                                                    nowMillis;
                                                        }
                                                    }


                                                    /*
                                                     * =========================================================
                                                     * LONG IDLE -> AUTOMATIC WORK END
                                                     * =========================================================
                                                     */

                                                    if (
                                                            sessionRunning
                                                                    && !autoWorkEndTriggered
                                                                    && isIdle
                                                                    && inactiveSeconds
                                                                    >= autoWorkEndIdleSeconds
                                                    ) {

                                                        System.out.println(
                                                                "================================="
                                                        );


                                                        System.out.println(
                                                                "LONG IDLE DETECTED"
                                                        );


                                                        System.out.println(
                                                                "Inactive for "
                                                                        + inactiveSeconds
                                                                        + " seconds."
                                                        );


                                                        System.out.println(
                                                                "Automatic WORK_ENDED will be triggered."
                                                        );


                                                        System.out.println(
                                                                "================================="
                                                        );


                                                        try {

                                                            AttendanceEvent
                                                                    workEndedEvent =
                                                                    eventFactory
                                                                            .create(
                                                                                    AttendanceEventType.WORK_ENDED
                                                                            );


                                                            AttendanceSendResult
                                                                    result =
                                                                    eventSender
                                                                            .sendEvent(
                                                                                    workEndedEvent
                                                                            );


                                                            if (
                                                                    result
                                                                            == AttendanceSendResult.SUCCESS
                                                            ) {

                                                                System.out.println(
                                                                        "Automatic WORK_ENDED sent successfully."
                                                                );


                                                                autoWorkEndTriggered =
                                                                        true;


                                                                sessionRunning =
                                                                        false;


                                                            } else if (
                                                                    result
                                                                            == AttendanceSendResult.REJECTED
                                                            ) {

                                                                System.out.println(
                                                                        "Automatic WORK_ENDED was rejected by server."
                                                                );


                                                            } else {

                                                                System.out.println(
                                                                        "Automatic WORK_ENDED failed."
                                                                );


                                                                attendanceEventQueue
                                                                        .add(
                                                                                workEndedEvent
                                                                        );


                                                                autoWorkEndTriggered =
                                                                        true;


                                                                sessionRunning =
                                                                        false;
                                                            }


                                                        } catch (Exception e) {

                                                            System.out.println(
                                                                    "Automatic WORK_ENDED failed: "
                                                                            + e.getMessage()
                                                            );


                                                            e.printStackTrace();
                                                        }
                                                    }


                                                } catch (
                                                        InterruptedException e) {

                                                    Thread.currentThread()
                                                            .interrupt();


                                                    System.out.println(
                                                            "Activity monitor stopped."
                                                    );


                                                    break;


                                                } catch (Exception e) {

                                                    System.out.println(
                                                            "Activity monitoring error: "
                                                                    + e.getMessage()
                                                    );
                                                }
                                            }


                                            System.out.println(
                                                    "User activity monitor terminated."
                                            );

                                        });


                                activityMonitorThread.setName(
                                        "User-Activity-Monitor"
                                );


                                activityMonitorThread.setDaemon(
                                        true
                                );


                                activityMonitorThread.start();


                                System.out.println(
                                        "System-wide activity monitoring started."
                                );


                                /*
                                 * =========================================================
                                 * HEARTBEAT
                                 * =========================================================
                                 */

                                System.out.println(
                                        "Starting heartbeat..."
                                );


                                HeartbeatService
                                        heartbeatService =
                                        new HeartbeatService(
                                                serverUrl,
                                                deviceId
                                        );


                                Thread heartbeatThread =
                                        new Thread(() -> {

                                            while (sessionRunning) {

                                                try {

                                                    String hbResponse = heartbeatService
                                                            .sendHeartbeat();
                                                            
                                                    if (hbResponse == null) {
                                                        if (!AgentApplication.isOfflineMode()) {
                                                            System.out.println("Heartbeat failed, switching to offline mode dynamically.");
                                                            AgentApplication.setOfflineMode(true);
                                                        }
                                                    } else {
                                                        if (AgentApplication.isOfflineMode()) {
                                                            System.out.println("Heartbeat succeeded, switching to online mode dynamically.");
                                                            AgentApplication.setOfflineMode(false);
                                                        }
                                                    }

                                                    if (hbResponse != null && hbResponse.contains("\"status\":\"OFFLINE\"")) {
                                                        System.out.println("ALERT: Session invalidated by server (Logged in on another machine).");
                                                        sessionRunning = false;
                                                        SwingUtilities.invokeLater(() -> {
                                                            workspaceWindow.setVisible(false);
                                                            workspaceWindow.dispose();
                                                            JOptionPane.showMessageDialog(null,
                                                                "Your session was terminated because this employee logged in on another device.",
                                                                "Session Terminated",
                                                                JOptionPane.WARNING_MESSAGE);
                                                            loginWindowHolder[0].setVisible(true);
                                                            loginWindowHolder[0].toFront();
                                                        });
                                                        break;
                                                    }


                                                    if (
                                                            agentStateManager
                                                                    != null
                                                    ) {

                                                        agentStateManager
                                                                .updateHeartbeat();
                                                    }


                                                    Thread.sleep(
                                                            config.getHeartbeatIntervalSeconds() * 1000L
                                                    );


                                                } catch (
                                                        InterruptedException e) {

                                                    Thread.currentThread()
                                                            .interrupt();


                                                    System.out.println(
                                                            "Heartbeat stopped."
                                                    );


                                                    break;
                                                }
                                            }


                                            System.out.println(
                                                    "Heartbeat monitor terminated."
                                            );

                                        });


                                heartbeatThread.setName(
                                        "Heartbeat-Monitor"
                                );


                                heartbeatThread.setDaemon(
                                        true
                                );


                                heartbeatThread.start();


                                /*
                                 * =========================================================
                                 * GRACEFUL SHUTDOWN
                                 * =========================================================
                                 */

                                Runtime.getRuntime()
                                        .addShutdownHook(
                                                new Thread(() -> {

                                                    System.out.println(
                                                            "Agent shutdown detected."
                                                    );


                                                    if (workEndedSent) {

                                                        System.out.println(
                                                                "WORK_ENDED already sent. "
                                                                        + "Skipping duplicate shutdown event."
                                                        );

                                                        return;
                                                    }


                                                    workEndedSent =
                                                            true;


                                                    try {

                                                        AttendanceEvent
                                                                workEndedEvent =
                                                                eventManager
                                                                        .endWork();


                                                        AttendanceSendResult
                                                                workEndedResult =
                                                                eventSender
                                                                        .sendEvent(
                                                                                workEndedEvent
                                                                        );


                                                        if (
                                                                workEndedResult
                                                                        == AttendanceSendResult.SUCCESS
                                                        ) {

                                                            System.out.println(
                                                                    "WORK_ENDED event sent successfully."
                                                            );

                                                        } else if (
                                                                workEndedResult
                                                                        == AttendanceSendResult.REJECTED
                                                        ) {

                                                            System.out.println(
                                                                    "WORK_ENDED event was rejected by server. "
                                                                            + "Event will NOT be queued."
                                                            );

                                                        } else {

                                                            System.out.println(
                                                                    "WORK_ENDED event failed to send. "
                                                                            + "Event will be queued for retry."
                                                            );


                                                            attendanceEventQueue
                                                                    .add(
                                                                            workEndedEvent
                                                                    );
                                                        }


                                                    } catch (Exception e) {

                                                        System.out.println(
                                                                "Unable to send WORK_ENDED event: "
                                                                        + e.getMessage()
                                                        );


                                                        try {

                                                            AttendanceEvent
                                                                    workEndedEvent =
                                                                    eventManager
                                                                            .endWork();


                                                            attendanceEventQueue
                                                                    .add(
                                                                            workEndedEvent
                                                                    );


                                                        } catch (
                                                                Exception queueException) {

                                                            System.out.println(
                                                                    "Unable to queue WORK_ENDED event: "
                                                                            + queueException
                                                                            .getMessage()
                                                            );
                                                        }
                                                    }

                                                })
                                        );

                            }
                    );


            /*
             * =========================================================
             * REGISTER EXISTING LOGIN WINDOW
             * =========================================================
             */

            loginPopupManager.setLoginWindow(
                    loginWindowHolder[0]
            );


            /*
             * =========================
             * Auto-Login / Show Window
             * =========================
             */

            boolean autoLoginStarted = loginWindowHolder[0].attemptAutoLogin();

            if (!autoLoginStarted) {
                loginWindowHolder[0].setVisible(true);
            }


            /*
             * =========================================================
             * START LOGIN REMINDER
             * =========================================================
             */

            loginPopupManager.start();

        });
    }


    /*
     * =========================================================
     * BREAK STATE
     * =========================================================
     */

    public static void setBreakRunning(
            boolean running) {

        breakRunning =
                running;


        if (running) {

            lunchRunning =
                    false;
        }
    }


    /*
     * =========================================================
     * LUNCH STATE
     * =========================================================
     */

    public static void setLunchRunning(
            boolean running) {

        lunchRunning =
                running;


        if (running) {

            breakRunning =
                    false;
        }
    }


    /*
     * =========================================================
     * TASK WORK STATE
     * =========================================================
     */

    public static void setTaskWorkRunning(
            boolean running) {

        taskWorkRunning =
                running;
    }


    public static boolean isTaskWorkRunning() {

        return taskWorkRunning;
    }


    /*
     * =========================================================
     * STOP AGENT SESSION
     * =========================================================
     */

    public static void stopAgentSession() {

        if (!sessionRunning) {

            return;
        }


        System.out.println(
                "Stopping Employee Monitoring Agent..."
        );


        /*
         * Stop all monitoring loops.
         */

        sessionRunning =
                false;


        /*
         * WORK_ENDED already handled
         * by Workspace.
         */

        workEndedSent =
                true;


        breakRunning =
                false;


        lunchRunning =
                false;


        taskWorkRunning =
                false;


        System.out.println(
                "All monitoring services are stopping..."
        );


        try {

            Thread.sleep(
                    500
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();
        }


        System.out.println(
                "Employee Monitoring Agent stopped."
        );


        System.exit(0);
    }


    /*
     * =========================================================
     * OPTIONAL STATE ACCESSORS
     * =========================================================
     */

    public static boolean isSessionRunning() {

        return sessionRunning;
    }


    public static boolean isBreakRunning() {

        return breakRunning;
    }


    public static boolean isLunchRunning() {

        return lunchRunning;
    }


    // =========================================================
    // OTA SUPPORT
    // =========================================================

    public static void checkForOtaUpdate(
            String serverUrl,
            Consumer<UpdateMetadata> onUpdateAvailable) {

        Thread otaThread =
                new Thread(
                        () -> {

                            try {

                                System.out.println(
                                        "================================="
                                );


                                System.out.println(
                                        "OTA: Starting update check."
                                );


                                System.out.println(
                                        "================================="
                                );


                                UpdateChecker checker =
                                        new UpdateChecker(
                                                serverUrl
                                        );


                                UpdateMetadata update =
                                        checker.checkForUpdate();


                                if (update == null) {

                                    System.out.println(
                                            "OTA: No update available."
                                    );

                                    return;
                                }


                                System.out.println(
                                        "OTA: Update available: "
                                                + update.getVersion()
                                );


                                if (
                                        onUpdateAvailable
                                                != null
                                ) {

                                    SwingUtilities.invokeLater(
                                            () ->
                                                    onUpdateAvailable
                                                            .accept(update)
                                    );
                                }


                            } catch (Exception e) {

                                System.out.println(
                                        "OTA: Update check failed: "
                                                + e.getMessage()
                                );


                                e.printStackTrace();
                            }

                        },
                        "EMS-OTA-Checker"
                );


        otaThread.setDaemon(true);


        otaThread.start();
    }


    public static void installOtaUpdate(
            UpdateMetadata update) {

        if (update == null) {

            System.out.println(
                    "OTA: Update metadata is null."
            );

            return;
        }


        Thread otaInstallThread =
                new Thread(
                        () -> {

                            try {

                                System.out.println(
                                        "================================="
                                );


                                System.out.println(
                                        "OTA: Starting update installation"
                                );


                                System.out.println(
                                        "OTA: Version = "
                                                + update.getVersion()
                                );


                                System.out.println(
                                        "================================="
                                );


                                OtaUpdateService otaService =
                                        new OtaUpdateService();


                                Path installer =
                                        otaService
                                                .downloadAndVerify(
                                                        update
                                                );


                                System.out.println(
                                        "OTA: Download and verification successful."
                                );


                                /*
                                 * Start updater.
                                 */

                                launchUpdater(
                                        installer
                                );


                            } catch (Exception e) {

                                System.out.println(
                                        "OTA: Update installation failed: "
                                                + e.getMessage()
                                );


                                e.printStackTrace();


                                SwingUtilities.invokeLater(
                                        () ->
                                                JOptionPane
                                                        .showMessageDialog(
                                                                null,
                                                                "Update installation failed.\n\n"
                                                                        + e.getMessage(),
                                                                "Employee Monitoring Agent",
                                                                JOptionPane.ERROR_MESSAGE
                                                        )
                                );
                            }

                        },
                        "EMS-OTA-Installer"
                );


        otaInstallThread.setDaemon(true);


        otaInstallThread.start();
    }


    /*
     * =========================================================
     * OTA UPDATER LAUNCHER
     * =========================================================
     *
     * IMPORTANT:
     *
     * We DO NOT use:
     *
     * runtime\bin\java.exe
     * runtime\bin\javaw.exe
     *
     * The jpackage runtime on the installed PC does not
     * contain those launcher executables.
     *
     *
     * Instead this method creates a small external
     * Windows CMD updater.
     *
     *
     * Flow:
     *
     * Agent
     *   |
     *   v
     * OTA CMD helper
     *   |
     *   v
     * Wait for Agent PID
     *   |
     *   v
     * msiexec.exe
     *   |
     *   v
     * Start updated Agent
     *
     * =========================================================
     */

    private static void launchUpdater(
            Path installer)
            throws Exception {

        System.out.println(
                "OTA: Preparing external updater..."
        );

        if (installer == null) {
            throw new IllegalArgumentException("OTA installer path is null.");
        }

        if (!Files.exists(installer)) {
            throw new IllegalStateException("OTA installer does not exist: " + installer);
        }

        String currentCommand = ProcessHandle.current().info().command().orElse("");
        Path agentExecutable;

        if (currentCommand != null && !currentCommand.isBlank() && currentCommand.toLowerCase().endsWith(".exe")) {
            agentExecutable = Path.of(currentCommand);
        } else {
            agentExecutable = Path.of(System.getProperty("user.dir"), "EmployeeMonitoringAgent.exe");
        }
        agentExecutable = agentExecutable.toAbsolutePath().normalize();

        if (!Files.exists(agentExecutable)) {
            throw new IllegalStateException("Current Agent executable not found: " + agentExecutable);
        }

        long currentPid = ProcessHandle.current().pid();
        Path updateDirectory = Path.of("C:\\ProgramData\\EmployeeAgent\\updates");
        Files.createDirectories(updateDirectory);

        Path otaMarker = Path.of("C:\\ProgramData\\EmployeeAgent\\ota-update.marker");
        if (!Files.exists(otaMarker)) {
            Files.createFile(otaMarker);
        }

        Path updaterScript = updateDirectory.resolve("ems-ota-updater-" + currentPid + ".cmd");
        String installerPath = installer.toAbsolutePath().normalize().toString();
        String agentPath = agentExecutable.toAbsolutePath().normalize().toString();

        StringBuilder script = new StringBuilder();
        script.append("@echo off\r\n");
        script.append("setlocal EnableExtensions EnableDelayedExpansion\r\n");
        script.append("set \"PARENT_PID=").append(currentPid).append("\"\r\n");
        script.append("set \"INSTALLER=").append(installerPath).append("\"\r\n");
        script.append("set \"AGENT=").append(agentPath).append("\"\r\n");
        script.append("set \"LOG_DIR=C:\\ProgramData\\EmployeeAgent\\logs\"\r\n");
        script.append("set \"LOG_FILE=%LOG_DIR%\\updater.log\"\r\n");
        script.append("if not exist \"%LOG_DIR%\" mkdir \"%LOG_DIR%\" >nul 2>&1\r\n");
        
        script.append("echo OTA: INSTALLING>>\"%LOG_FILE%\"\r\n");

        script.append(":WAIT_FOR_AGENT\r\n");
        script.append("tasklist /FI \"PID eq %PARENT_PID%\" | findstr /C:\"%PARENT_PID%\" >nul\r\n");
        script.append("if not errorlevel 1 (\r\n");
        script.append("    timeout /t 1 /nobreak >nul\r\n");
        script.append("    goto WAIT_FOR_AGENT\r\n");
        script.append(")\r\n");
        
        script.append("powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command \"$p = Start-Process -FilePath 'msiexec.exe' -ArgumentList '/i', '\\\"%INSTALLER%\\\"', '/qn', '/norestart', '/L*v', '\\\"%LOG_DIR%\\ota-msi-install.log\\\"' -Wait -PassThru -Verb RunAs; exit $p.ExitCode\"\r\n");
        script.append("set \"MSI_EXIT_CODE=!ERRORLEVEL!\"\r\n");

        script.append("if \"!MSI_EXIT_CODE!\"==\"0\" goto INSTALL_SUCCESS\r\n");
        script.append("if \"!MSI_EXIT_CODE!\"==\"3010\" goto INSTALL_SUCCESS\r\n");
        
        script.append("echo OTA: FAILED>>\"%LOG_FILE%\"\r\n");
        script.append("echo OTA: RESTARTING>>\"%LOG_FILE%\"\r\n");
        script.append("explorer.exe \"%AGENT%\"\r\n");
        script.append("goto END\r\n");

        script.append(":INSTALL_SUCCESS\r\n");
        script.append("echo OTA: INSTALLED>>\"%LOG_FILE%\"\r\n");
        script.append("del /f /q \"%INSTALLER%\" >nul 2>&1\r\n");
        script.append("echo OTA: RESTARTING>>\"%LOG_FILE%\"\r\n");
        script.append("explorer.exe \"%AGENT%\"\r\n");
        script.append("echo OTA: VERIFYING_VERSION>>\"%LOG_FILE%\"\r\n");
        script.append("timeout /t 5 /nobreak >nul\r\n");
        script.append("tasklist /FI \"IMAGENAME eq EMS Agent.exe\" | findstr /C:\"EMS Agent.exe\" >nul\r\n");
        script.append("if not errorlevel 1 (\r\n");
        script.append("    echo OTA: SUCCESS>>\"%LOG_FILE%\"\r\n");
        script.append(") else (\r\n");
        script.append("    echo OTA: FAILED>>\"%LOG_FILE%\"\r\n");
        script.append(")\r\n");

        script.append(":END\r\n");
        script.append("timeout /t 2 /nobreak >nul\r\n");
        script.append("del /f /q \"%~f0\" >nul 2>&1\r\n");
        script.append("exit /b 0\r\n");

        Files.writeString(
                updaterScript,
                script.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        new ProcessBuilder(
                "cmd.exe",
                "/c",
                "start",
                "\"Employee Monitoring OTA Updater\"",
                "/min",
                "cmd.exe",
                "/c",
                updaterScript.toString()
        ).start();

        System.exit(0);
    }
}
