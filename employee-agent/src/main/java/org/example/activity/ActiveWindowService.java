package org.example.activity;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class ActiveWindowService {

    // ============================================================
    // WINDOWS USER32
    // ============================================================

    private interface User32 extends Library {

        User32 INSTANCE = Native.load(
                "user32",
                User32.class
        );

        Pointer GetForegroundWindow();

        int GetWindowTextW(
                Pointer hWnd,
                char[] lpString,
                int nMaxCount
        );

        int GetWindowThreadProcessId(
                Pointer hWnd,
                int[] processId
        );
    }


    // ============================================================
    // GET ACTIVE WINDOW SNAPSHOT
    // ============================================================

    public ActiveWindowSnapshot getActiveWindowSnapshot() {

        Pointer windowHandle =
                User32.INSTANCE.GetForegroundWindow();

        if (windowHandle == null) {

            return new ActiveWindowSnapshot(
                    "",
                    0,
                    "",
                    "",
                    ""
            );
        }


        // --------------------------------------------------------
        // WINDOW TITLE
        // --------------------------------------------------------

        char[] windowTitle =
                new char[512];

        int titleLength =
                User32.INSTANCE.GetWindowTextW(
                        windowHandle,
                        windowTitle,
                        windowTitle.length
                );

        String title = "";

        if (titleLength > 0) {

            title =
                    new String(
                            windowTitle,
                            0,
                            titleLength
                    );
        }


        // --------------------------------------------------------
        // PROCESS ID
        // --------------------------------------------------------

        int[] processId =
                new int[1];

        User32.INSTANCE.GetWindowThreadProcessId(
                windowHandle,
                processId
        );

        int pid =
                processId[0];


        // --------------------------------------------------------
        // PROCESS NAME
        // --------------------------------------------------------

        String processName =
                getProcessName(pid);


        // --------------------------------------------------------
        // URL + DOMAIN
        // --------------------------------------------------------

        BrowserUrlInfo browserUrl =
                new BrowserUrlInfo(
                        "",
                        ""
                );

        if (isSupportedBrowser(processName)) {

            browserUrl =
                    getBrowserUrl(
                            windowHandle
                    );
        }


        // --------------------------------------------------------
        // RETURN SNAPSHOT
        // --------------------------------------------------------

        return new ActiveWindowSnapshot(
                processName,
                pid,
                title,
                browserUrl.getUrl(),
                browserUrl.getDomain()
        );
    }


    // ============================================================
    // SUPPORTED BROWSER
    // ============================================================

    private boolean isSupportedBrowser(
            String processName
    ) {

        if (processName == null) {

            return false;
        }

        String name =
                processName
                        .trim()
                        .toLowerCase(Locale.ROOT);

        return name.equals("chrome.exe")
                || name.equals("msedge.exe");
    }


    // ============================================================
    // GET BROWSER URL
    // ============================================================

    private BrowserUrlInfo getBrowserUrl(
            Pointer windowHandle
    ) {

        try {

            long hwnd =
                    Pointer.nativeValue(
                            windowHandle
                    );


            String script =
                    buildPowerShellScript(
                            hwnd
                    );


            Process process =
                    new ProcessBuilder(
                            "powershell.exe",
                            "-NoProfile",
                            "-NonInteractive",
                            "-ExecutionPolicy",
                            "Bypass",
                            "-Command",
                            script
                    )
                            .redirectErrorStream(true)
                            .start();


            String detectedUrl =
                    "";


            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream(),
                                            StandardCharsets.UTF_8
                                    )
                            )
            ) {

                String line;

                while (
                        (line = reader.readLine())
                                != null
                ) {

                    line =
                            line.trim();

                    if (line.isEmpty()) {

                        continue;
                    }


                    if (isValidBrowserUrl(line)) {

                        detectedUrl =
                                line;

                        break;
                    }
                }
            }


            process.waitFor();


            return createBrowserUrlInfo(
                    detectedUrl
            );

        } catch (Exception ignored) {

            /*
             * URL detection must never
             * break application tracking.
             */

            return new BrowserUrlInfo(
                    "",
                    ""
            );
        }
    }


    // ============================================================
    // POWERSHELL UI AUTOMATION
    // ============================================================

    private String buildPowerShellScript(
            long hwnd
    ) {

        return

                // ------------------------------------------------
                // Load UI Automation
                // ------------------------------------------------

                "Add-Type -AssemblyName UIAutomationClient; " +

                        "Add-Type -AssemblyName UIAutomationTypes; " +


                        // ------------------------------------------------
                        // Convert HWND
                        // ------------------------------------------------

                        "$hwnd = [IntPtr]::new(" +
                        hwnd +
                        "); " +


                        // ------------------------------------------------
                        // Get browser root
                        // ------------------------------------------------

                        "$root = " +
                        "[System.Windows.Automation.AutomationElement]" +
                        "::FromHandle($hwnd); " +


                        "if ($null -eq $root) { " +
                        "exit 0; " +
                        "}; " +


                        // ------------------------------------------------
                        // Search Edit controls
                        // ------------------------------------------------

                        "$condition = New-Object " +
                        "System.Windows.Automation.PropertyCondition(" +

                        "[System.Windows.Automation.AutomationElement]" +
                        "::ControlTypeProperty, " +

                        "[System.Windows.Automation.ControlType]::Edit" +

                        "); " +


                        "$elements = $root.FindAll(" +

                        "[System.Windows.Automation.TreeScope]::Descendants, " +

                        "$condition" +

                        "); " +


                        // ------------------------------------------------
                        // Inspect each Edit control
                        // ------------------------------------------------

                        "foreach ($element in $elements) { " +

                        "try { " +


                        // ------------------------------------------------
                        // Read Name
                        // ------------------------------------------------

                        "$name = ''; " +

                        "try { " +

                        "$name = $element.Current.Name; " +

                        "} catch {} " +


                        // ------------------------------------------------
                        // Read AutomationId
                        // ------------------------------------------------

                        "$automationId = ''; " +

                        "try { " +

                        "$automationId = " +
                        "$element.Current.AutomationId; " +

                        "} catch {} " +


                        // ------------------------------------------------
                        // METHOD 1 - ValuePattern
                        // ------------------------------------------------

                        "$value = ''; " +

                        "try { " +

                        "$pattern = " +

                        "$element.GetCurrentPattern(" +

                        "[System.Windows.Automation.ValuePattern]::Pattern" +

                        "); " +

                        "$value = " +
                        "$pattern.Current.Value; " +

                        "} catch {} " +


                        // ------------------------------------------------
                        // Check ValuePattern
                        // ------------------------------------------------

                        "if (-not [string]::IsNullOrWhiteSpace($value)) { " +

                        "if ($value -match '^https?://') { " +

                        "Write-Output $value; " +

                        "exit 0; " +

                        "} " +


                        "if ($value -match '^www\\.') { " +

                        "Write-Output $value; " +

                        "exit 0; " +

                        "} " +


                        "if ($value -match " +
                        "'^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$'" +
                        ") { " +

                        "Write-Output $value; " +

                        "exit 0; " +

                        "} " +

                        "} " +


                        // ------------------------------------------------
                        // METHOD 2 - LegacyIAccessible
                        // ------------------------------------------------

                        "$legacyValue = ''; " +

                        "try { " +

                        "$legacy = " +

                        "$element.GetCurrentPattern(" +

                        "[System.Windows.Automation.LegacyIAccessiblePattern]::Pattern" +

                        "); " +

                        "$legacyValue = " +

                        "$legacy.Current.Value; " +

                        "} catch {} " +


                        // ------------------------------------------------
                        // Check LegacyIAccessible
                        // ------------------------------------------------

                        "if (-not [string]::IsNullOrWhiteSpace($legacyValue)) { " +

                        "if ($legacyValue -match '^https?://') { " +

                        "Write-Output $legacyValue; " +

                        "exit 0; " +

                        "} " +


                        "if ($legacyValue -match '^www\\.') { " +

                        "Write-Output $legacyValue; " +

                        "exit 0; " +

                        "} " +

                        "} " +


                        // ------------------------------------------------
                        // METHOD 3 - Name
                        // ------------------------------------------------

                        "if (-not [string]::IsNullOrWhiteSpace($name)) { " +

                        "if ($name -match '^https?://') { " +

                        "Write-Output $name; " +

                        "exit 0; " +

                        "} " +


                        "if ($name -match '^www\\.') { " +

                        "Write-Output $name; " +

                        "exit 0; " +

                        "} " +


                        "if ($name -match " +
                        "'^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$'" +
                        ") { " +

                        "Write-Output $name; " +

                        "exit 0; " +

                        "} " +

                        "} " +


                        // ------------------------------------------------
                        // END TRY
                        // ------------------------------------------------

                        "} catch {} " +

                        "}";
    }


    // ============================================================
    // VALIDATE URL
    // ============================================================

    private boolean isValidBrowserUrl(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return false;
        }


        value =
                value.trim();


        if (value.startsWith(
                "http://"
        )) {

            return true;
        }


        if (value.startsWith(
                "https://"
        )) {

            return true;
        }


        if (value.startsWith(
                "www."
        )) {

            return true;
        }


        return value.matches(
                "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
        );
    }


    // ============================================================
    // CREATE URL + DOMAIN
    // ============================================================

    private BrowserUrlInfo createBrowserUrlInfo(
            String url
    ) {

        if (url == null ||
                url.isBlank()) {

            return new BrowserUrlInfo(
                    "",
                    ""
            );
        }


        url =
                url.trim();


        String normalizedUrl =
                url;


        String urlForParsing =
                url;


        // --------------------------------------------------------
        // Add protocol for URI parsing
        // --------------------------------------------------------

        if (!url.startsWith("http://") &&
                !url.startsWith("https://")) {

            urlForParsing =
                    "https://" + url;
        }


        String domain = "";


        try {

            URI uri =
                    URI.create(
                            urlForParsing
                    );


            domain =
                    uri.getHost();


            if (domain != null) {

                domain =
                        domain.toLowerCase(
                                Locale.ROOT
                        );
            }

        } catch (Exception ignored) {

            domain = "";
        }


        return new BrowserUrlInfo(
                normalizedUrl,
                domain
        );
    }


    // ============================================================
    // GET PROCESS NAME
    // ============================================================

    private String getProcessName(
            int processId
    ) {

        if (processId == 0) {

            return "";
        }


        try {

            Process process =
                    new ProcessBuilder(
                            "tasklist",
                            "/FI",
                            "PID eq " + processId,
                            "/FO",
                            "CSV",
                            "/NH"
                    )
                            .start();


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream()
                            )
                    );


            String line =
                    reader.readLine();


            if (line == null ||
                    line.isBlank()) {

                return "";
            }


            int firstQuote =
                    line.indexOf('"');


            int secondQuote =
                    line.indexOf(
                            '"',
                            firstQuote + 1
                    );


            if (firstQuote >= 0 &&
                    secondQuote > firstQuote) {

                return line.substring(
                        firstQuote + 1,
                        secondQuote
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Unable to detect process name: "
                            + e.getMessage()
            );
        }


        return "";
    }


    // ============================================================
    // BROWSER URL INFO
    // ============================================================

    private static class BrowserUrlInfo {

        private final String url;

        private final String domain;


        public BrowserUrlInfo(
                String url,
                String domain
        ) {

            this.url =
                    url;

            this.domain =
                    domain;
        }


        public String getUrl() {

            return url;
        }


        public String getDomain() {

            return domain;
        }
    }


    // ============================================================
    // ACTIVE WINDOW SNAPSHOT
    // ============================================================

    public static class ActiveWindowSnapshot {

        private final String processName;

        private final int processId;

        private final String windowTitle;

        private final String url;

        private final String domain;


        public ActiveWindowSnapshot(
                String processName,
                int processId,
                String windowTitle,
                String url,
                String domain
        ) {

            this.processName =
                    processName;

            this.processId =
                    processId;

            this.windowTitle =
                    windowTitle;

            this.url =
                    url;

            this.domain =
                    domain;
        }


        public String getProcessName() {

            return processName;
        }


        public int getProcessId() {

            return processId;
        }


        public String getWindowTitle() {

            return windowTitle;
        }


        public String getUrl() {

            return url;
        }


        public String getDomain() {

            return domain;
        }


        @Override
        public String toString() {

            return "ActiveWindowSnapshot{" +

                    "processName='" +
                    processName +
                    '\'' +

                    ", processId=" +
                    processId +

                    ", windowTitle='" +
                    windowTitle +
                    '\'' +

                    ", url='" +
                    url +
                    '\'' +

                    ", domain='" +
                    domain +
                    '\'' +

                    '}';
        }
    }


    // ============================================================
    // TEST MAIN
    // ============================================================

    public static void main(
            String[] args
    ) {

        ActiveWindowService service =
                new ActiveWindowService();


        while (true) {

            ActiveWindowSnapshot snapshot =
                    service.getActiveWindowSnapshot();


            System.out.println(
                    "Process Name: "
                            + snapshot.getProcessName()
            );


            System.out.println(
                    "Process ID: "
                            + snapshot.getProcessId()
            );


            System.out.println(
                    "Window Title: "
                            + snapshot.getWindowTitle()
            );


            System.out.println(
                    "URL: "
                            + snapshot.getUrl()
            );


            System.out.println(
                    "Domain: "
                            + snapshot.getDomain()
            );


            System.out.println(
                    "------------------------------------------------"
            );


            try {

                Thread.sleep(2000);

            } catch (InterruptedException e) {

                Thread.currentThread()
                        .interrupt();

                break;
            }
        }
    }
}