package org.example.config;

public class AgentTokenHolder {
    
    private static volatile String token;
    private static volatile String refreshToken;

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        AgentTokenHolder.token = token;
    }

    public static String getRefreshToken() {
        return refreshToken;
    }

    public static void setRefreshToken(String refreshToken) {
        AgentTokenHolder.refreshToken = refreshToken;
    }
    
    public static void clearTokens() {
        token = null;
        refreshToken = null;
    }
}
