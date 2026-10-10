package org.example.security;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.Structure;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Windows Credential Manager implementation for Remember Me.
 *
 * Username:
 *   Windows LPWSTR -> UTF-16LE / Wide String
 *
 * Password:
 *   CredentialBlob -> UTF-8 bytes
 *
 * Credentials are stored in Windows Credential Manager.
 */
public final class WindowsCredentialManager {

    private WindowsCredentialManager() {
    }

    private static final String TARGET_NAME =
            "EMS.EmployeeMonitoringAgent.RememberMe";

    private static final int CRED_TYPE_GENERIC = 1;

    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;

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
     *
     * Windows definition:
     *
     * typedef struct _CREDENTIALW {
     *     DWORD Flags;
     *     DWORD Type;
     *     LPWSTR TargetName;
     *     LPWSTR Comment;
     *     FILETIME LastWritten;
     *     DWORD CredentialBlobSize;
     *     LPBYTE CredentialBlob;
     *     DWORD Persist;
     *     DWORD AttributeCount;
     *     PCREDENTIAL_ATTRIBUTEW Attributes;
     *     LPWSTR TargetAlias;
     *     LPWSTR UserName;
     * } CREDENTIALW;
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
            return java.util.Arrays.asList(
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
     * Save username/password into Windows Credential Manager.
     */
    public static boolean saveCredentials(
            String username,
            String password
    ) {

        if (username == null || username.isBlank()) {
            System.err.println(
                    "Remember Me: username is empty."
            );
            return false;
        }

        if (password == null || password.isEmpty()) {
            System.err.println(
                    "Remember Me: password is empty."
            );
            return false;
        }

        Memory targetMemory = null;
        Memory usernameMemory = null;
        Memory passwordMemory = null;

        try {

            System.out.println(
                    "Saving Remember Me credentials to Windows Credential Manager..."
            );

            /*
             * Windows Credential Manager expects TargetName
             * and UserName as UTF-16 wide strings.
             */
            targetMemory = toWideMemory(TARGET_NAME);
            usernameMemory = toWideMemory(username);

            /*
             * Password is stored inside CredentialBlob.
             *
             * We intentionally store the password bytes as UTF-8.
             */
            byte[] passwordBytes =
                    password.getBytes(StandardCharsets.UTF_8);

            passwordMemory =
                    new Memory(passwordBytes.length);

            passwordMemory.write(
                    0,
                    passwordBytes,
                    0,
                    passwordBytes.length
            );

            NativeCredential credential =
                    new NativeCredential();

            credential.Flags = 0;
            credential.Type = CRED_TYPE_GENERIC;

            credential.TargetName = targetMemory;
            credential.Comment = null;

            credential.LastWrittenLow = 0;
            credential.LastWrittenHigh = 0;

            credential.CredentialBlobSize =
                    passwordBytes.length;

            credential.CredentialBlob =
                    passwordMemory;

            credential.Persist =
                    CRED_PERSIST_LOCAL_MACHINE;

            credential.AttributeCount = 0;
            credential.Attributes = null;

            credential.TargetAlias = null;

            /*
             * IMPORTANT:
             * UserName is a Windows LPWSTR.
             */
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
                        "Windows Credential Manager CredWrite failed."
                );

                System.err.println(
                        "Error code: " + error
                );

                return false;
            }

            System.out.println(
                    "Windows Credential Manager: credentials saved."
            );

            /*
             * Verify immediately.
             *
             * This also catches native structure/encoding
             * problems immediately.
             */
            String[] verification =
                    getSavedCredentials();

            if (verification != null
                    && verification.length >= 2
                    && username.equals(verification[0])
                    && password.equals(verification[1])) {

                System.out.println(
                        "Remember Me credential verification successful."
                );

                return true;
            }

            System.err.println(
                    "Remember Me credential verification failed."
            );

            return false;

        } catch (Exception e) {

            System.err.println(
                    "Unable to save Remember Me credentials."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            e.printStackTrace();

            return false;

        } finally {

            closeMemory(targetMemory);
            closeMemory(usernameMemory);
            closeMemory(passwordMemory);
        }
    }

    /**
     * Read username/password from Windows Credential Manager.
     */
    public static String[] getSavedCredentials() {

        PointerByReference credentialPointer =
                new PointerByReference();

        Pointer nativeCredential = null;

        try {

            System.out.println(
                    "Checking Windows Credential Manager for Remember Me credentials..."
            );

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

                System.err.println(
                        "Windows Credential Manager CredRead failed."
                );

                System.err.println(
                        "Target: " + TARGET_NAME
                );

                System.err.println(
                        "Error code: " + error
                );

                if (error == 1168) {

                    System.out.println(
                            "No saved EMS Remember Me credential exists for this Windows user."
                    );
                }

                return null;
            }

            nativeCredential =
                    credentialPointer.getValue();

            if (nativeCredential == null) {

                System.err.println(
                        "CredRead succeeded but returned a null credential pointer."
                );

                return null;
            }

            NativeCredential credential =
                    new NativeCredential(nativeCredential);

            /*
             * =====================================================
             * IMPORTANT FIX
             * =====================================================
             *
             * UserName is LPWSTR.
             *
             * DO NOT read it as UTF-8 bytes.
             *
             * getWideString() correctly reads Windows UTF-16.
             */
            String username = "";

            if (credential.UserName != null) {

                username =
                        credential.UserName.getWideString(0);
            }

            /*
             * CredentialBlob contains our UTF-8 password bytes.
             */
            String password = "";

            if (credential.CredentialBlob != null
                    && credential.CredentialBlobSize > 0) {

                byte[] passwordBytes =
                        credential.CredentialBlob.getByteArray(
                                0,
                                credential.CredentialBlobSize
                        );

                password =
                        new String(
                                passwordBytes,
                                StandardCharsets.UTF_8
                        );
            }

            System.out.println(
                    "Remembered credentials loaded successfully."
            );

            System.out.println(
                    "Remembered username loaded: "
                            + username
            );

            return new String[]{
                    username,
                    password
            };

        } catch (Exception e) {

            System.err.println(
                    "Unable to read Remember Me credentials."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            e.printStackTrace();

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
     * Delete Remember Me credentials.
     */
    public static boolean clearCredentials() {

        try {

            boolean result =
                    Advapi32.INSTANCE.CredDeleteW(
                            new WString(TARGET_NAME),
                            CRED_TYPE_GENERIC,
                            0
                    );

            if (result) {

                System.out.println(
                        "Windows Credential Manager: credentials cleared."
                );

                return true;
            }

            int error =
                    Native.getLastError();

            /*
             * 1168 = ERROR_NOT_FOUND.
             *
             * Treat this as already cleared.
             */
            if (error == 1168) {

                System.out.println(
                        "No Remember Me credentials to clear."
                );

                return true;
            }

            System.err.println(
                    "Windows Credential Manager CredDelete failed."
            );

            System.err.println(
                    "Error code: " + error
            );

            return false;

        } catch (Exception e) {

            System.err.println(
                    "Unable to clear Remember Me credentials."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            e.printStackTrace();

            return false;
        }
    }

    /**
     * Convert Java String to Windows UTF-16 memory.
     */
    private static Memory toWideMemory(String value) {

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

    private static void closeMemory(Memory memory) {

        if (memory != null) {

            try {
                memory.close();
            } catch (Exception ignored) {
            }
        }
    }
}