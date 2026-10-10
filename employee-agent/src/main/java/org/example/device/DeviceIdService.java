package org.example.device;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeviceIdService {

    public String generateDeviceId() {

        try {
            Path deviceIdFile = Path.of("C:\\ProgramData\\EmployeeAgent\\device-id.txt");
            
            // 1. Check if we already have a persisted device ID
            if (Files.exists(deviceIdFile)) {
                String savedId = Files.readString(deviceIdFile, StandardCharsets.UTF_8).trim();
                if (!savedId.isEmpty()) {
                    return savedId;
                }
            }

            // 2. Generate a new one if it doesn't exist
            String macAddress = getMacAddress();

            if (macAddress == null) {
                throw new IllegalStateException(
                        "Unable to detect device MAC address"
                );
            }

            String hash =
                    sha256(macAddress);

            // Use first 12 characters
            String newDeviceId = "DEV-" + hash.substring(0, 12).toUpperCase();
            
            // 3. Persist it so it never changes even if network adapters change
            Files.createDirectories(deviceIdFile.getParent());
            Files.writeString(deviceIdFile, newDeviceId, StandardCharsets.UTF_8);
            
            return newDeviceId;

        } catch (Exception e) {

            System.out.println(
                    "Device ID generation failed: "
                            + e.getMessage()
            );

            return null;
        }
    }

    private String getMacAddress()
            throws Exception {

        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {

            NetworkInterface networkInterface =
                    interfaces.nextElement();

            if (networkInterface.isLoopback()
                    || networkInterface.isVirtual()
                    || !networkInterface.isUp()) {
                continue;
            }

            byte[] mac =
                    networkInterface.getHardwareAddress();

            if (mac == null || mac.length == 0) {
                continue;
            }

            StringBuilder macAddress =
                    new StringBuilder();

            for (byte b : mac) {

                macAddress.append(
                        String.format(
                                "%02X",
                                b
                        )
                );
            }

            return macAddress.toString();
        }

        return null;
    }

    private String sha256(String value)
            throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hash =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder result =
                new StringBuilder();

        for (byte b : hash) {

            result.append(
                    String.format(
                            "%02x",
                            b
                    )
            );
        }

        return result.toString();
    }
}