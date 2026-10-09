package com.example.employee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admin_config")
public class AdminConfig {
    @Id
    private String id;
    private String email;
    private String mfaSecret;

    public AdminConfig() {}

    public AdminConfig(String id, String email, String mfaSecret) {
        this.id = id;
        this.email = email;
        this.mfaSecret = mfaSecret;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
}
