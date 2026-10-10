package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AgentUpdatePublishRequest {

    @NotBlank(message = "Version is required")
    @Pattern(
            regexp = "\\d+(\\.\\d+){2,3}",
            message = "Version must be in format 1.0.0 or 1.0.0.0"
    )
    private String version;

    @NotBlank(message = "Platform is required")
    private String platform = "windows";

    private boolean mandatory;

    private String releaseNotes;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
}