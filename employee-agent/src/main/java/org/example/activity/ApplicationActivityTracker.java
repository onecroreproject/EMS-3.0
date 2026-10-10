package org.example.activity;

import java.time.Duration;
import java.time.Instant;

public class ApplicationActivityTracker {

    private final ActiveWindowService activeWindowService;
    private final ApplicationActivitySender activitySender;

    private final String employeeCode;
    private final String deviceId;

    private String currentProcessName;
    private int currentProcessId;
    private String currentWindowTitle;

    private String currentUrl;
    private String currentDomain;

    private Instant activityStartedAt;


    public ApplicationActivityTracker(
            ActiveWindowService activeWindowService,
            ApplicationActivitySender activitySender,
            String employeeCode,
            String deviceId
    ) {
        this.activeWindowService = activeWindowService;
        this.activitySender = activitySender;
        this.employeeCode = employeeCode;
        this.deviceId = deviceId;
    }


    /**
     * Checks the currently active Windows application.
     */
    public ApplicationInfo checkActivity() {

        ActiveWindowService.ActiveWindowSnapshot snapshot =
                activeWindowService.getActiveWindowSnapshot();


        String processName =
                snapshot.getProcessName();

        int processId =
                snapshot.getProcessId();

        String windowTitle =
                snapshot.getWindowTitle();

        String url =
                snapshot.getUrl();

        String domain =
                snapshot.getDomain();


        // --------------------------------------------------------
        // Invalid process
        // --------------------------------------------------------

        if (processName == null
                || processName.isBlank()) {

            return null;
        }


        // --------------------------------------------------------
        // First activity
        // --------------------------------------------------------

        if (currentProcessName == null) {

            startNewActivity(
                    processName,
                    processId,
                    windowTitle,
                    url,
                    domain
            );

            return null;
        }


        // --------------------------------------------------------
        // Application/process changed
        // --------------------------------------------------------

        boolean applicationChanged =
                !currentProcessName.equalsIgnoreCase(
                        processName
                )
                        || currentProcessId != processId;


        // --------------------------------------------------------
        // Window title changed
        // --------------------------------------------------------

        boolean windowChanged =
                !safeEquals(
                        currentWindowTitle,
                        windowTitle
                );


        // --------------------------------------------------------
        // URL changed
        //
        // Important for:
        //
        // Google -> Gmail
        // GitHub -> MongoDB
        // MongoDB -> GitHub
        //
        // even though process remains chrome.exe.
        // --------------------------------------------------------

        boolean urlChanged =
                !safeEquals(
                        currentUrl,
                        url
                );


        // --------------------------------------------------------
        // Domain changed
        // --------------------------------------------------------

        boolean domainChanged =
                !safeEquals(
                        currentDomain,
                        domain
                );


        // --------------------------------------------------------
        // Activity changed
        // --------------------------------------------------------

        boolean activityChanged =
                applicationChanged
                        || windowChanged
                        || urlChanged
                        || domainChanged;


        // --------------------------------------------------------
        // Finish previous activity
        // --------------------------------------------------------

        if (activityChanged) {

            ApplicationInfo completedActivity =
                    finishCurrentActivity();


            // Start new activity
            startNewActivity(
                    processName,
                    processId,
                    windowTitle,
                    url,
                    domain
            );


            return completedActivity;
        }


        return null;
    }


    /**
     * Starts tracking a new activity.
     */
    private void startNewActivity(
            String processName,
            int processId,
            String windowTitle,
            String url,
            String domain
    ) {

        currentProcessName =
                processName;

        currentProcessId =
                processId;

        currentWindowTitle =
                windowTitle;

        currentUrl =
                normalizeValue(url);

        currentDomain =
                normalizeValue(domain);

        activityStartedAt =
                Instant.now();


        System.out.println();

        System.out.println(
                "APPLICATION STARTED"
        );


        System.out.println(
                "Process: "
                        + currentProcessName
        );


        System.out.println(
                "Process ID: "
                        + currentProcessId
        );


        System.out.println(
                "Window: "
                        + currentWindowTitle
        );


        System.out.println(
                "URL: "
                        + currentUrl
        );


        System.out.println(
                "Domain: "
                        + currentDomain
        );


        System.out.println(
                "Started At: "
                        + activityStartedAt
        );


        System.out.println(
                "------------------------------------------------"
        );
    }


    /**
     * Finishes the current activity and creates
     * an ApplicationInfo object.
     */
    private ApplicationInfo finishCurrentActivity() {

        if (activityStartedAt == null) {

            return null;
        }


        Instant activityEndedAt =
                Instant.now();


        long durationSeconds =
                Duration.between(
                        activityStartedAt,
                        activityEndedAt
                ).getSeconds();


        ApplicationInfo activity =
                new ApplicationInfo(
                        currentProcessName,
                        currentProcessId,
                        currentWindowTitle,
                        currentUrl,
                        currentDomain,
                        activityStartedAt,
                        activityEndedAt,
                        durationSeconds
                );


        System.out.println();

        System.out.println(
                "APPLICATION ENDED"
        );


        System.out.println(
                "Process: "
                        + activity.getProcessName()
        );


        System.out.println(
                "Process ID: "
                        + activity.getProcessId()
        );


        System.out.println(
                "Window: "
                        + activity.getWindowTitle()
        );


        System.out.println(
                "URL: "
                        + activity.getUrl()
        );


        System.out.println(
                "Domain: "
                        + activity.getDomain()
        );


        System.out.println(
                "Started At: "
                        + activity.getStartedAt()
        );


        System.out.println(
                "Ended At: "
                        + activity.getEndedAt()
        );


        System.out.println(
                "Duration: "
                        + formatDuration(
                        activity.getDurationSeconds()
                )
        );


        System.out.println(
                "Duration Seconds: "
                        + activity.getDurationSeconds()
        );


        System.out.println(
                "------------------------------------------------"
        );


        return activity;
    }


    /**
     * Safely compares two strings.
     */
    private boolean safeEquals(
            String first,
            String second
    ) {

        if (first == null &&
                second == null) {

            return true;
        }


        if (first == null ||
                second == null) {

            return false;
        }


        return first.equals(second);
    }


    /**
     * Converts null/blank values to empty string.
     */
    private String normalizeValue(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return "";
        }


        return value.trim();
    }


    /**
     * Formats seconds as HH:mm:ss.
     */
    private String formatDuration(
            long totalSeconds
    ) {

        long hours =
                totalSeconds / 3600;


        long minutes =
                (totalSeconds % 3600) / 60;


        long seconds =
                totalSeconds % 60;


        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );
    }
}