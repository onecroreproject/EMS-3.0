package org.example.attendance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class PersistentAttendanceQueue {

    private final Path queueFile;

    public PersistentAttendanceQueue(Path queueFile) {

        this.queueFile = queueFile;

        try {

            Path parent = queueFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(queueFile)) {
                Files.createFile(queueFile);
            }

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to initialize attendance queue storage.",
                    e
            );
        }
    }

    /**
     * Add an attendance event to local persistent storage.
     */
    public synchronized void add(AttendanceEvent event) {

        if (event == null) {
            return;
        }

        /*
         * Queue format:
         *
         * eventId|employeeCode|deviceId|eventType|timestamp
         */
        String eventData =
                event.getEventId()
                        + "|"
                        + event.getEmployeeCode()
                        + "|"
                        + event.getDeviceId()
                        + "|"
                        + event.getEventType()
                        + "|"
                        + event.getTimestamp();

        try {

            Files.writeString(
                    queueFile,
                    eventData + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println(
                    "Attendance event persisted locally: "
                            + event.getEventType()
                            + " | Event ID: "
                            + event.getEventId()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to persist attendance event.",
                    e
            );
        }
    }

    public synchronized AttendanceEvent peek() {

        List<String> lines = readLines();
        boolean corruptedRemoved = false;

        while (!lines.isEmpty()) {
            try {
                AttendanceEvent event = parseEvent(lines.get(0));
                if (corruptedRemoved) {
                    writeLines(lines);
                }
                return event;
            } catch (Exception e) {
                System.out.println(
                        "Discarding corrupted attendance event from queue: "
                                + lines.get(0)
                                + " | Reason: "
                                + e.getMessage()
                );
                lines.remove(0);
                corruptedRemoved = true;
            }
        }

        if (corruptedRemoved) {
            writeLines(lines);
        }

        return null;
    }

    /**
     * Removes the oldest event after
     * successful server acknowledgement.
     */
    public synchronized void removeFirst() {

        List<String> lines = readLines();

        if (lines.isEmpty()) {
            return;
        }

        lines.remove(0);

        writeLines(lines);

        System.out.println(
                "Persisted attendance event removed from local queue."
        );
    }

    /**
     * Checks whether the persistent queue is empty.
     */
    public synchronized boolean isEmpty() {

        return readLines().isEmpty();
    }

    /**
     * Returns the number of events currently stored.
     */
    public synchronized int size() {

        return readLines().size();
    }

    /**
     * Reads all queued events from disk.
     */
    private List<String> readLines() {

        try {

            if (!Files.exists(queueFile)) {
                return new ArrayList<>();
            }

            return new ArrayList<>(
                    Files.readAllLines(
                            queueFile,
                            StandardCharsets.UTF_8
                    )
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to read persistent attendance queue.",
                    e
            );
        }
    }

    /**
     * Writes the updated queue back to disk.
     */
    private void writeLines(List<String> lines) {

        try {

            Files.write(
                    queueFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to update persistent attendance queue.",
                    e
            );
        }
    }

    /**
     * Converts one stored queue line
     * back into an AttendanceEvent.
     *
     * Queue format:
     *
     * eventId|employeeCode|deviceId|eventType|timestamp
     */
    private AttendanceEvent parseEvent(String line) {

        String[] parts =
                line.split("\\|", -1);

        if (parts.length != 5) {

            throw new IllegalStateException(
                    "Invalid attendance event stored in queue: "
                            + line
            );
        }

        String eventId =
                parts[0];

        String employeeCode =
                parts[1];

        String deviceId =
                parts[2];

        AttendanceEventType eventType =
                AttendanceEventType.valueOf(parts[3]);

        java.time.LocalDateTime timestamp =
                java.time.LocalDateTime.parse(parts[4]);

        /*
         * Important:
         *
         * Use the constructor that accepts eventId.
         * This preserves the original event ID when
         * the event is loaded after an agent restart.
         */
        return new AttendanceEvent(
                eventId,
                employeeCode,
                deviceId,
                eventType,
                timestamp
        );
    }

    /**
     * Returns the location of the queue file.
     */
    public Path getQueueFile() {
        return queueFile;
    }
}