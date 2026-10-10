package org.example.commucnication;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public class AgentLoginService {

    private final HttpClient httpClient;
    private final String serverUrl;

    public AgentLoginService(String serverUrl) {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.serverUrl = serverUrl;
    }

    public LoginResult login(String username, String password) {

        try {

            String url = serverUrl + "/api/agent/login";

            String requestBody = """
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """.formatted(username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            System.out.println("---------------------------------");
            System.out.println("Connecting to EMS server...");
            System.out.println("URL: " + url);

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println("Agent Login Response:");
            System.out.println("HTTP Status: " + response.statusCode());
            System.out.println("Response: " + response.body());

            /*
             * =========================================================
             * SUCCESS
             * =========================================================
             */

            if (response.statusCode() == 200) {

                String responseBody = response.body();

                String employeeId =
                        extractValue(responseBody, "employeeId");

                String employeeCode =
                        extractValue(responseBody, "employeeCode");

                String employeeName =
                        extractValue(responseBody, "employeeName");

                String email =
                        extractValue(responseBody, "email");

                String token =
                        extractValue(responseBody, "token");

                String refreshToken =
                        extractValue(responseBody, "refreshToken");

                System.out.println("Employee ID: " + employeeId);
                System.out.println("Employee Code: " + employeeCode);
                System.out.println("Employee Name: " + employeeName);
                System.out.println("Email: " + email);
                System.out.println("Has Token: " + (token != null && !token.isBlank()));
                System.out.println("Has Refresh Token: " + (refreshToken != null && !refreshToken.isBlank()));

                return new LoginResult(
                        true,
                        employeeId,
                        employeeCode,
                        employeeName,
                        email,
                        token,
                        refreshToken,
                        LoginStatus.ONLINE_SUCCESS
                );
            }

            /*
             * =========================================================
             * INVALID CREDENTIALS
             * =========================================================
             *
             * 401 means the server was reached successfully,
             * but authentication failed.
             *
             * DO NOT use offline login for this case.
             */

            if (response.statusCode() == 401) {

                System.out.println(
                        "Login rejected by server: Invalid credentials."
                );

                return new LoginResult(
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LoginStatus.INVALID_CREDENTIALS
                );
            }

            /*
             * =========================================================
             * OTHER SERVER ERROR
             * =========================================================
             */

            System.out.println(
                    "EMS server returned HTTP "
                            + response.statusCode()
            );

            return new LoginResult(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LoginStatus.SERVER_ERROR
            );

        } catch (HttpTimeoutException e) {

            System.out.println(
                    "EMS server connection timed out."
            );

            return createServerUnavailableResult();

        } catch (ConnectException e) {

            System.out.println(
                    "EMS server is unavailable."
            );

            return createServerUnavailableResult();

        } catch (Exception e) {

            /*
             * Network-related failures can also arrive wrapped
             * inside another exception.
             */

            System.out.println(
                    "Agent login failed: " + e.getMessage()
            );

            return createServerUnavailableResult();
        }
    }

    private LoginResult createServerUnavailableResult() {

        return new LoginResult(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                LoginStatus.SERVER_UNAVAILABLE
        );
    }

    private String extractValue(
            String json,
            String key) {
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /*
     * =============================================================
     * LOGIN STATUS
     * =============================================================
     */

    public enum LoginStatus {

        ONLINE_SUCCESS,

        OFFLINE_SUCCESS,

        INVALID_CREDENTIALS,

        SERVER_UNAVAILABLE,

        SERVER_ERROR
    }

    /*
     * =============================================================
     * LOGIN RESULT
     * =============================================================
     */

    public static class LoginResult {

        private final boolean success;

        private final String employeeId;

        private final String employeeCode;

        private final String employeeName;

        private final String email;

        private final String token;

        private final String refreshToken;

        private final LoginStatus status;

        public LoginResult(
                boolean success,
                String employeeId,
                String employeeCode,
                String employeeName,
                String email,
                String token,
                String refreshToken,
                LoginStatus status) {

            this.success = success;
            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.employeeName = employeeName;
            this.email = email;
            this.token = token;
            this.refreshToken = refreshToken;
            this.status = status;
        }

        public static LoginResult offlineSuccess(
                String employeeId,
                String employeeCode,
                String username,
                String refreshToken) {

            return new LoginResult(
                    true,
                    employeeId,
                    employeeCode,
                    null,
                    username,
                    null,
                    refreshToken,
                    LoginStatus.OFFLINE_SUCCESS
            );
        }

        public boolean isSuccess() {
            return success;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public String getEmail() {
            return email;
        }

        public String getToken() {
            return token;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public LoginStatus getStatus() {
            return status;
        }

        public boolean isServerUnavailable() {
            return status == LoginStatus.SERVER_UNAVAILABLE;
        }

        public boolean isInvalidCredentials() {
            return status == LoginStatus.INVALID_CREDENTIALS;
        }
    }
}