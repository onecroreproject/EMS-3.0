package org.example.attendance;

import java.nio.file.Path;

public class AttendanceEventQueue {

    private final PersistentAttendanceQueue persistentQueue;

    public AttendanceEventQueue() {

        Path queueFile =
                Path.of(
                        "C:",
                        "ProgramData",
                        "EmployeeAgent",
                        "data",
                        "attendance.queue"
                );

        this.persistentQueue =
                new PersistentAttendanceQueue(queueFile);

        System.out.println(
                "Persistent attendance queue initialized."
        );

        System.out.println(
                "Queue file: "
                        + queueFile
        );
    }

    public synchronized void add(AttendanceEvent event) {

        if (event == null) {
            return;
        }

        persistentQueue.add(event);

        System.out.println(
                "Attendance event queued persistently: "
                        + event.getEventType()
        );
    }

    public synchronized AttendanceEvent poll() {

        AttendanceEvent event =
                persistentQueue.peek();

        if (event == null) {
            return null;
        }

        persistentQueue.removeFirst();

        return event;
    }

    public synchronized AttendanceEvent peek() {

        return persistentQueue.peek();
    }

    public synchronized boolean isEmpty() {

        return persistentQueue.isEmpty();
    }

    public synchronized int size() {

        return persistentQueue.size();
    }
}