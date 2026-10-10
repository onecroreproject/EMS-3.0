package org.example.attendance;

public enum AttendanceSendResult {

    /**
     * Server successfully accepted the event.
     */
    SUCCESS,

    /**
     * Server rejected the event.
     * The event should not be retried.
     */
    REJECTED,

    /**
     * Temporary failure.
     * The event should remain in the queue and be retried.
     */
    RETRY
}
