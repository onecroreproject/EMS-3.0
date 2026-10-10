package org.example.security;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Structure;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Secure offline authorization storage.
 *
 * Uses Windows Credential Manager.
 *
 * IMPORTANT:
 * This is completely separate from Remember Me.
 *
 * Remember Me:
 * EMS.EmployeeMonitoringAgent.RememberMe
 *
 * Offline Authorization:
 * EMS.EmployeeMonitoringAgent.OfflineAuth
 */
public final class OfflineAuthorizationManager {

    private OfflineAuthorizationManager() {
    }

    private static final String TARGET_NAME =
            "EMS.EmployeeMonitoringAgent.OfflineAuth";

    private static final int CRED_TYPE_GENERIC = 1;

    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;

    /**
     * Default offline validity:
     * 7 days from the last successful online login.
     */
    private static final long OFFLINE_VALIDITY_DAYS = 7;

    /**
     * Windows Credential Manager native API.
     */
    private interface Advapi32 extends Library {

        Advapi32 INSTANCE =
                Native.load("Advapi32", Advapi32.class);

        boolean CredWriteW(
                Pointer credential,
                int flags
        );

        boolean CredReadW(
                WString targetName,
                int type,
                int flags,
                PointerByReference credential
        );

        boolean CredDeleteW(
                WString targetName,
                int type,
                int flags
        );

        int CredFree(
                Pointer credential
        );
    }

    /**
     * Native Windows CREDENTIALW structure.
     */
    public static class NativeCredential extends Structure {

        public int Flags;
        public int Type;

        public Pointer TargetName;
        public Pointer Comment;

        public int LastWrittenLow;
        public int LastWrittenHigh;

        public int CredentialBlobSize;
        public Pointer CredentialBlob;

        public int Persist;

        public int AttributeCount;
        public Pointer Attributes;

        public Pointer TargetAlias;
        public Pointer UserName;

        public NativeCredential() {
            super();
        }

        public NativeCredential(Pointer pointer) {
            super(pointer);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "Flags",
                    "Type",
                    "TargetName",
                    "Comment",
                    "LastWrittenLow",
                    "LastWrittenHigh",
                    "CredentialBlobSize",
                    "CredentialBlob",
                    "Persist",
                    "AttributeCount",
                    "Attributes",
                    "TargetAlias",
                    "UserName"
            );
        }
    }

    /**
     * Saves trusted offline authorization information.
     *
     * This method should ONLY be called after
     * a successful ONLINE login.
     */
    public static boolean saveAuthorization(
            String employeeId,
            String employeeCode,
            String username,
            String deviceId,
            String refreshToken
    ) {

        if (isBlank(employeeCode)
                || isBlank(username)
                || isBlank(deviceId)) {

            System.err.println(
                    "Offline authorization: required information is missing."
            );

            return false;
        }

        Instant authorizedAt = Instant.now();

        Instant expiresAt =
                authorizedAt.plus(
                        OFFLINE_VALIDITY_DAYS,
                        ChronoUnit.DAYS
                );

        /*
         * Store only trusted authorization information.
         *
         * DO NOT store the employee password here.
         */
        String authorizationData =
                employeeId + "|" +
                        employeeCode + "|" +
                        username + "|" +
                        deviceId + "|" +
                        authorizedAt + "|" +
                        expiresAt + "|" +
                        (refreshToken != null ? refreshToken : "");

        Memory targetMemory = null;
        Memory usernameMemory = null;
        Memory blobMemory = null;

        try {

            System.out.println(
                    "Saving offline authorization..."
            );

            targetMemory =
                    toWideMemory(TARGET_NAME);

            /*
             * Windows UserName field.
             *
             * We store the employee username/email.
             */
            usernameMemory =
                    toWideMemory(username);

            byte[] blobBytes =
                    authorizationData.getBytes(
                            StandardCharsets.UTF_8
                    );

            blobMemory =
                    new Memory(blobBytes.length);

            blobMemory.write(
                    0,
                    blobBytes,
                    0,
                    blobBytes.length
            );

            NativeCredential credential =
                    new NativeCredential();

            credential.Flags = 0;
            credential.Type = CRED_TYPE_GENERIC;

            credential.TargetName =
                    targetMemory;

            credential.Comment = null;

            credential.LastWrittenLow = 0;
            credential.LastWrittenHigh = 0;

            credential.CredentialBlobSize =
                    blobBytes.length;

            credential.CredentialBlob =
                    blobMemory;

            credential.Persist =
                    CRED_PERSIST_LOCAL_MACHINE;

            credential.AttributeCount = 0;
            credential.Attributes = null;

            credential.TargetAlias = null;

            credential.UserName =
                    usernameMemory;

            credential.write();

            boolean result =
                    Advapi32.INSTANCE.CredWriteW(
                            credential.getPointer(),
                            0
                    );

            if (!result) {

                int error =
                        Native.getLastError();

                System.err.println(
                        "Offline authorization CredWrite failed."
                );

                System.err.println(
                        "Error code: " + error
                );

                return false;
            }

            System.out.println(
                    "Offline authorization saved successfully."
            );

            System.out.println(
                    "Employee ID: " + employeeId
            );

            System.out.println(
                    "Device ID: " + deviceId
            );

            System.out.println(
                    "Offline authorization expires at: "
                            + expiresAt
            );

            return true;

        } catch (Exception e) {

            System.err.println(
                    "Unable to save offline authorization."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            e.printStackTrace();

            return false;

        } finally {

            closeMemory(targetMemory);
            closeMemory(usernameMemory);
            closeMemory(blobMemory);
        }
    }

    /**
     * Loads the stored offline authorization.
     *
     * Returns null when no authorization exists.
     */
    public static OfflineAuthorization getAuthorization() {

        PointerByReference credentialPointer =
                new PointerByReference();

        Pointer nativeCredential = null;

        try {

            boolean result =
                    Advapi32.INSTANCE.CredReadW(
                            new WString(TARGET_NAME),
                            CRED_TYPE_GENERIC,
                            0,
                            credentialPointer
                    );

            if (!result) {

                int error =
                        Native.getLastError();

                if (error == 1168) {

                    System.out.println(
                            "No offline authorization exists."
                    );

                } else {

                    System.err.println(
                            "Offline authorization CredRead failed."
                    );

                    System.err.println(
                            "Error code: " + error
                    );
                }

                return null;
            }

            nativeCredential =
                    credentialPointer.getValue();

            if (nativeCredential == null) {

                return null;
            }

            NativeCredential credential =
                    new NativeCredential(nativeCredential);

            if (credential.CredentialBlob == null
                    || credential.CredentialBlobSize <= 0) {

                return null;
            }

            byte[] blobBytes =
                    credential.CredentialBlob.getByteArray(
                            0,
                            credential.CredentialBlobSize
                    );

            String authorizationData =
                    new String(
                            blobBytes,
                            StandardCharsets.UTF_8
                    );

            return parseAuthorization(
                    authorizationData
            );

        } catch (Exception e) {

            System.err.println(
                    "Unable to read offline authorization."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            return null;

        } finally {

            if (nativeCredential != null) {

                try {

                    Advapi32.INSTANCE.CredFree(
                            nativeCredential
                    );

                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Checks whether valid offline authorization exists
     * for this employee and this device.
     */
    public static boolean isAuthorized(
            String employeeId,
            String deviceId
    ) {

        OfflineAuthorization authorization =
                getAuthorization();

        if (authorization == null) {

            System.out.println(
                    "Offline authorization: NOT FOUND."
            );

            return false;
        }

        /*
         * Prevent another employee from using
         * this PC's offline authorization.
         */
        if (!employeeId.equals(
                authorization.getEmployeeId())) {

            System.out.println(
                    "Offline authorization belongs to another employee."
            );

            return false;
        }

        /*
         * Prevent copying the authorization to
         * another registered device.
         */
        if (!deviceId.equals(
                authorization.getDeviceId())) {

            System.out.println(
                    "Offline authorization belongs to another device."
            );

            return false;
        }

        Instant now =
                Instant.now();

        if (now.isAfter(
                authorization.getExpiresAt())) {

            System.out.println(
                    "Offline authorization has expired."
            );

            return false;
        }

        System.out.println(
                "Offline authorization is valid."
        );

        System.out.println(
                "Expires at: "
                        + authorization.getExpiresAt()
        );

        return true;
    }

    /**
     * Removes offline authorization.
     *
     * Normally this should be used when the employee
     * is explicitly removed from the device or during
     * administrative cleanup.
     */
    public static boolean clearAuthorization() {

        try {

            boolean result =
                    Advapi32.INSTANCE.CredDeleteW(
                            new WString(TARGET_NAME),
                            CRED_TYPE_GENERIC,
                            0
                    );

            if (result) {

                System.out.println(
                        "Offline authorization cleared."
                );

                return true;
            }

            int error =
                    Native.getLastError();

            /*
             * 1168 = ERROR_NOT_FOUND.
             */
            if (error == 1168) {

                System.out.println(
                        "No offline authorization to clear."
                );

                return true;
            }

            System.err.println(
                    "Offline authorization CredDelete failed."
            );

            System.err.println(
                    "Error code: " + error
            );

            return false;

        } catch (Exception e) {

            System.err.println(
                    "Unable to clear offline authorization."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Parse the stored authorization data.
     */
    private static OfflineAuthorization parseAuthorization(
            String data
    ) {

        if (data == null || data.isBlank()) {
            return null;
        }

        String[] values =
                data.split(
                        "\\|",
                        -1
                );

        /*
         * Expected:
         *
         * 0 = employeeId
         * 1 = employeeCode
         * 2 = username
         * 3 = deviceId
         * 4 = authorizedAt
         * 5 = expiresAt
         * 6 = refreshToken
         */
        if (values.length < 6) {

            System.err.println(
                    "Invalid offline authorization format."
            );

            return null;
        }

        try {

            return new OfflineAuthorization(
                    values[0],
                    values[1],
                    values[2],
                    values[3],
                    Instant.parse(values[4]),
                    Instant.parse(values[5]),
                    values.length > 6 ? values[6] : null
            );

        } catch (Exception e) {

            System.err.println(
                    "Unable to parse offline authorization."
            );

            return null;
        }
    }

    /**
     * Convert Java String to Windows UTF-16 memory.
     */
    private static Memory toWideMemory(
            String value
    ) {

        Memory memory =
                new Memory(
                        (value.length() + 1L) * 2L
                );

        memory.setWideString(
                0,
                value
        );

        return memory;
    }

    private static void closeMemory(
            Memory memory
    ) {

        if (memory != null) {

            try {
                memory.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }

    /**
     * Offline authorization data.
     */
    public static class OfflineAuthorization {

        private final String employeeId;
        private final String employeeCode;
        private final String username;
        private final String deviceId;
        private final Instant authorizedAt;
        private final Instant expiresAt;
        private final String refreshToken;

        public OfflineAuthorization(
                String employeeId,
                String employeeCode,
                String username,
                String deviceId,
                Instant authorizedAt,
                Instant expiresAt,
                String refreshToken
        ) {

            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.username = username;
            this.deviceId = deviceId;
            this.authorizedAt = authorizedAt;
            this.expiresAt = expiresAt;
            this.refreshToken = refreshToken;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public String getUsername() {
            return username;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public Instant getAuthorizedAt() {
            return authorizedAt;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}