package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AgentConfig {

    private static final String CONFIG_DIRECTORY =
            "C:\\ProgramData\\EmployeeAgent";

    private static final String CONFIG_FILE =
            "config.properties";

    private final Properties properties = new Properties();

    public AgentConfig() {
        loadConfig();
    }

    private void loadConfig() {

        Path configDirectory = Paths.get(CONFIG_DIRECTORY);
        Path configPath = configDirectory.resolve(CONFIG_FILE);

        try {

            /*
             * 1. If external configuration exists,
             *    always use it.
             *
             * This means existing user configuration
             * will NEVER be overwritten.
             */
            if (Files.exists(configPath)) {

                loadFromFile(configPath);

                String currentUrl = properties.getProperty("server.url", "").trim();
                if (currentUrl.isEmpty()) {

                    System.out.println(
                            "Detected empty server.url in "
                                    + configPath
                                    + ". Auto-syncing to bundled configuration..."
                    );

                    try (InputStream inputStream =
                                 AgentConfig.class.getResourceAsStream(
                                         "/config.properties"
                                 )) {

                        if (inputStream != null) {
                            Files.copy(
                                    inputStream,
                                    configPath,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            );
                            loadFromFile(configPath);
                            System.out.println(
                                    "Successfully updated "
                                            + configPath
                                            + " to: "
                                            + properties.getProperty("server.url")
                            );
                        }
                    } catch (Exception ex) {
                        System.out.println(
                                "Failed to auto-update config.properties: "
                                        + ex.getMessage()
                        );
                    }
                }

                System.out.println(
                        "Configuration loaded from: "
                                + configPath
                );

                return;
            }

            /*
             * 2. Configuration does not exist.
             *    Create the ProgramData directory.
             */
            Files.createDirectories(configDirectory);

            /*
             * 3. Load the default configuration
             *    packaged inside the application JAR.
             */
            try (InputStream inputStream =
                         AgentConfig.class.getResourceAsStream(
                                 "/config.properties"
                         )) {

                if (inputStream == null) {

                    throw new IllegalStateException(
                            "Default config.properties was not found "
                                    + "inside the application."
                    );
                }

                /*
                 * 4. Create the external configuration file.
                 *
                 * Files.copy() does not overwrite an existing file.
                 */
                Files.copy(
                        inputStream,
                        configPath
                );
            }

            /*
             * 5. Load the newly created configuration.
             */
            loadFromFile(configPath);

            System.out.println(
                    "Default configuration created at: "
                            + configPath
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to initialize configuration: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void loadFromFile(Path configPath)
            throws IOException {

        try (InputStream inputStream =
                     Files.newInputStream(configPath)) {

            properties.load(inputStream);

            if (properties.containsKey("workday.required-hours")) {
                try {
                    requiredHours = Integer.parseInt(properties.getProperty("workday.required-hours").trim());
                } catch (Exception ignored) {}
            }
            if (properties.containsKey("idle.grace-minutes")) {
                try {
                    idleGraceMinutes = Integer.parseInt(properties.getProperty("idle.grace-minutes").trim());
                } catch (Exception ignored) {}
            }
            if (properties.containsKey("idle.auto-work-end-minutes")) {
                try {
                    autoWorkEndMinutes = Long.parseLong(properties.getProperty("idle.auto-work-end-minutes").trim());
                } catch (Exception ignored) {}
            }
        }
    }

    public String getServerUrl() {

        String serverUrl =
                properties.getProperty("server.url");

        if (serverUrl == null || serverUrl.trim().isEmpty()) {

            throw new IllegalStateException(
                    "server.url is missing in config.properties"
            );
        }

        return serverUrl.trim();
    }

    private int requiredHours = 9;
    private int idleGraceMinutes = 2;
    private long autoWorkEndMinutes = 60;
    private int heartbeatIntervalSeconds = 10;
    private int loginReminderIntervalSeconds = 60;

    // Dynamic Screen Monitoring Settings
    private boolean captureEnabled = true;
    private int captureInterval = 5; // minutes
    private int imageQuality = 75; // percentage
    private String imageFormat = "JPEG";
    private boolean captureDuringWorking = true;
    private boolean captureDuringIdle = true;
    private boolean captureDuringBreak = true;
    private boolean captureWhenLoggedOut = false;

    // Offline & Sync
    private boolean enableOfflineCapture = true;
    private int localStorageLimit = 2; // GB
    private int retryInterval = 5; // minutes
    private int maxRetryAttempts = 10;
    private boolean autoSyncOnStartup = true;

    // Attendance & Idle Policy
    private int markOfflineAfterContinuousIdle = 30; // minutes
    private boolean enableAutomaticOfflineStatus = true;
    private int configurationVersion = 1;
    private String versionString = "v2.1.0";

    public void fetchDynamicConfig(String serverUrl) {
        try {
            java.net.URL url = new java.net.URL(serverUrl + "/api/agent/config");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                String json = content.toString();

                requiredHours = parseJsonInt(json, "requiredHours", requiredHours);
                idleGraceMinutes = parseJsonInt(json, "idleGraceMinutes", idleGraceMinutes);
                autoWorkEndMinutes = parseJsonLong(json, "autoWorkEndMinutes", autoWorkEndMinutes);
                heartbeatIntervalSeconds = parseJsonInt(json, "heartbeatIntervalSeconds", heartbeatIntervalSeconds);
                loginReminderIntervalSeconds = parseJsonInt(json, "loginReminderIntervalSeconds", loginReminderIntervalSeconds);

                // Screen capture properties
                captureEnabled = parseJsonBoolean(json, "captureEnabled", captureEnabled);
                captureInterval = parseJsonInt(json, "captureInterval", captureInterval);
                imageQuality = parseJsonInt(json, "imageQuality", imageQuality);
                imageFormat = parseJsonString(json, "imageFormat", imageFormat);
                captureDuringWorking = parseJsonBoolean(json, "captureDuringWorking", captureDuringWorking);
                captureDuringIdle = parseJsonBoolean(json, "captureDuringIdle", captureDuringIdle);
                captureDuringBreak = parseJsonBoolean(json, "captureDuringBreak", captureDuringBreak);
                captureWhenLoggedOut = parseJsonBoolean(json, "captureWhenLoggedOut", captureWhenLoggedOut);

                // Offline & Sync properties
                enableOfflineCapture = parseJsonBoolean(json, "enableOfflineCapture", enableOfflineCapture);
                localStorageLimit = parseJsonInt(json, "localStorageLimit", localStorageLimit);
                retryInterval = parseJsonInt(json, "retryInterval", retryInterval);
                maxRetryAttempts = parseJsonInt(json, "maxRetryAttempts", maxRetryAttempts);
                autoSyncOnStartup = parseJsonBoolean(json, "autoSyncOnStartup", autoSyncOnStartup);

                // Attendance policy properties
                markOfflineAfterContinuousIdle = parseJsonInt(json, "markOfflineAfterContinuousIdle", markOfflineAfterContinuousIdle);
                enableAutomaticOfflineStatus = parseJsonBoolean(json, "enableAutomaticOfflineStatus", enableAutomaticOfflineStatus);
                configurationVersion = parseJsonInt(json, "configurationVersion", configurationVersion);
                versionString = parseJsonString(json, "versionString", versionString);

                System.out.println("Dynamic configuration loaded successfully from server (Version: " + versionString + ").");
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch dynamic config. Using default fallbacks: " + e.getMessage());
        }
    }

    private int parseJsonInt(String json, String key, int defaultVal) {
        try {
            String pattern = "\"" + key + "\":";
            if (json.contains(pattern)) {
                String sub = json.substring(json.indexOf(pattern) + pattern.length()).trim();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < sub.length(); i++) {
                    char c = sub.charAt(i);
                    if (Character.isDigit(c) || (c == '-' && sb.length() == 0)) {
                        sb.append(c);
                    } else if (c == ',' || c == '}' || c == ' ' || c == '\n') {
                        break;
                    }
                }
                if (sb.length() > 0) {
                    return Integer.parseInt(sb.toString());
                }
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private long parseJsonLong(String json, String key, long defaultVal) {
        try {
            String pattern = "\"" + key + "\":";
            if (json.contains(pattern)) {
                String sub = json.substring(json.indexOf(pattern) + pattern.length()).trim();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < sub.length(); i++) {
                    char c = sub.charAt(i);
                    if (Character.isDigit(c) || (c == '-' && sb.length() == 0)) {
                        sb.append(c);
                    } else if (c == ',' || c == '}' || c == ' ' || c == '\n') {
                        break;
                    }
                }
                if (sb.length() > 0) {
                    return Long.parseLong(sb.toString());
                }
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private boolean parseJsonBoolean(String json, String key, boolean defaultVal) {
        try {
            String pattern = "\"" + key + "\":";
            if (json.contains(pattern)) {
                String sub = json.substring(json.indexOf(pattern) + pattern.length()).trim();
                if (sub.startsWith("true")) return true;
                if (sub.startsWith("false")) return false;
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private String parseJsonString(String json, String key, String defaultVal) {
        try {
            String pattern = "\"" + key + "\":";
            if (json.contains(pattern)) {
                String sub = json.substring(json.indexOf(pattern) + pattern.length()).trim();
                if (sub.startsWith("\"")) {
                    int nextQuote = sub.indexOf("\"", 1);
                    if (nextQuote != -1) {
                        return sub.substring(1, nextQuote);
                    }
                }
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    public int getRequiredWorkHours() {
        return requiredHours;
    }

    public int getIdleGraceMinutes() {
        return idleGraceMinutes;
    }

    public long getAutoWorkEndIdleMinutes() {
        return autoWorkEndMinutes;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public int getLoginReminderIntervalSeconds() {
        return loginReminderIntervalSeconds;
    }

    public boolean isCaptureEnabled() {
        return captureEnabled;
    }

    public int getCaptureIntervalMinutes() {
        return captureInterval;
    }

    public int getImageQuality() {
        return imageQuality;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public boolean isCaptureDuringWorking() {
        return captureDuringWorking;
    }

    public boolean isCaptureDuringIdle() {
        return captureDuringIdle;
    }

    public boolean isCaptureDuringBreak() {
        return captureDuringBreak;
    }

    public boolean isCaptureWhenLoggedOut() {
        return captureWhenLoggedOut;
    }

    public boolean isEnableOfflineCapture() {
        return enableOfflineCapture;
    }

    public int getLocalStorageLimitGb() {
        return localStorageLimit;
    }

    public int getRetryIntervalMinutes() {
        return retryInterval;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public boolean isAutoSyncOnStartup() {
        return autoSyncOnStartup;
    }

    public int getMarkOfflineAfterContinuousIdleMinutes() {
        return markOfflineAfterContinuousIdle;
    }

    public boolean isEnableAutomaticOfflineStatus() {
        return enableAutomaticOfflineStatus;
    }

    public int getConfigurationVersion() {
        return configurationVersion;
    }

    public String getVersionString() {
        return versionString;
    }
}