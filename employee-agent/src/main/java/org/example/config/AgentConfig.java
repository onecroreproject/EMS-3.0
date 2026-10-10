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
                
                // Simple parsing since we can't use complex JSON libs easily here
                if (json.contains("\"requiredHours\":")) {
                    String part = json.split("\"requiredHours\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                    requiredHours = Integer.parseInt(part);
                }
                if (json.contains("\"idleGraceMinutes\":")) {
                    String part = json.split("\"idleGraceMinutes\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                    idleGraceMinutes = Integer.parseInt(part);
                }
                if (json.contains("\"autoWorkEndMinutes\":")) {
                    String part = json.split("\"autoWorkEndMinutes\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                    autoWorkEndMinutes = Long.parseLong(part);
                }
                if (json.contains("\"heartbeatIntervalSeconds\":")) {
                    String part = json.split("\"heartbeatIntervalSeconds\":")[1].split(",")[0].replaceAll("[^0-9]", "");
                    heartbeatIntervalSeconds = Integer.parseInt(part);
                }
                if (json.contains("\"loginReminderIntervalSeconds\":")) {
                    String part = json.split("\"loginReminderIntervalSeconds\":")[1].split("}")[0].replaceAll("[^0-9]", "");
                    loginReminderIntervalSeconds = Integer.parseInt(part);
                }
                System.out.println("Dynamic configuration loaded successfully from server.");
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch dynamic config. Using default fallbacks: " + e.getMessage());
        }
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
}