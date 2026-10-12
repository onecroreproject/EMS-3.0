package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "active_sessions")
public class ActiveSession {
    @Id
    private String id;
    private String email;
    private String token;
    private String refreshToken;
    private String userAgent;
    private LocalDateTime loginTime;

    public ActiveSession() {}

    public ActiveSession(String email, String token, String userAgent, LocalDateTime loginTime) {
        this.email = email;
        this.token = token;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
    }

    public ActiveSession(String email, String token, String refreshToken, String userAgent, LocalDateTime loginTime) {
        this.email = email;
        this.token = token;
        this.refreshToken = refreshToken;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}
