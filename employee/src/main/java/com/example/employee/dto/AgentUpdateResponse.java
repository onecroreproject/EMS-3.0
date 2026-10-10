package com.example.employee.dto;

public class AgentUpdateResponse {

    private String version;
    private String downloadUrl;
    private String sha256;
    private boolean mandatory;
    private String releaseNotes;

    public AgentUpdateResponse() {
    }

    public AgentUpdateResponse(String version, String downloadUrl, String sha256, boolean mandatory, String releaseNotes) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.sha256 = sha256;
        this.mandatory = mandatory;
        this.releaseNotes = releaseNotes;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
}