package org.example.device;

public class DeviceInfoService {

    public String getHostname() {

        return System.getenv("COMPUTERNAME");
    }

    public String getOperatingSystem() {

        return System.getProperty("os.name");
    }

    public String getOsVersion() {

        return System.getProperty("os.version");
    }

    public String getArchitecture() {

        return System.getProperty("os.arch");
    }

    public String getIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
