
package org.example.attendance;

import java.time.Instant;
import java.time.ZoneId;

public class AttendanceEventManager {

    private final AttendanceEventFactory eventFactory;

    /*
     * Responsible for persisting the current
     * attendance/session state locally.
     */
    private final AgentStateManager stateManager;

    private AttendanceState currentState;

    public AttendanceEventManager(
            AttendanceEventFactory eventFactory
    ) {

        this(
                eventFactory,
                new AgentStateManager()
        );
    }


    /**
     * Uses the application's single AgentStateManager.
     */
    public AttendanceEventManager(
            AttendanceEventFactory eventFactory,
            AgentStateManager stateManager
    ) {

        this.eventFactory = eventFactory;

        this.stateManager = stateManager;

        AgentState savedState =
                stateManager.getState();

        if (savedState != null &&
                savedState.getCurrentState() != null) {

            try {

                currentState =
                        AttendanceState.valueOf(
                                savedState.getCurrentState()
                        );

            } catch (IllegalArgumentException e) {

                currentState =
                        AttendanceState.CLOCKED_OUT;
            }

        } else {

            currentState =
                    AttendanceState.CLOCKED_OUT;
        }

        System.out.println("INFO  Agent started");
        System.out.println("INFO  Attendance state: " + currentState.name());
    }

    // =========================================================
    // WORK START
    // =========================================================

    /**
     * Employee starts work.
     *
     * Creates the attendance event first.
     * The exact same event timestamp is then
     * persisted into agent-state.json.
     */
    public synchronized AttendanceEvent startWork() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.WORK_STARTED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.WORKING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.startWork(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // IDLE
    // =========================================================

    /**
     * Employee becomes idle.
     */
    public synchronized AttendanceEvent startIdle() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.IDLE_STARTED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.IDLE;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.startIdle(
                toInstant(event)
        );

        return event;
    }

    /**
     * Employee becomes active again.
     */
    public synchronized AttendanceEvent endIdle() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.IDLE_ENDED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.WORKING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.endIdle(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // BREAK
    // =========================================================

    /**
     * Employee starts break.
     */
    public synchronized AttendanceEvent startBreak() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.BREAK_STARTED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.BREAK;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.startBreak(
                toInstant(event)
        );

        return event;
    }

    /**
     * Employee ends break.
     */
    public synchronized AttendanceEvent endBreak() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.BREAK_ENDED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.WORKING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.endBreak(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // LUNCH
    // =========================================================

    /**
     * Employee starts lunch.
     */
    public synchronized AttendanceEvent startLunch() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.LUNCH_STARTED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.LUNCH;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.startLunch(
                toInstant(event)
        );

        return event;
    }

    /**
     * Employee ends lunch.
     */
    public synchronized AttendanceEvent endLunch() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.LUNCH_ENDED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.WORKING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.endLunch(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // MEETING
    // =========================================================

    /**
     * Employee starts meeting.
     */
    public synchronized AttendanceEvent startMeeting() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.MEETING_STARTED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.MEETING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.startMeeting(
                toInstant(event)
        );

        return event;
    }

    /**
     * Employee ends meeting.
     */
    public synchronized AttendanceEvent endMeeting() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.MEETING_ENDED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.WORKING;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.endMeeting(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // WORK END
    // =========================================================

    /**
     * Employee ends the attendance session.
     */
    public synchronized AttendanceEvent endWork() {

        AttendanceEvent event =
                eventFactory.create(
                        AttendanceEventType.WORK_ENDED
                );

        AttendanceState oldState = currentState;
        currentState =
                AttendanceState.CLOCKED_OUT;
        
        System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());

        stateManager.endWork(
                toInstant(event)
        );

        return event;
    }

    // =========================================================
    // EXISTING EVENT -> STATE
    // =========================================================

    /**
     * Persist an event created outside this manager.
     *
     * Example:
     * EmployeeWorkspaceWindow creates BREAK_STARTED.
     *
     * This method makes sure the same event timestamp
     * is also written to agent-state.json.
     */
    public synchronized void persistAttendanceEvent(
            AttendanceEvent event
    ) {

        stateManager.persistAttendanceEvent(event);

        if (event == null ||
                event.getEventType() == null) {

            return;
        }

        AttendanceState oldState = currentState;

        switch (event.getEventType()) {

            case WORK_STARTED, CLOCK_IN ->
                    currentState =
                            AttendanceState.WORKING;

            case WORK_ENDED, CLOCK_OUT ->
                    currentState =
                            AttendanceState.CLOCKED_OUT;

            case IDLE_STARTED ->
                    currentState =
                            AttendanceState.IDLE;

            case IDLE_ENDED ->
                    currentState =
                            AttendanceState.WORKING;

            case BREAK_STARTED ->
                    currentState =
                            AttendanceState.BREAK;

            case BREAK_ENDED ->
                    currentState =
                            AttendanceState.WORKING;

            case LUNCH_STARTED ->
                    currentState =
                            AttendanceState.LUNCH;

            case LUNCH_ENDED ->
                    currentState =
                            AttendanceState.WORKING;

            case MEETING_STARTED ->
                    currentState =
                            AttendanceState.MEETING;

            case MEETING_ENDED ->
                    currentState =
                            AttendanceState.WORKING;
        }
        
        if (oldState != currentState) {
            System.out.println("INFO  Attendance state changed: " + oldState.name() + " -> " + currentState.name());
        }
    }

    // =========================================================
    // STATE ACCESS
    // =========================================================

    /**
     * Returns the current in-memory attendance state.
     */
    public synchronized AttendanceState getCurrentState() {

        return currentState;
    }

    /**
     * Returns the persisted local agent state.
     */
    public synchronized AgentState getPersistedState() {

        return stateManager.getState();
    }

    // =========================================================
    // TIMESTAMP CONVERSION
    // =========================================================

    /**
     * Converts the AttendanceEvent LocalDateTime
     * into Instant using the system timezone.
     *
     * AttendanceEvent:
     *     LocalDateTime
     *
     * AgentState:
     *     Instant
     */
    private Instant toInstant(
            AttendanceEvent event
    ) {

        if (event == null) {

            return Instant.now();
        }

        return event.getTimestamp()
                .atZone(
                        ZoneId.systemDefault()
                )
                .toInstant();
    }
}

