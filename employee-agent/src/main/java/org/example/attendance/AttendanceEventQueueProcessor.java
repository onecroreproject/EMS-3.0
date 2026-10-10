package org.example.attendance;

public class AttendanceEventQueueProcessor {

    private final AttendanceEventQueue queue;
    private final AttendanceEventSender eventSender;

    public AttendanceEventQueueProcessor(
            AttendanceEventQueue queue,
            AttendanceEventSender eventSender) {

        this.queue = queue;
        this.eventSender = eventSender;
    }

    public void processQueue() {

        while (!queue.isEmpty()) {

            AttendanceEvent event =
                    queue.peek();

            if (event == null) {
                return;
            }

            AttendanceSendResult result =
                    eventSender.sendEvent(event);

            /*
             * =====================================================
             * SUCCESS
             *
             * Server successfully accepted the event.
             * Remove it from the persistent queue.
             * =====================================================
             */
            if (result == AttendanceSendResult.SUCCESS) {

                queue.poll();

                System.out.println(
                        "Queued attendance event sent successfully: "
                                + event.getEventType()
                );

                continue;
            }

            /*
             * =====================================================
             * REJECTED
             *
             * Server received the event but rejected it.
             *
             * Do NOT retry it.
             * Remove it from the queue because retrying will not
             * solve a client/business validation error.
             * =====================================================
             */
            if (result == AttendanceSendResult.REJECTED) {

                queue.poll();

                System.out.println(
                        "Queued attendance event was rejected by server: "
                                + event.getEventType()
                                + ". Event removed from queue."
                );

                continue;
            }

            /*
             * =====================================================
             * RETRY
             *
             * Server is temporarily unavailable or returned
             * a server-side error.
             *
             * Keep the event in the queue.
             * Stop processing so it can be retried later.
             * =====================================================
             */
            if (result == AttendanceSendResult.RETRY) {

                System.out.println(
                        "Unable to send queued event: "
                                + event.getEventType()
                                + ". Event remains in queue for retry."
                );

                return;
            }
        }
    }
}