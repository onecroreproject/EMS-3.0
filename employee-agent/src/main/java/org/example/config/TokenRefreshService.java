package org.example.config;

public class TokenRefreshService {
    
    /**
     * Refreshes the JWT token with the server.
     * @param serverUrl the base URL of the backend server
     * @return true if the token was successfully refreshed, false otherwise
     */
    public static boolean refreshToken(String serverUrl) {
        String refreshToken = AgentTokenHolder.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String url = serverUrl + "/api/agent/refresh";
            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
                    
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                if (rootNode.has("success") && rootNode.get("success").asBoolean()) {
                    String newToken = rootNode.get("token").asText();
                    String newRefreshToken = rootNode.get("refreshToken").asText();
                    AgentTokenHolder.setToken(newToken);
                    AgentTokenHolder.setRefreshToken(newRefreshToken);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh token: " + e.getMessage());
        }
        
        return false;
    }
}
