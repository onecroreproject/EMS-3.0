package org.example.ui;

import org.example.AgentApplication;
import org.example.attendance.AttendanceEvent;
import org.example.attendance.AttendanceEventFactory;
import org.example.attendance.AttendanceEventSender;
import org.example.attendance.AttendanceSendResult;
import org.example.attendance.AttendanceEventType;
import org.example.commucnication.AgentLoginService;
import org.example.activity.UserActivityMonitor;
import org.example.config.AgentConfig;

import org.example.ui.theme.UITheme;
import org.example.ui.theme.Icons;
import org.example.ui.theme.Icons.IconType;
import static org.example.ui.theme.UITheme.*;
import static org.example.ui.theme.Icons.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Employee Monitoring Agent - Employee Workspace
 *
 * First working condition:
 *
 * LOGIN
 *   Ã¢â€ â€œ
 * WORKING
 *   Ã¢â€ â€œ
 * Work timer counts
 *
 * BREAK:
 *   WORKING Ã¢â€ â€™ BREAK Ã¢â€ â€™ WORKING
 *
 * LUNCH:
 *   WORKING Ã¢â€ â€™ LUNCH Ã¢â€ â€™ WORKING
 *
 * IDLE:
 *   120 seconds without keyboard/mouse activity
 *   WORKING Ã¢â€ â€™ IDLE
 *
 * activity resumes:
 *   IDLE Ã¢â€ â€™ WORKING
 *
 * Attendance events are sent using the same device ID
 * used by AgentApplication.
 */
public class EmployeeWorkspaceWindow extends JFrame {

    // =========================================================
    // WINDOW
    // =========================================================
    private static final int WINDOW_WIDTH = 980;

    private static final int WINDOW_HEIGHT = 550;


    // =========================================================
    // LOGIN / EMPLOYEE
    // =========================================================

    private final AgentLoginService.LoginResult loginResult;

    private final String employeeId;

    private final String employeeCode;

    private final String employeeDisplayName;


    // =========================================================
    // DEVICE / SERVER
    // =========================================================

    private final String deviceId;

    private final String serverUrl;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();


    // =========================================================
    // ATTENDANCE
    // =========================================================

    private final AttendanceEventFactory eventFactory;

    private final AttendanceEventSender eventSender;


    // =========================================================
    // UI COMPONENTS
    // =========================================================

    private JLabel welcomeLabel;

    private JLabel empCodeLabel;

    private JLabel dateLabel;

    private JLabel timeHeaderLabel;

    private JLabel onlineBadgeLabel;

    private JPanel onlinePill;

    private JPanel onlineDot;

    // =========================================================
    // OTA UPDATE
    // =========================================================

    private JPanel updatePanel;

    private JLabel updateVersionLabel;

    private JLabel updateMessageLabel;

    private JButton updateNowButton;

    private volatile boolean updateAvailable = false;

    private volatile boolean updateInstalling = false;


    private JComboBox<String> clientCombo;

    private JComboBox<String> projectCombo;

    private JComboBox<String> taskCombo;

    /** Existing /api/tasks title -> Mongo task id mapping. */
    private final Map<String, String> taskIdByTitle =
            new HashMap<>();

    /** Backend employee-task work session id. */
    private volatile String currentTaskWorkId;

    /** Currently selected Mongo task id. */
    private volatile String currentTaskId;

    /** Title of the task currently associated with the active timer. */
    private volatile String currentTaskTitle;

    /** Prevents multiple Logout clicks while WORK_ENDED is being sent. */
    private volatile boolean logoutInProgress = false;

    /** True while the agent is checking the backend for an existing running task. */
    private volatile boolean taskWorkStateLoading = false;


    private JLabel workTimeLabel;

    private JButton workTimerButton;


    // Current status

    private JLabel currentStatusBadge;

    private JPanel statusDotPanel;

    private JLabel idleTimerHeaderLabel;

    private JLabel currentTaskLabel;

    private JLabel startedAtLabel;

    private JLabel durationLabel;

    private JLabel idleTimeLabel;


    // Attendance cards

    private JLabel clockInMiniLabel;

    private JLabel workHoursMiniLabel;

    private JLabel breakTimeMiniLabel;

    private JLabel lunchTimeMiniLabel;

    private JLabel idleTimeMiniLabel;


    private JButton breakButton;

    private JButton lunchButton;

    private JLabel lastActivityLabel;


    // =========================================================
    // LOCAL WORKSPACE STATE
    // =========================================================

    private Timer uiTimer;


    private Color currentStatusDotColor =
            TEXT_MUTED;


    /*
     * Existing Windows activity monitor.
     *
     * This is the same activity-monitor concept already
     * used by AgentApplication.
     */
    private final UserActivityMonitor activityMonitor =
            new UserActivityMonitor();


    /*
     * 2 minutes = 120 seconds.
     *
     * This value is only used by the UI to reflect
     * the already-existing activity monitor.
     *
     * The actual AgentApplication IdleDetectionService
     * continues to send the real IDLE_STARTED/IDLE_ENDED
     * attendance events.
     */
    private final long idleThresholdSeconds;


    // =========================================================
    // WORK STATE
    // =========================================================

    private volatile boolean workRunning =
            false;

    private volatile boolean breakRunning =
            false;

    private volatile boolean lunchRunning =
            false;


    // =========================================================
    // ACCUMULATED TIME
    // =========================================================

    /*
     * Overall active task-work time for this employee session.
     * This NEVER resets when switching tasks.
     */
    private long employeeWorkAccumulatedSeconds =
            0L;

    /*
     * Current/last task-work segment duration.
     */
    private long workAccumulatedSeconds =
            0L;

    private long breakAccumulatedSeconds =
            0L;

    private long lunchAccumulatedSeconds =
            0L;

    private long idleAccumulatedSeconds =
            0L;




    // =========================================================
    // SEGMENT START TIMES
    // =========================================================

    private long workSegmentStartNanos;

    /*
     * Segment used only for the employee-level Work Time total.
     */
    private long employeeWorkSegmentStartNanos;

    private long breakSegmentStartNanos;

    private long lunchSegmentStartNanos;

    /*
     * Tracks an IDLE period that starts immediately after
     * Break or Lunch ends.
     */
    private volatile long idleSegmentStartNanos = 0L;



    private volatile boolean idleAfterBreakLunch = false;


    // =========================================================
    // WORKSPACE START
    // =========================================================

    /**
     * Actual WORK_STARTED attendance timestamp.
     */
    private long workStartedAtMillis;

    /**
     * Workspace creation timestamp.
     */
    private long workspaceStartedAtMillis;

    /*
     * Start time of the current/last task-work session.
     */
    private long currentTaskStartedAtMillis =
            0L;


    // =========================================================
    // CURRENT STATUS
    // =========================================================

    private String localStatus =
            "WORKING";


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EmployeeWorkspaceWindow(
            AgentLoginService.LoginResult loginResult
    ) {

        this(
                loginResult,
                "",
                System.currentTimeMillis()
        );
    }


    public EmployeeWorkspaceWindow(
            AgentLoginService.LoginResult loginResult,
            String deviceId
    ) {

        this(
                loginResult,
                deviceId,
                System.currentTimeMillis()
        );
    }


    /**
     * Main constructor.
     */
    public EmployeeWorkspaceWindow(
            AgentLoginService.LoginResult loginResult,
            String deviceId,
            long workStartedAtMillis
    ) {

        this.loginResult =
                loginResult;


        this.employeeId =
                loginResult != null
                        ? safe(
                        loginResult.getEmployeeId()
                )
                        : "";


        this.employeeCode =
                loginResult != null
                        ? safe(
                        loginResult.getEmployeeCode()
                )
                        : "1000";


        this.employeeDisplayName =
                loginResult != null
                        && loginResult.getEmployeeName() != null
                        && !loginResult
                        .getEmployeeName()
                        .trim()
                        .isEmpty()

                        ? loginResult
                        .getEmployeeName()
                        .trim()

                        : "Santhosh";


        this.deviceId =
                safe(deviceId);


        AgentConfig config =
                new AgentConfig();


        this.serverUrl =
                config.getServerUrl();


        this.workStartedAtMillis =
                workStartedAtMillis > 0L
                        ? workStartedAtMillis
                        : System.currentTimeMillis();

        this.workspaceStartedAtMillis =
                System.currentTimeMillis();


        /*
         * IMPORTANT:
         *
         * Use the same device ID generated
         * by AgentApplication.
         */
        this.eventFactory =
                new AttendanceEventFactory(
                        employeeCode,
                        this.deviceId
                );


        this.eventSender =
                new AttendanceEventSender(
                        serverUrl
                );


        /*
         * 120 seconds.
         */
        this.idleThresholdSeconds =
                120L;


        /*
         * Workspace starts WORKING.
         */
        this.workSegmentStartNanos =
                0L;

        this.employeeWorkSegmentStartNanos =
                0L;

        this.localStatus =
                "WORKING";


        // =====================================================
        // WINDOW CONFIGURATION
        // =====================================================

        setTitle(
                "Employee Monitoring Agent"
        );


        setSize(
                WINDOW_WIDTH,
                WINDOW_HEIGHT
        );


        setMinimumSize(
                new Dimension(
                        WINDOW_WIDTH,
                        WINDOW_HEIGHT
                )
        );


        setResizable(false);


        setLocationRelativeTo(null);


        setUndecorated(true);

        // Set the application icon for the taskbar / Alt+Tab switcher.
        AppIcon.apply(this);


        setDefaultCloseOperation(
                JFrame.DO_NOTHING_ON_CLOSE
        );


        setShape(
                new RoundRectangle2D.Double(
                        0,
                        0,
                        WINDOW_WIDTH,
                        WINDOW_HEIGHT,
                        16,
                        16
                )
        );


        // =====================================================
        // ROOT
        // =====================================================

        JPanel root =
                new JPanel(
                        new BorderLayout()
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        super.paintComponent(g);

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();

                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );

                        g2.setColor(
                                BORDER_CARD
                        );

                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                16,
                                16
                        );

                        g2.dispose();
                    }
                };


        root.setBackground(
                BG_CANVAS
        );


        root.add(
                createTitleBar(),
                BorderLayout.NORTH
        );


        root.add(
                createMainContent(),
                BorderLayout.CENTER
        );


        setContentPane(root);


        // =====================================================
        // INITIAL DATA
        // =====================================================

        loadDynamicWorkspaceData();


        /*
         * IMPORTANT:
         *
         * Never initialize the UI as IDLE or SYNCING.
         */
        updateStatus(
                "WORKING"
        );


        updateLocalUi();


        startUiTimer();

        // =====================================================
        // OTA UPDATE CHECK
        // =====================================================

        SwingUtilities.invokeLater(
                () -> {

                    Timer otaDelayTimer =
                            new Timer(
                                    2000,
                                    e -> checkForWorkspaceUpdate()
                            );

                    otaDelayTimer.setRepeats(false);

                    otaDelayTimer.start();
                }
        );
    }


    // =========================================================
    // TITLE BAR
    // =========================================================

    private JPanel createTitleBar() {

        JPanel bar =
                new JPanel(
                        new BorderLayout()
                );


        bar.setPreferredSize(
                new Dimension(
                        WINDOW_WIDTH,
                        36
                )
        );


        bar.setBackground(
                Color.WHITE
        );


        bar.setBorder(
                BorderFactory.createMatteBorder(
                        0,
                        0,
                        1,
                        0,
                        new Color(
                                241,
                                245,
                                249
                        )
                )
        );


        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                6
                        )
                );


        left.setOpaque(false);


        JPanel logoBadge =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();

                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );

                        g2.setColor(
                                NAVY_BANNER
                        );

                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                5,
                                5
                        );


                        g2.setColor(
                                Color.WHITE
                        );


                        g2.setFont(
                                new Font(
                                        FONT_FAMILY,
                                        Font.BOLD,
                                        10
                                )
                        );


                        FontMetrics fm =
                                g2.getFontMetrics();


                        String text =
                                "DLK";


                        int x =
                                (
                                        getWidth()
                                                - fm.stringWidth(
                                                text
                                        )
                                )
                                        / 2;


                        int y =
                                (
                                        (
                                                getHeight()
                                                        - fm.getHeight()
                                        )
                                                / 2
                                )
                                        + fm.getAscent();


                        g2.drawString(
                                text,
                                x,
                                y
                        );


                        g2.dispose();
                    }
                };


        logoBadge.setPreferredSize(
                new Dimension(
                        28,
                        20
                )
        );


        logoBadge.setOpaque(false);


        left.add(
                logoBadge
        );


        JLabel title =
                new JLabel(
                        "Employee Monitoring Agent"
                );


        title.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        title.setForeground(
                TEXT_DARK
        );


        left.add(
                title
        );


        bar.add(
                left,
                BorderLayout.WEST
        );


        JPanel controls =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );


        controls.setOpaque(false);


        // Only Minimize is available in the custom title bar.
        // Maximize and Close are intentionally removed.
        JButton btnMinimize =
                createTitleButton(
                        "Ã¢â‚¬â€",
                        false
                );


        btnMinimize.setToolTipText(
                "Minimize"
        );


        btnMinimize.addActionListener(
                e ->
                        setState(
                                JFrame.ICONIFIED
                        )
        );


        controls.add(
                btnMinimize
        );


        bar.add(
                controls,
                BorderLayout.EAST
        );


        MouseAdapter dragAdapter =
                new MouseAdapter() {

                    private Point start;


                    @Override
                    public void mousePressed(
                            MouseEvent e
                    ) {

                        start =
                                e.getPoint();
                    }


                    @Override
                    public void mouseDragged(
                            MouseEvent e
                    ) {

                        Point current =
                                e.getLocationOnScreen();


                        setLocation(
                                current.x - start.x,
                                current.y - start.y
                        );
                    }
                };


        bar.addMouseListener(
                dragAdapter
        );


        bar.addMouseMotionListener(
                dragAdapter
        );


        return bar;
    }


    private JButton createTitleButton(
            String symbol,
            boolean isClose
    ) {

        JButton btn =
                new JButton(
                        symbol
                );


        btn.setPreferredSize(
                new Dimension(
                        42,
                        36
                )
        );


        btn.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        btn.setForeground(
                TEXT_MUTED
        );


        btn.setBackground(
                Color.WHITE
        );


        btn.setBorderPainted(false);

        btn.setFocusPainted(false);

        btn.setOpaque(true);

        btn.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        btn.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            MouseEvent e
                    ) {

                        btn.setBackground(
                                isClose
                                        ? new Color(
                                        239,
                                        68,
                                        68
                                )
                                        : new Color(
                                        241,
                                        245,
                                        249
                                )
                        );


                        btn.setForeground(
                                isClose
                                        ? Color.WHITE
                                        : TEXT_DARK
                        );
                    }


                    @Override
                    public void mouseExited(
                            MouseEvent e
                    ) {

                        btn.setBackground(
                                Color.WHITE
                        );


                        btn.setForeground(
                                TEXT_MUTED
                        );
                    }
                }
        );


        return btn;
    }


    // =========================================================
    // MAIN CONTENT
    // =========================================================

    private JPanel createMainContent() {

        JPanel container =
                new JPanel(
                        new BorderLayout()
                );


        container.setBackground(
                BG_CANVAS
        );


        container.add(
                createNavyBanner(),
                BorderLayout.NORTH
        );


        JPanel body =
                new JPanel();


        body.setLayout(
                new BoxLayout(
                        body,
                        BoxLayout.Y_AXIS
                )
        );


        body.setOpaque(false);


        body.setBorder(
                new EmptyBorder(
                        12,
                        16,
                        10,
                        16
                )
        );


        body.add(
                createProfileHeaderCard()
        );

        body.add(
                Box.createVerticalStrut(
                        10
                )
        );

        body.add(
                createUpdatePanel()
        );

        body.add(
                Box.createVerticalStrut(
                        10
                )
        );

        body.add(
                createWorkSelectorCard()
        );


        body.add(
                Box.createVerticalStrut(
                        10
                )
        );


        body.add(
                createMiddleSection()
        );


        body.add(
                Box.createVerticalStrut(
                        10
                )
        );


        body.add(
                createBottomActionButtons()
        );


        body.add(
                Box.createVerticalStrut(
                        8
                )
        );


        body.add(
                createFooterBar()
        );


        container.add(
                body,
                BorderLayout.CENTER
        );


        return container;
    }

    // =========================================================
    // OTA UPDATE PANEL
    // =========================================================

    private JPanel createUpdatePanel() {

        updatePanel =
                new JPanel(
                        new BorderLayout(
                                12,
                                0
                        )
                );

        updatePanel.setBackground(
                new Color(
                        239,
                        246,
                        255
                )
        );

        updatePanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(
                                        191,
                                        219,
                                        254
                                )
                        ),
                        new EmptyBorder(
                                7,
                                12,
                                7,
                                12
                        )
                )
        );

        // =====================================================
        // LEFT CONTENT
        // =====================================================

        JPanel textPanel =
                new JPanel();

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        textPanel.setOpaque(false);

        JLabel titleLabel =
                new JLabel(
                        "\uD83D\uDD04  Update Available"
                );

        titleLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        14
                )
        );

        titleLabel.setForeground(TEXT_DARK);

        updateMessageLabel =
                new JLabel(
                        "A new version of Employee Monitoring Agent is available."
                );

        updateMessageLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );

        updateMessageLabel.setForeground(TEXT_BODY);

        updateVersionLabel =
                new JLabel(
                        "v" + org.example.AgentVersion.VERSION + "  \u2192  New Version"
                );

        updateVersionLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );

        updateVersionLabel.setForeground(PRIMARY_BLUE);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(updateMessageLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(updateVersionLabel);

        // =====================================================
        // UPDATE BUTTON
        // =====================================================

        updateNowButton =
                new JButton(
                        "Update Now"
                );

        updateNowButton.setPreferredSize(
                new Dimension(
                        120,
                        36
                )
        );

        updateNowButton.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );

        updateNowButton.setForeground(
                Color.WHITE
        );

        updateNowButton.setBackground(
                PRIMARY_BLUE
        );

        updateNowButton.setFocusPainted(false);

        updateNowButton.setBorderPainted(false);

        updateNowButton.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );

        updateNowButton.addActionListener(
                e -> startOtaInstallation()
        );

        updatePanel.add(
                textPanel,
                BorderLayout.CENTER
        );

        updatePanel.add(
                updateNowButton,
                BorderLayout.EAST
        );

        // =====================================================
        // HIDDEN INITIALLY
        // =====================================================

        updatePanel.setVisible(false);

        return updatePanel;
    }

    // =========================================================
    // CHECK OTA AFTER LOGIN
    // =========================================================

    private void checkForWorkspaceUpdate() {

        System.out.println(
                "OTA: Checking for update from Employee Workspace..."
        );

        AgentApplication.checkForOtaUpdate(
                serverUrl,
                this::showUpdateAvailable
        );
    }

    // =========================================================
    // SHOW UPDATE AVAILABLE
    // =========================================================

    private void showUpdateAvailable(
            org.example.ota.UpdateMetadata update
    ) {

        if (update == null) {
            return;
        }

        updateAvailable = true;

        if (updatePanel != null) {

            updateVersionLabel.setText(
                    "v" + org.example.AgentVersion.VERSION
                            + "  \u2192  v"
                            + update.getVersion()
            );

            updateMessageLabel.setText(
                    update.getReleaseNotes() != null
                            && !update.getReleaseNotes().isBlank()
                            ? update.getReleaseNotes()
                            : "A new version of Employee Monitoring Agent is available."
            );

            updateNowButton.setEnabled(true);

            updateNowButton.setText(
                    "Update Now"
            );

            updatePanel.setVisible(true);

            revalidate();

            repaint();
        }

        System.out.println(
                "OTA: Workspace update notification shown."
        );
    }

    // =========================================================
    // START OTA INSTALLATION
    // =========================================================

    private void startOtaInstallation() {

        if (!updateAvailable) {

            return;
        }

        if (updateInstalling) {

            return;
        }

        updateInstalling = true;

        updateNowButton.setEnabled(false);

        updateNowButton.setText(
                "Updating..."
        );

        updateMessageLabel.setText(
                "Downloading and verifying the update. Please wait..."
        );

        System.out.println(
                "OTA: Employee clicked Update Now."
        );

        // We need the latest update metadata again.
        AgentApplication.checkForOtaUpdate(
                serverUrl,
                update -> {

                    if (update == null) {

                        updateInstalling = false;

                        updateNowButton.setEnabled(true);

                        updateNowButton.setText(
                                "Update Now"
                        );

                        updateMessageLabel.setText(
                                "No update is currently available."
                        );

                        return;
                    }

                    AgentApplication.installOtaUpdate(
                            update
                    );
                }
        );
    }


    // =========================================================
    // TOP NAV BANNER
    // =========================================================

    private JPanel createNavyBanner() {

        JPanel banner =
                new JPanel(
                        new BorderLayout()
                );


        banner.setBackground(
                NAVY_BANNER
        );


        banner.setPreferredSize(
                new Dimension(
                        WINDOW_WIDTH,
                        56
                )
        );


        banner.setBorder(
                new EmptyBorder(
                        6,
                        20,
                        6,
                        20
                )
        );


        JPanel brand =
                new JPanel();


        brand.setLayout(
                new BorderLayout()
        );


        brand.setOpaque(false);


        JLabel emsTitle =
                new JLabel(
                        "DLK"
                );


        emsTitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        20
                )
        );


        emsTitle.setForeground(
                Color.WHITE
        );


        JLabel emsSubtitle =
                new JLabel(
                        ""
                );


        emsSubtitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        emsSubtitle.setForeground(
                new Color(
                        219,
                        234,
                        254
                )
        );


        emsTitle.setVerticalAlignment(SwingConstants.CENTER);
        brand.add(
                emsTitle,
                BorderLayout.CENTER
        );


        banner.add(
                brand,
                BorderLayout.WEST
        );


        JPanel menu =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                12,
                                4
                        )
                );


        menu.setOpaque(false);


        JButton myWorkspaceBtn =
                new JButton(
                        "  My Work Space"
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                Color.WHITE
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.setColor(
                                PRIMARY_BLUE
                        );


                        int cx =
                                18;


                        int cy =
                                getHeight() / 2;


                        int[] xPoints = {
                                cx,
                                cx - 6,
                                cx + 6
                        };


                        int[] yPoints = {
                                cy - 6,
                                cy,
                                cy
                        };


                        g2.fillPolygon(
                                xPoints,
                                yPoints,
                                3
                        );


                        g2.fillRect(
                                cx - 4,
                                cy,
                                8,
                                6
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        myWorkspaceBtn.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        myWorkspaceBtn.setForeground(
                PRIMARY_BLUE
        );


        myWorkspaceBtn.setPreferredSize(
                new Dimension(
                        145,
                        34
                )
        );


        myWorkspaceBtn.setContentAreaFilled(
                false
        );


        myWorkspaceBtn.setBorderPainted(
                false
        );


        myWorkspaceBtn.setFocusPainted(
                false
        );


        myWorkspaceBtn.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );





        JButton logoutButton =
                createNavMenuLinkWithIcon(
                        "Logout",
                        IconType.LOGOUT
                );


        logoutButton.addActionListener(
                e ->
                        handleLogout()
        );


        menu.add(
                logoutButton
        );


        banner.add(
                menu,
                BorderLayout.EAST
        );


        return banner;
    }


    private JButton createNavMenuLink(
            String text
    ) {

        JButton btn =
                new JButton(
                        text
                );


        btn.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        btn.setForeground(
                Color.WHITE
        );


        btn.setContentAreaFilled(
                false
        );


        btn.setBorderPainted(
                false
        );


        btn.setFocusPainted(
                false
        );


        btn.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        // Logout has its own real action handler. Other navigation
        // items keep the existing placeholder behaviour.
        if (!text.contains("Logout")) {

            btn.addActionListener(
                    e ->
                            showInformation(
                                    text,
                                    text.replace(
                                            " ",
                                            ""
                                    )
                                            + " will be available soon."
                            )
            );
        }


        return btn;
    }


    private JButton createNavMenuLinkWithIcon(
            String text,
            IconType iconType
    ) {

        JButton btn =
                createNavMenuLink(text);

        btn.setIcon(
                createWorkspaceIcon(
                        iconType,
                        Color.WHITE
                )
        );

        btn.setIconTextGap(8);

        return btn;
    }


    // =========================================================
    // PROFILE HEADER
    // =========================================================

    private JPanel createProfileHeaderCard() {

        JPanel card =
                createWhiteCard();


        card.setLayout(
                new BorderLayout(
                        15,
                        0
                )
        );


        card.setBorder(
                new EmptyBorder(
                        12,
                        18,
                        12,
                        18
                )
        );


        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        74
                )
        );


        JPanel left =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                14,
                                0
                        )
                );


        left.setOpaque(false);


        JPanel avatar =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                new Color(
                                        219,
                                        234,
                                        254
                                )
                        );


                        g2.fillOval(
                                0,
                                0,
                                getWidth(),
                                getHeight()
                        );


                        g2.setColor(
                                new Color(
                                        37,
                                        99,
                                        235
                                )
                        );


                        int cx =
                                getWidth() / 2;


                        int cy =
                                getHeight() / 2;


                        g2.fillOval(
                                cx - 7,
                                cy - 11,
                                14,
                                14
                        );


                        g2.fillArc(
                                cx - 13,
                                cy + 3,
                                26,
                                20,
                                0,
                                180
                        );


                        g2.dispose();
                    }
                };


        avatar.setPreferredSize(
                new Dimension(
                        46,
                        46
                )
        );


        avatar.setOpaque(false);


        left.add(
                avatar
        );


        JPanel info =
                new JPanel();


        info.setLayout(
                new BoxLayout(
                        info,
                        BoxLayout.Y_AXIS
                )
        );


        info.setOpaque(false);


        welcomeLabel =
                new JLabel(
                        "<html>Welcome, <b>"
                                + employeeDisplayName
                                + "</b></html>"
                );


        welcomeLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        15
                )
        );


        welcomeLabel.setForeground(
                TEXT_DARK
        );


        empCodeLabel =
                new JLabel(
                        "Employee Code: "
                                + employeeCode
                );


        empCodeLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        empCodeLabel.setForeground(
                TEXT_MUTED
        );


        JLabel subtext =
                new JLabel(
                        "Stay productive and keep moving forward!"
                );


        subtext.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        subtext.setForeground(
                new Color(
                        148,
                        163,
                        184
                )
        );


        info.add(
                welcomeLabel
        );


        info.add(
                empCodeLabel
        );


        info.add(
                subtext
        );


        left.add(
                info
        );


        card.add(
                left,
                BorderLayout.WEST
        );


        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                14,
                                4
                        )
                );


        right.setOpaque(false);


        JPanel dateTimePanel =
                new JPanel();


        dateTimePanel.setLayout(
                new BoxLayout(
                        dateTimePanel,
                        BoxLayout.Y_AXIS
                )
        );


        dateTimePanel.setOpaque(false);


        String curDate =
                LocalDate.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "EEEE, dd MMM yyyy"
                                )
                        );


        dateLabel =
                new JLabel(
                        curDate,
                        createWorkspaceIcon(
                                IconType.CALENDAR,
                                TEXT_MUTED
                        ),
                        JLabel.LEFT
                );


        dateLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        dateLabel.setForeground(
                TEXT_BODY
        );


        timeHeaderLabel =
                new JLabel(
                        formatTime(
                                LocalTime.now()
                        )
                );


        timeHeaderLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        timeHeaderLabel.setForeground(
                TEXT_MUTED
        );


        dateTimePanel.add(
                dateLabel
        );


        dateTimePanel.add(
                timeHeaderLabel
        );


        right.add(
                dateTimePanel
        );


        onlinePill =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                6,
                                4
                        )
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );

                        boolean isOffline = AgentApplication.isOfflineMode();


                        g2.setColor(
                                isOffline ? new Color(254, 226, 226) : MINI_GREEN_BG
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                16,
                                16
                        );


                        g2.setColor(
                                isOffline ? new Color(252, 165, 165) : MINI_GREEN_BORDER
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                16,
                                16
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        onlinePill.setOpaque(false);


        onlinePill.setPreferredSize(
                new Dimension(
                        84,
                        28
                )
        );


        boolean isOffline = org.example.AgentApplication.isOfflineMode();
        java.awt.Color statusColor = isOffline ? DANGER_RED : SUCCESS_GREEN;
        String statusText = isOffline ? "Offline" : "Online";

        onlineDot =
                createDot(
                        statusColor,
                        7
                );


        onlinePill.add(
                onlineDot
        );


        onlineBadgeLabel =
                new JLabel(
                        statusText
                );


        onlineBadgeLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        onlineBadgeLabel.setForeground(
                statusColor
        );


        onlinePill.add(
                onlineBadgeLabel
        );


        right.add(
                onlinePill
        );


        card.add(
                right,
                BorderLayout.EAST
        );


        return card;
    }


    // =========================================================
    // WORK SELECTOR
    // =========================================================

    private JPanel createWorkSelectorCard() {

        JPanel card =
                createWhiteCard();


        card.setLayout(
                new GridBagLayout()
        );


        card.setBorder(
                new EmptyBorder(
                        12,
                        16,
                        12,
                        16
                )
        );


        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        70
                )
        );


        GridBagConstraints gbc =
                new GridBagConstraints();


        gbc.gridy = 0;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;


        gbc.insets =
                new Insets(
                        0,
                        0,
                        0,
                        10
                );


        // =====================================================
        // CLIENT
        // =====================================================

        gbc.gridx = 0;

        gbc.weightx = 0.22;


        JPanel clientPanel =
                createLabeledSelector(
                        "Client",
                        IconType.TASK,
                        new String[]{
                                "Internal",
                                "External Client"
                        }
                );


        clientCombo =
                (JComboBox<String>)
                        clientPanel
                                .getClientProperty(
                                        "combo"
                                );


        card.add(
                clientPanel,
                gbc
        );


        // =====================================================
        // PROJECT
        // =====================================================

        gbc.gridx = 1;

        gbc.weightx = 0.25;


        JPanel projectPanel =
                createLabeledSelector(
                        "Project",
                        IconType.TASK,
                        new String[]{
                                "Software Team",
                                "Employee Monitoring System",
                                "Backend Development"
                        }
                );


        projectCombo =
                (JComboBox<String>)
                        projectPanel
                                .getClientProperty(
                                        "combo"
                                );


        card.add(
                projectPanel,
                gbc
        );


        // =====================================================
        // TASK
        // =====================================================

        gbc.gridx = 2;

        gbc.weightx = 0.26;


        JPanel taskPanel =
                createLabeledSelector(
                        "Task",
                        IconType.TASK,
                        new String[]{
                                "Build Employee API",
                                "API Testing",
                                "Application Monitoring",
                                "Attendance API"
                        }
                );


        taskCombo =
                (JComboBox<String>)
                        taskPanel
                                .getClientProperty(
                                        "combo"
                                );


        taskCombo.addActionListener(
                e -> {

                    if (taskCombo.getSelectedItem() == null) {
                        if (!workRunning) {
                            currentTaskId = null;
                            currentTaskTitle = null;
                        }
                        return;
                    }

                    String selectedTitle =
                            taskCombo
                                    .getSelectedItem()
                                    .toString()
                                    .trim();

                    if (taskWorkStateLoading) {
                        return;
                    }

                    String selectedTaskId =
                            taskIdByTitle.get(
                                    selectedTitle
                            );

                    // Never silently switch away from a running task.
                    // The current task must be stopped successfully first.
                    if (workRunning
                            && currentTaskTitle != null
                            && !currentTaskTitle.equals(selectedTitle)) {

                        showInformation(
                                "Task Timer",
                                "Please stop the current task timer before switching tasks."
                        );

                        SwingUtilities.invokeLater(
                                () -> taskCombo.setSelectedItem(
                                        currentTaskTitle
                                )
                        );

                        return;
                    }

                    currentTaskId = selectedTaskId;
                    currentTaskTitle = selectedTitle;

                    /*
                     * Selecting a task does NOT start work.
                     * Reset only the current-task display; the overall
                     * employee Work Time remains untouched.
                     */
                    if (!workRunning) {
                        workAccumulatedSeconds = 0L;
                        currentTaskStartedAtMillis = 0L;
                    }

                    if (currentTaskLabel != null) {
                        currentTaskLabel.setText(
                                selectedTitle
                        );
                    }

                    System.out.println(
                            "Selected Task: "
                                    + selectedTitle
                                    + " | Task ID: "
                                    + currentTaskId
                    );
                }
        );


        card.add(
                taskPanel,
                gbc
        );


        // =====================================================
        // WORK TIME
        // =====================================================

        gbc.gridx = 3;

        gbc.weightx = 0.15;

        gbc.insets =
                new Insets(
                        0,
                        6,
                        0,
                        10
                );


        JPanel timeBox =
                new JPanel(
                        new BorderLayout(
                                0,
                                2
                        )
                );


        timeBox.setOpaque(false);


        JLabel lblWork =
                new JLabel(
                        "Work Time"
                );


        lblWork.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        lblWork.setForeground(
                TEXT_DARK
        );


        JPanel timerRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                6,
                                0
                        )
                );


        timerRow.setOpaque(false);


        JLabel clockIcon =
                new JLabel(
                        createWorkspaceIcon(
                                IconType.CLOCK,
                                PRIMARY_BLUE
                        )
                );


        clockIcon.setForeground(
                PRIMARY_BLUE
        );


        timerRow.add(
                clockIcon
        );


        workTimeLabel =
                new JLabel(
                        "00:00:00"
                );


        workTimeLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        18
                )
        );


        workTimeLabel.setForeground(
                PRIMARY_BLUE
        );


        timerRow.add(
                workTimeLabel
        );


        timeBox.add(
                lblWork,
                BorderLayout.NORTH
        );


        timeBox.add(
                timerRow,
                BorderLayout.CENTER
        );


        card.add(
                timeBox,
                gbc
        );


        // =====================================================
        // STOP TIMER
        // =====================================================

        gbc.gridx = 4;

        gbc.weightx = 0.12;

        gbc.insets =
                new Insets(
                        14,
                        0,
                        0,
                        0
                );


        workTimerButton =
                new JButton(
                        "Start Timer"
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                workRunning
                                        ? (getModel().isRollover() ? DANGER_RED_HOVER : DANGER_RED)
                                        : (getModel().isRollover() ? PRIMARY_BLUE.darker() : PRIMARY_BLUE)
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        workTimerButton.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        workTimerButton.setForeground(
                Color.WHITE
        );

        workTimerButton.setIcon(
                createWorkspaceIcon(
                        IconType.START,
                        PRIMARY_BLUE
                )
        );
        workTimerButton.setIconTextGap(8);


        workTimerButton.setPreferredSize(
                new Dimension(
                        120,
                        36
                )
        );


        workTimerButton.setContentAreaFilled(
                false
        );


        workTimerButton.setBorderPainted(
                false
        );


        workTimerButton.setFocusPainted(
                false
        );


        workTimerButton.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        workTimerButton.addActionListener(
                e ->
                        toggleWorkTimer()
        );


        card.add(
                workTimerButton,
                gbc
        );


        return card;
    }


    private JPanel createLabeledSelector(
            String labelTitle,
            IconType iconType,
            String[] items
    ) {

        JPanel p =
                new JPanel(
                        new BorderLayout(
                                0,
                                4
                        )
                );


        p.setOpaque(false);


        JLabel lbl =
                new JLabel(
                        labelTitle
                );


        lbl.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        lbl.setForeground(
                TEXT_MUTED
        );


        p.add(
                lbl,
                BorderLayout.NORTH
        );


        JComboBox<String> combo =
                new JComboBox<>(
                        items
                );


        combo.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        combo.setForeground(
                TEXT_DARK
        );


        combo.setBackground(
                Color.WHITE
        );


        combo.setPreferredSize(
                new Dimension(
                        combo.getPreferredSize().width,
                        34
                )
        );


        combo.setFocusable(false);


        combo.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_INPUT,
                                1,
                                true
                        ),
                        BorderFactory.createEmptyBorder(
                                0,
                                6,
                                0,
                                4
                        )
                )
        );


        combo.setRenderer(
                new DefaultListCellRenderer() {

                    @Override
                    public Component
                    getListCellRendererComponent(
                            JList<?> list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {

                        JLabel l =
                                (JLabel)
                                        super
                                                .getListCellRendererComponent(
                                                        list,
                                                        value,
                                                        index,
                                                        isSelected,
                                                        cellHasFocus
                                                );



                        l.setBackground(Color.WHITE);

                        l.setOpaque(true);

                        l.setForeground(TEXT_DARK);


                        JLabel iconLabel =
                                new JLabel(
                                        createWorkspaceIcon(
                                                iconType,
                                                TEXT_MUTED
                                        )
                                );

                        l.setIcon(
                                createWorkspaceIcon(
                                        iconType,
                                        TEXT_MUTED
                                )
                        );
                        l.setText(
                                value != null
                                        ? value.toString()
                                        : ""
                        );
                        l.setIconTextGap(6);


                        l.setBorder(
                                new EmptyBorder(
                                        4,
                                        6,
                                        4,
                                        6
                                )
                        );


                        return l;
                    }
                }
        );


        combo.setUI(
                new BasicComboBoxUI() {

                    @Override
                    public void paintCurrentValueBackground(
                            Graphics g,
                            Rectangle bounds,
                            boolean hasFocus
                    ) {
                        Graphics2D g2 =
                                (Graphics2D) g.create();

                        g2.setColor(Color.WHITE);

                        g2.fillRect(
                                bounds.x,
                                bounds.y,
                                bounds.width,
                                bounds.height
                        );

                        g2.dispose();
                    }

                    @Override
                    protected JButton createArrowButton() {

                        JButton btn =
                                new JButton() {

                                    @Override
                                    protected void paintComponent(
                                            Graphics g
                                    ) {

                                        Graphics2D g2 =
                                                (Graphics2D) g.create();

                                        g2.setRenderingHint(
                                                RenderingHints.KEY_ANTIALIASING,
                                                RenderingHints.VALUE_ANTIALIAS_ON
                                        );

                                        g2.setColor(TEXT_MUTED);

                                        int cx =
                                                getWidth() / 2;

                                        int cy =
                                                getHeight() / 2;

                                        g2.drawLine(
                                                cx - 3,
                                                cy - 2,
                                                cx,
                                                cy + 1
                                        );

                                        g2.drawLine(
                                                cx,
                                                cy + 1,
                                                cx + 3,
                                                cy - 2
                                        );

                                        g2.dispose();
                                    }
                                };

                        btn.setOpaque(true);
                        btn.setBackground(Color.WHITE);
                        btn.setForeground(TEXT_MUTED);
                        btn.setContentAreaFilled(false);
                        btn.setBorderPainted(false);
                        btn.setFocusPainted(false);

                        btn.setPreferredSize(
                                new Dimension(
                                        20,
                                        20
                                )
                        );

                        return btn;
                    }
                }
        );


        p.add(
                combo,
                BorderLayout.CENTER
        );


        p.putClientProperty(
                "combo",
                combo
        );


        return p;
    }


    // =========================================================
    // MIDDLE SECTION
    // =========================================================

    private JPanel createMiddleSection() {

        JPanel section =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                12,
                                0
                        )
                );


        section.setOpaque(false);


        section.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        175
                )
        );


        section.add(
                createCurrentStatusPanel()
        );


        section.add(
                createTodayAttendancePanel()
        );


        return section;
    }


    // =========================================================
    // CURRENT STATUS
    // =========================================================

    private JPanel createCurrentStatusPanel() {

        JPanel card =
                createWhiteCard();


        card.setLayout(
                new BorderLayout(
                        0,
                        8
                )
        );


        card.setBorder(
                new EmptyBorder(
                        12,
                        14,
                        12,
                        14
                )
        );


        JPanel header =
                new JPanel(
                        new BorderLayout()
                );


        header.setOpaque(false);


        JPanel leftHeader =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                6,
                                0
                        )
                );


        leftHeader.setOpaque(false);


        JLabel lblTitle =
                new JLabel(
                        "Current Status  : "
                );


        lblTitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        13
                )
        );


        lblTitle.setForeground(
                TEXT_DARK
        );


        leftHeader.add(
                lblTitle
        );


        statusDotPanel =
                createDot(
                        SUCCESS_GREEN,
                        9
                );


        leftHeader.add(
                statusDotPanel
        );


        currentStatusBadge =
                new JLabel(
                        "WORKING"
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                getBackground()
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                6,
                                6
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        currentStatusBadge.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        11
                )
        );


        currentStatusBadge.setForeground(
                SUCCESS_GREEN
        );


        currentStatusBadge.setBackground(
                MINI_GREEN_BG
        );


        currentStatusBadge.setBorder(
                new EmptyBorder(
                        2,
                        8,
                        2,
                        8
                )
        );


        currentStatusBadge.setOpaque(false);


        leftHeader.add(
                currentStatusBadge
        );


        header.add(
                leftHeader,
                BorderLayout.WEST
        );


        idleTimerHeaderLabel =
                new JLabel(
                        "Idle for: 00:00:00",
                        createWorkspaceIcon(
                                IconType.CLOCK,
                                PRIMARY_BLUE
                        ),
                        JLabel.LEFT
                );


        idleTimerHeaderLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        12
                )
        );


        idleTimerHeaderLabel.setForeground(
                TEXT_MUTED
        );


        header.add(
                idleTimerHeaderLabel,
                BorderLayout.EAST
        );


        card.add(
                header,
                BorderLayout.NORTH
        );


        JPanel detailBox =
                new JPanel(
                        new GridLayout(
                                4,
                                1,
                                0,
                                5
                        )
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                new Color(
                                        255,
                                        253,
                                        247
                                )
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.setColor(
                                new Color(
                                        254,
                                        240,
                                        138
                                )
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                8,
                                8
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        detailBox.setOpaque(false);


        detailBox.setBorder(
                new EmptyBorder(
                        8,
                        12,
                        8,
                        12
                )
        );


        currentTaskLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        detailBox.add(
                createDetailLine(
                        IconType.TASK,
                        "Current Task",
                        " :   ",
                        currentTaskLabel,
                        TEXT_BODY,
                        false
                )
        );


        startedAtLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        detailBox.add(
                createDetailLine(
                        IconType.CLOCK,
                        "Started At",
                        " :   ",
                        startedAtLabel,
                        TEXT_BODY,
                        false
                )
        );


        durationLabel =
                new JLabel(
                        "00:00:00"
                );


        detailBox.add(
                createDetailLine(
                        IconType.DURATION,
                        "Duration",
                        " :   ",
                        durationLabel,
                        TEXT_BODY,
                        false
                )
        );


        idleTimeLabel =
                new JLabel(
                        "00:00:00"
                );


        detailBox.add(
                createDetailLine(
                        IconType.IDLE,
                        "Idle Time",
                        " :   ",
                        idleTimeLabel,
                        WARNING_ORANGE,
                        true
                )
        );


        card.add(
                detailBox,
                BorderLayout.CENTER
        );


        return card;
    }


    private JPanel createDetailLine(
            IconType iconType,
            String title,
            String sep,
            JLabel valueLabel,
            Color valColor,
            boolean isBold
    ) {

        JPanel p =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );


        p.setOpaque(false);


        JLabel lblTitle =
                new JLabel(
                        title,
                        createWorkspaceIcon(
                                iconType,
                                TEXT_DARK
                        ),
                        JLabel.LEFT
                );


        lblTitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        lblTitle.setForeground(
                TEXT_DARK
        );


        lblTitle.setPreferredSize(
                new Dimension(
                        110,
                        18
                )
        );


        JLabel lblSep =
                new JLabel(
                        sep
                );


        lblSep.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        12
                )
        );


        lblSep.setForeground(
                TEXT_DARK
        );


        valueLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        isBold
                                ? Font.BOLD
                                : Font.PLAIN,
                        12
                )
        );


        valueLabel.setForeground(
                valColor
        );


        p.add(
                lblTitle
        );


        p.add(
                lblSep
        );


        p.add(
                valueLabel
        );


        return p;
    }


    // =========================================================
    // TODAY'S ATTENDANCE
    // =========================================================

    private JPanel createTodayAttendancePanel() {

        JPanel card =
                createWhiteCard();


        card.setLayout(
                new BorderLayout(
                        0,
                        10
                )
        );


        card.setBorder(
                new EmptyBorder(
                        12,
                        14,
                        12,
                        14
                )
        );


        JLabel headerTitle =
                new JLabel(
                        "Today's Attendance",
                        createWorkspaceIcon(
                                IconType.CALENDAR,
                                TEXT_DARK
                        ),
                        JLabel.LEFT
                );


        headerTitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        13
                )
        );


        headerTitle.setForeground(
                TEXT_DARK
        );


        card.add(
                headerTitle,
                BorderLayout.NORTH
        );


        JPanel miniGrid =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                8,
                                0
                        )
                );


        miniGrid.setOpaque(false);


        clockInMiniLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        miniGrid.add(
                createMiniCard(
                        IconType.START,
                        MINI_GREEN_BG,
                        MINI_GREEN_BORDER,
                        SUCCESS_GREEN,
                        "Clock In",
                        clockInMiniLabel
                )
        );


        workHoursMiniLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        miniGrid.add(
                createMiniCard(
                        IconType.CLOCK,
                        MINI_BLUE_BG,
                        MINI_BLUE_BORDER,
                        PRIMARY_BLUE,
                        "Work Hours",
                        workHoursMiniLabel
                )
        );


        breakTimeMiniLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        miniGrid.add(
                createMiniCard(
                        IconType.BREAK,
                        MINI_ORANGE_BG,
                        MINI_ORANGE_BORDER,
                        WARNING_ORANGE,
                        "Break Time",
                        breakTimeMiniLabel
                )
        );


        lunchTimeMiniLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        miniGrid.add(
                createMiniCard(
                        IconType.LUNCH,
                        MINI_PURPLE_BG,
                        MINI_PURPLE_BORDER,
                        PURPLE_TEXT,
                        "Lunch Time",
                        lunchTimeMiniLabel
                )
        );


        idleTimeMiniLabel =
                new JLabel(
                        "Ã¢â‚¬â€"
                );


        miniGrid.add(
                createMiniCard(
                        IconType.IDLE,
                        MINI_ROSE_BG,
                        MINI_ROSE_BORDER,
                        DANGER_RED,
                        "Idle Time",
                        idleTimeMiniLabel
                )
        );


        card.add(
                miniGrid,
                BorderLayout.CENTER
        );


        return card;
    }


    private JPanel createMiniCard(
            IconType iconType,
            Color bg,
            Color border,
            Color iconColor,
            String title,
            JLabel valLabel
    ) {

        JPanel p =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                bg
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.setColor(
                                border
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                8,
                                8
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        p.setOpaque(false);


        p.setLayout(
                new BoxLayout(
                        p,
                        BoxLayout.Y_AXIS
                )
        );


        p.setBorder(
                new EmptyBorder(
                        8,
                        6,
                        8,
                        6
                )
        );


        JLabel icon =
                new JLabel(
                        createWorkspaceIcon(
                                iconType,
                                iconColor
                        )
                );


        icon.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        14
                )
        );


        icon.setForeground(
                iconColor
        );


        icon.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        JLabel lblTitle =
                new JLabel(
                        title
                );


        lblTitle.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        lblTitle.setForeground(
                TEXT_MUTED
        );


        lblTitle.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        valLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        13
                )
        );


        valLabel.setForeground(
                TEXT_DARK
        );


        valLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );


        p.add(
                icon
        );


        p.add(
                Box.createVerticalStrut(
                        4
                )
        );


        p.add(
                lblTitle
        );


        p.add(
                Box.createVerticalStrut(
                        4
                )
        );


        p.add(
                valLabel
        );


        return p;
    }


    // =========================================================
    // BOTTOM ACTION BUTTONS
    // =========================================================

    private JPanel createBottomActionButtons() {

        JPanel row =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                12,
                                0
                        )
                );


        row.setOpaque(false);


        Dimension buttonSize =
                new Dimension(
                        Integer.MAX_VALUE,
                        56
                );

        row.setPreferredSize(buttonSize);
        row.setMinimumSize(buttonSize);
        row.setMaximumSize(buttonSize);


        breakButton =
                new JButton(
                        "Start Break          "
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                BREAK_BG
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.setColor(
                                BREAK_BORDER
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                8,
                                8
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        breakButton.setPreferredSize(
                new Dimension(
                        0,
                        56
                )
        );

        breakButton.setMinimumSize(
                new Dimension(
                        0,
                        56
                )
        );

        breakButton.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        14
                )
        );


        breakButton.setForeground(
                PRIMARY_BLUE
        );

        breakButton.setIcon(
                createWorkspaceIcon(
                        IconType.BREAK,
                        WARNING_ORANGE
                )
        );

        breakButton.setHorizontalAlignment(SwingConstants.CENTER);
        breakButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        breakButton.setIconTextGap(8);

        breakButton.setContentAreaFilled(
                false
        );


        breakButton.setBorderPainted(
                false
        );


        breakButton.setFocusPainted(
                false
        );


        breakButton.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        breakButton.addActionListener(
                e ->
                        toggleBreak()
        );


        lunchButton =
                new JButton(
                        "Start Lunch                 "
                ) {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                LUNCH_BG
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                8,
                                8
                        );


                        g2.setColor(
                                LUNCH_BORDER
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                8,
                                8
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        lunchButton.setPreferredSize(
                new Dimension(
                        0,
                        56
                )
        );

        lunchButton.setMinimumSize(
                new Dimension(
                        0,
                        56
                )
        );

        lunchButton.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.BOLD,
                        14
                )
        );


        lunchButton.setForeground(
                PURPLE_TEXT
        );

        lunchButton.setIcon(
                createWorkspaceIcon(
                        IconType.LUNCH,
                        PURPLE_TEXT
                )
        );

        breakButton.setHorizontalAlignment(SwingConstants.CENTER);
        breakButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        breakButton.setIconTextGap(8);
        lunchButton.setContentAreaFilled(
                false
        );


        lunchButton.setBorderPainted(
                false
        );


        lunchButton.setFocusPainted(
                false
        );


        lunchButton.setCursor(
                new Cursor(
                        Cursor.HAND_CURSOR
                )
        );


        lunchButton.addActionListener(
                e ->
                        toggleLunch()
        );


        row.add(
                breakButton
        );


        row.add(
                lunchButton
        );


        return row;
    }


    // =========================================================
    // FOOTER
    // =========================================================

    private JPanel createFooterBar() {

        JPanel bar =
                new JPanel(
                        new BorderLayout()
                );


        bar.setOpaque(false);


        Dimension footerSize = new Dimension(Integer.MAX_VALUE, 36);
        bar.setPreferredSize(footerSize);
        bar.setMinimumSize(footerSize);
        bar.setMaximumSize(footerSize);


        JLabel infoText =
                new JLabel(
                        "Status changes to IDLE automatically when there is no activity for a certain period.",
                        createWorkspaceIcon(
                                IconType.IDLE,
                                TEXT_MUTED
                        ),
                        JLabel.LEFT
                );


        infoText.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        infoText.setForeground(
                TEXT_MUTED
        );


        bar.add(
                infoText,
                BorderLayout.WEST
        );


        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                6,
                                0
                        )
                );


        right.setOpaque(false);

        JLabel versionLabel = new JLabel("v" + org.example.AgentVersion.VERSION);
        versionLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        versionLabel.setForeground(PRIMARY_BLUE);
        right.add(versionLabel);

        JLabel divider = new JLabel("  |  ");
        divider.setForeground(TEXT_MUTED);
        right.add(divider);

        right.add(
                createDot(
                        SUCCESS_GREEN,
                        7
                )
        );


        lastActivityLabel =
                new JLabel(
                        "Last Activity : "
                                + formatTimeWithSeconds(
                                LocalTime.now()
                        ),
                        createWorkspaceIcon(
                                IconType.CLOCK,
                                TEXT_MUTED
                        ),
                        JLabel.LEFT
                );


        lastActivityLabel.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );


        lastActivityLabel.setForeground(
                TEXT_MUTED
        );


        right.add(
                lastActivityLabel
        );


        bar.add(
                right,
                BorderLayout.EAST
        );


        return bar;
    }


    // =========================================================
    // WHITE CARD
    // =========================================================

    private JPanel createWhiteCard() {

        JPanel card =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        g2.setColor(
                                BG_CARD
                        );


                        g2.fillRoundRect(
                                0,
                                0,
                                getWidth(),
                                getHeight(),
                                10,
                                10
                        );


                        g2.setColor(
                                BORDER_CARD
                        );


                        g2.drawRoundRect(
                                0,
                                0,
                                getWidth() - 1,
                                getHeight() - 1,
                                10,
                                10
                        );


                        g2.dispose();


                        super.paintComponent(g);
                    }
                };


        card.setOpaque(false);


        return card;
    }


    // =========================================================
    // DOT
    // =========================================================

    private JPanel createDot(
            Color color,
            int size
    ) {

        JPanel dot =
                new JPanel() {

                    @Override
                    protected void paintComponent(
                            Graphics g
                    ) {

                        Graphics2D g2 =
                                (Graphics2D)
                                        g.create();


                        g2.setRenderingHint(
                                RenderingHints
                                        .KEY_ANTIALIASING,
                                RenderingHints
                                        .VALUE_ANTIALIAS_ON
                        );


                        Color paintColor =
                                color;


                        Object property =
                                getClientProperty(
                                        "dotColor"
                                );


                        if (property instanceof Color) {

                            paintColor =
                                    (Color) property;
                        }


                        g2.setColor(
                                paintColor
                        );


                        g2.fillOval(
                                0,
                                2,
                                size,
                                size
                        );


                        g2.dispose();
                    }
                };


        dot.putClientProperty(
                "dotColor",
                color
        );


        dot.setPreferredSize(
                new Dimension(
                        size,
                        size + 4
                )
        );


        dot.setOpaque(false);


        return dot;
    }


    // =========================================================
    // UI TIMER
    // =========================================================

    private void startUiTimer() {

        if (uiTimer != null) {

            uiTimer.stop();
        }


        uiTimer =
                new Timer(
                        1000,
                        e ->
                                updateLocalUi()
                );


        uiTimer.setRepeats(true);


        uiTimer.setInitialDelay(
                1000
        );


        uiTimer.start();
    }


    // =========================================================
    // UPDATE LOCAL UI
    // =========================================================

    private void updateLocalUi() {

        long nowNanos = System.nanoTime();

        long inactiveSeconds = 0L;

        try {

            inactiveSeconds = Math.max(
                    0L,
                    activityMonitor.getInactiveDurationSeconds()
            );

        } catch (Exception ex) {

            System.out.println(
                    "Workspace activity check failed: "
                            + ex.getMessage()
            );
        }

        // =====================================================
        // STATE PRIORITY
        //
        // 1. BREAK
        // 2. LUNCH
        // 3. WORKING
        // 4. IDLE
        // =====================================================

        if (breakRunning) {

            if (!"BREAK".equals(localStatus)) {
                updateStatus("BREAK");
            }
        }

        else if (lunchRunning) {

            if (!"LUNCH".equals(localStatus)) {
                updateStatus("LUNCH");
            }
        }

        else if ("WORKING".equals(localStatus)) {

            /*
             * Employee Work Time is based on the employee
             * WORKING state, not on whether Task Timer is running.
             *
             * When inactivity reaches the configured threshold:
             *
             * WORKING -> IDLE
             *
             * Employee Work Time stops.
             */

            if ("WORKING".equals(localStatus)
                    && inactiveSeconds >= idleThresholdSeconds) {

                closeEmployeeWorkSegment();

                /*
                 * Task Timer is only active online.
                 */
                if (!AgentApplication.isOfflineMode()
                        && workSegmentStartNanos > 0L) {

                    long currentTaskWorkSeconds =
                            Math.max(
                                    0L,
                                    (nowNanos
                                            - workSegmentStartNanos)
                                            / 1_000_000_000L
                            );

                    workAccumulatedSeconds +=
                            currentTaskWorkSeconds;
                }

                /*
                 * Stop Task Timer while IDLE.
                 */
                workSegmentStartNanos = 0L;

                /*
                 * Start Idle segment.
                 */
                idleSegmentStartNanos = nowNanos;

                idleAfterBreakLunch = false;

                updateStatus("IDLE");

                System.out.println(
                        "Workspace state changed: "
                                + "WORKING -> IDLE"
                                + " after "
                                + inactiveSeconds
                                + " seconds inactivity."
                );
            }
        }

        else if ("IDLE".equals(localStatus)) {

            /*
             * After Break/Lunch, employee must explicitly
             * start the timer again.
             */
            if (idleAfterBreakLunch) {

                // Stay IDLE until the user explicitly
                // starts work after Break/Lunch.

            }

            else if (inactiveSeconds < idleThresholdSeconds
                    && "IDLE".equals(localStatus)) {

                /*
                 * Close the current idle segment.
                 */
                if (idleSegmentStartNanos > 0L) {

                    long idleSegmentSeconds =
                            Math.max(
                                    0L,
                                    (nowNanos
                                            - idleSegmentStartNanos)
                                            / 1_000_000_000L
                            );

                    idleAccumulatedSeconds +=
                            idleSegmentSeconds;
                }

                idleSegmentStartNanos = 0L;

                long resumeNanos =
                        System.nanoTime();

                /*
                 * Employee Work Time resumes both ONLINE
                 * and OFFLINE.
                 */
                employeeWorkSegmentStartNanos =
                        resumeNanos;

                /*
                 * Task Timer resumes ONLY ONLINE.
                 *
                 * OFFLINE mode does not have a backend task
                 * session, so Task Timer remains disabled.
                 */
                if (!AgentApplication.isOfflineMode()) {

                    workSegmentStartNanos =
                            resumeNanos;
                }

                updateStatus("WORKING");

                System.out.println(
                        "Workspace state changed: "
                                + "IDLE -> WORKING"
                );
            }
        }

        // =====================================================
        // RECALCULATE DISPLAY VALUES
        // =====================================================

        nowNanos = System.nanoTime();

        long workSeconds =
                employeeWorkAccumulatedSeconds;

        long breakSeconds =
                breakAccumulatedSeconds;

        long lunchSeconds =
                lunchAccumulatedSeconds;

        // =====================================================
        // CURRENT EMPLOYEE WORK TIME
        // =====================================================

        if ("WORKING".equals(localStatus)
                && employeeWorkSegmentStartNanos > 0L) {

            workSeconds += Math.max(
                    0L,
                    (nowNanos
                            - employeeWorkSegmentStartNanos)
                            / 1_000_000_000L
            );
        }

        // =====================================================
        // CURRENT BREAK TIME
        // =====================================================

        if (breakRunning
                && breakSegmentStartNanos > 0L) {

            breakSeconds += Math.max(
                    0L,
                    (nowNanos
                            - breakSegmentStartNanos)
                            / 1_000_000_000L
            );
        }

        // =====================================================
        // CURRENT LUNCH TIME
        // =====================================================

        if (lunchRunning
                && lunchSegmentStartNanos > 0L) {

            lunchSeconds += Math.max(
                    0L,
                    (nowNanos
                            - lunchSegmentStartNanos)
                            / 1_000_000_000L
            );
        }

        // =====================================================
        // IDLE DISPLAY
        // =====================================================

        long idleDisplay =
                idleAccumulatedSeconds;

        if ("IDLE".equals(localStatus)
                && idleSegmentStartNanos > 0L) {

            long currentIdleSegmentSeconds =
                    Math.max(
                            0L,
                            (nowNanos
                                    - idleSegmentStartNanos)
                                    / 1_000_000_000L
                    );

            idleDisplay =
                    idleAccumulatedSeconds
                            + currentIdleSegmentSeconds;
        }

        // =====================================================
        // EMPLOYEE WORK TIME
        // =====================================================

        if (workTimeLabel != null) {

            workTimeLabel.setText(
                    formatDuration(workSeconds)
            );
        }

        // =====================================================
        // CURRENT TASK DURATION
        // =====================================================

        long taskDurationSeconds =
                workAccumulatedSeconds;

        /*
         * Task Timer is available only ONLINE.
         */
        if (!AgentApplication.isOfflineMode()
                && "WORKING".equals(localStatus)
                && workSegmentStartNanos > 0L) {

            taskDurationSeconds += Math.max(
                    0L,
                    (nowNanos
                            - workSegmentStartNanos)
                            / 1_000_000_000L
            );
        }

        /*
         * Keep Task Timer at zero while OFFLINE.
         */
        if (AgentApplication.isOfflineMode()) {
            taskDurationSeconds = 0L;
        }

        if (durationLabel != null) {

            durationLabel.setText(
                    formatDuration(taskDurationSeconds)
            );
        }

        // =====================================================
        // WORK HOURS
        // =====================================================

        if (workHoursMiniLabel != null) {

            workHoursMiniLabel.setText(
                    formatShortHoursMinutes(workSeconds)
            );
        }

        // =====================================================
        // BREAK TIME
        // =====================================================

        if (breakTimeMiniLabel != null) {

            breakTimeMiniLabel.setText(
                    formatShortMinutes(breakSeconds)
            );
        }

        // =====================================================
        // LUNCH TIME
        // =====================================================

        if (lunchTimeMiniLabel != null) {

            lunchTimeMiniLabel.setText(
                    formatShortMinutes(lunchSeconds)
            );
        }

        // =====================================================
        // IDLE TIME
        // =====================================================

        if (idleTimeMiniLabel != null) {

            idleTimeMiniLabel.setText(
                    formatShortMinutes(idleDisplay)
            );
        }

        if (idleTimeLabel != null) {

            idleTimeLabel.setText(
                    formatDuration(idleDisplay)
            );
        }

        // =====================================================
        // IDLE HEADER
        // =====================================================

        if (idleTimerHeaderLabel != null) {

            long currentIdleSeconds = 0L;

            if ("IDLE".equals(localStatus)
                    && idleSegmentStartNanos > 0L) {

                currentIdleSeconds =
                        Math.max(
                                0L,
                                (nowNanos
                                        - idleSegmentStartNanos)
                                        / 1_000_000_000L
                        );
            }

            idleTimerHeaderLabel.setText(
                    "Idle for: "
                            + formatDuration(currentIdleSeconds)
            );

            idleTimerHeaderLabel.setIcon(
                    createWorkspaceIcon(
                            IconType.CLOCK,
                            PRIMARY_BLUE
                    )
            );
        }

        // =====================================================
        // CLOCK IN
        // =====================================================

        if (clockInMiniLabel != null) {

            clockInMiniLabel.setText(
                    formatMillisTime(
                            workStartedAtMillis
                    )
            );
        }

        // =====================================================
        // STARTED AT
        // =====================================================

        if (startedAtLabel != null) {

            startedAtLabel.setText(
                    currentTaskStartedAtMillis > 0L
                            ? formatMillisTime(
                            currentTaskStartedAtMillis
                    )
                            : "Ã¢â‚¬â€"
            );
        }

        // =====================================================
        // CURRENT TASK
        // =====================================================

        if (currentTaskLabel != null
                && taskCombo != null
                && taskCombo.getSelectedItem() != null) {

            currentTaskLabel.setText(
                    taskCombo.getSelectedItem().toString()
            );
        }

        // =====================================================
        // LAST ACTIVITY
        // =====================================================

        if (lastActivityLabel != null) {

            lastActivityLabel.setText(
                    "Last Activity : "
                            + formatTimeWithSeconds(
                            LocalTime.now()
                    )
            );

            lastActivityLabel.setIcon(
                    createWorkspaceIcon(
                            IconType.CLOCK,
                            TEXT_MUTED
                    )
            );
        }

        // =====================================================
        // ONLINE BADGE
        // =====================================================

        boolean isOffline = AgentApplication.isOfflineMode();
        java.awt.Color statusColor = isOffline ? DANGER_RED : SUCCESS_GREEN;
        String statusText = isOffline ? "Offline" : "Online";

        if (onlineBadgeLabel != null) {
            onlineBadgeLabel.setText(statusText);
            onlineBadgeLabel.setForeground(statusColor);
        }

        if (onlineDot != null) {
            onlineDot.putClientProperty("dotColor", statusColor);
            onlineDot.repaint();
        }

        if (onlinePill != null) {
            onlinePill.repaint();
        }

        // =====================================================
        // ACTION BUTTON
        // =====================================================

        updateActionButtonText();
    }


    // =========================================================
    // BUTTON TEXT
    // =========================================================

    private void updateActionButtonText() {

        long nowNanos = System.nanoTime();

        // =====================================================
        // BREAK BUTTON
        // =====================================================

        if (breakButton != null) {

            long currentBreakSeconds = 0L;

            if (breakRunning && breakSegmentStartNanos > 0L) {
                currentBreakSeconds = Math.max(
                        0L,
                        (nowNanos
                                - breakSegmentStartNanos)
                                / 1_000_000_000L
                );
            }

            if (breakRunning) {

                breakButton.setText(
                        "End Break"
                                + formatDuration(currentBreakSeconds)

                );

            } else {

                breakButton.setText(
                        "Start Break"
                );
            }

            breakButton.setIcon(
                    createWorkspaceIcon(
                            IconType.BREAK,
                            WARNING_ORANGE
                    )
            );
        }

        // =====================================================
        // LUNCH BUTTON
        // =====================================================

        if (lunchButton != null) {

            long currentLunchSeconds = 0L;

            if (lunchRunning && lunchSegmentStartNanos > 0L) {
                currentLunchSeconds = Math.max(
                        0L,
                        (nowNanos
                                - lunchSegmentStartNanos)
                                / 1_000_000_000L
                );
            }

            if (lunchRunning) {

                lunchButton.setText(
                        "End Lunch   "
                                + formatDuration(currentLunchSeconds)

                );

            } else {

                lunchButton.setText(
                        "Start Lunch"
                );
            }

            lunchButton.setIcon(
                    createWorkspaceIcon(
                            IconType.LUNCH,
                            PURPLE_TEXT
                    )
            );
        }

        // =====================================================
        // WORK TIMER BUTTON
        // =====================================================

        if (workTimerButton != null) {

            if (breakRunning || lunchRunning) {
                workTimerButton.setText(
                        "Start Timer"
                );
                workTimerButton.setIcon(
                        createWorkspaceIcon(
                                IconType.START,
                                Color.WHITE
                        )
                );
            }

            else if ("IDLE".equals(localStatus)) {
                workTimerButton.setText(
                        "Start Timer"
                );
                workTimerButton.setIcon(
                        createWorkspaceIcon(
                                IconType.START,
                                Color.WHITE
                        )
                );
            }

            else {
                workTimerButton.setText(
                        workRunning
                                ? "Stop Timer"
                                : "Start Timer"
                );
                workTimerButton.setIcon(
                        createWorkspaceIcon(
                                workRunning
                                        ? IconType.STOP
                                        : IconType.START,
                                Color.WHITE
                        )
                );
            }
        }
    }


    // =========================================================
// STOP / RESUME WORK
// =========================================================

    private void toggleWorkTimer() {

        if (breakRunning || lunchRunning) {

            showInformation(
                    "Work Timer",
                    "End Break or Lunch before starting work."
            );

            return;
        }

        if (workRunning) {

            if (AgentApplication.isOfflineMode()) {
                stopOfflineWorkTimer();
            } else {
                stopTaskWork();
            }

            return;
        }

        if (AgentApplication.isOfflineMode()) {
            startOfflineWorkTimer();
        } else {
            startTaskWork();
        }
    }


// =========================================================
// BREAK
// =========================================================

    private void toggleBreak() {

        if (lunchRunning) {

            showInformation(
                    "Break",
                    "Please end Lunch before starting a Break."
            );

            return;
        }

        if (breakRunning) {

            breakRunning = false;

            if (breakSegmentStartNanos > 0L) {

                long breakSeconds =
                        Math.max(
                                0L,
                                (
                                        System.nanoTime()
                                                - breakSegmentStartNanos
                                ) / 1_000_000_000L
                        );

                breakAccumulatedSeconds += breakSeconds;
            }

            breakSegmentStartNanos = 0L;

            updateStatus("IDLE");

            sendAttendanceEvent(
                    AttendanceEventType.BREAK_ENDED
            );

            // -------------------------------------------------
            // BREAK ENDED -> IDLE
            // -------------------------------------------------

            sendAttendanceEvent(
                    AttendanceEventType.IDLE_STARTED
            );

            idleSegmentStartNanos =
                    System.nanoTime();

            idleAfterBreakLunch = true;

            updateActionButtonText();

            return;
        }

        // -----------------------------------------------------
        // START BREAK
        // -----------------------------------------------------

        if (!AgentApplication.isOfflineMode()) {
            if (workRunning) {
                stopTaskWork();
                if (workRunning) {
                    return;
                }
            } else {
                closeEmployeeWorkSegment();
            }
        } else {
            closeEmployeeWorkSegment();
            workRunning = false;
            workSegmentStartNanos = 0L;
            AgentApplication.setTaskWorkRunning(false);
        }

        breakRunning = true;

        breakSegmentStartNanos =
                System.nanoTime();

        workSegmentStartNanos = 0L;

        closeEmployeeWorkSegment();

        updateStatus("BREAK");

        sendAttendanceEvent(
                AttendanceEventType.BREAK_STARTED
        );

        updateActionButtonText();
    }


// =========================================================
// OFFLINE WORK TIMER
// =========================================================

    private void startOfflineWorkTimer() {

        System.out.println("=================================");
        System.out.println("START OFFLINE WORK TIMER");

        if (workRunning) {
            return;
        }



        /*
         * ---------------------------------------------------------
         * CLOSE MANUAL IDLE SEGMENT
         * ---------------------------------------------------------
         *
         * This happens when:
         *
         * BREAK_ENDED -> IDLE -> START TIMER
         *
         * or
         *
         * LUNCH_ENDED -> IDLE -> START TIMER
         *
         * or
         *
         * STOP TIMER -> IDLE -> START TIMER
         *
         * The local timer already calculates the idle duration.
         * We now also store IDLE_ENDED in attendance.
         */
        if (idleAfterBreakLunch
                && idleSegmentStartNanos > 0L) {

            long currentIdleSegmentSeconds =
                    Math.max(
                            0L,
                            (System.nanoTime()
                                    - idleSegmentStartNanos)
                                    / 1_000_000_000L
                    );

            idleAccumulatedSeconds +=
                    currentIdleSegmentSeconds;

            /*
             * Attendance:
             * IDLE period has ended.
             */
            sendAttendanceEvent(
                    AttendanceEventType.IDLE_ENDED
            );
        }

        /*
         * Reset manual idle segment.
         */
        idleAfterBreakLunch = false;
        idleSegmentStartNanos = 0L;


        /*
         * Start employee work timer.
         */
        employeeWorkSegmentStartNanos =
                System.nanoTime();


        /*
         * Offline mode does not use backend Task Work.
         */
        workSegmentStartNanos = 0L;
        currentTaskWorkId = null;
        currentTaskStartedAtMillis = 0L;
        workAccumulatedSeconds = 0L;


        workRunning = true;

        AgentApplication.setTaskWorkRunning(true);

        localStatus = "WORKING";

        updateStatus("WORKING");
        updateLocalUi();


        System.out.println(
                "OFFLINE WORK TIMER STARTED"
        );
    }

    private void stopOfflineWorkTimer() {

        System.out.println("=================================");
        System.out.println("STOP OFFLINE WORK TIMER");

        if (!workRunning) {
            return;
        }


        /*
         * Clear running work segment.
         */
        closeEmployeeWorkSegment();
        workSegmentStartNanos = 0L;
        workAccumulatedSeconds = 0L;
        currentTaskWorkId = null;
        currentTaskStartedAtMillis = 0L;


        workRunning = false;

        AgentApplication.setTaskWorkRunning(false);


        /*
         * ---------------------------------------------------------
         * START MANUAL IDLE SEGMENT
         * ---------------------------------------------------------
         *
         * Employee stopped the timer manually.
         *
         * WORKING
         *    Ã¢â€ â€œ
         * IDLE_STARTED
         *    Ã¢â€ â€œ
         * PAUSED / IDLE
         */
        idleSegmentStartNanos =
                System.nanoTime();

        idleAfterBreakLunch = true;


        /*
         * Store the manual idle start in attendance.
         */
        sendAttendanceEvent(
                AttendanceEventType.IDLE_STARTED
        );


        localStatus = "IDLE";

        updateStatus("IDLE");
        updateLocalUi();


        System.out.println(
                "OFFLINE WORK TIMER STOPPED -> IDLE"
        );
    }


    // =========================================================
    // TASK WORK - START
    // =========================================================

    private void startTaskWork() {

        if (taskWorkStateLoading) {

            showInformation(
                    "Task Timer",
                    "Please wait while the current task status is being checked."
            );

            return;
        }


        if (workRunning
                && currentTaskWorkId != null
                && !currentTaskWorkId.isBlank()) {

            showInformation(
                    "Task Timer",
                    "A task timer is already running. Stop the current task before starting another one."
            );

            return;
        }


        if (currentTaskId == null
                || currentTaskId.isBlank()) {

            String selectedTitle =
                    taskCombo != null
                            && taskCombo.getSelectedItem() != null
                            ? taskCombo.getSelectedItem()
                            .toString()
                            .trim()
                            : "";

            currentTaskId =
                    taskIdByTitle.get(selectedTitle);
        }


        if (currentTaskId == null
                || currentTaskId.isBlank()) {

            showInformation(
                    "Task Timer",
                    "Please select a task before starting the timer."
            );

            return;
        }


        String url =
                serverUrl
                        + "/api/task-work/start";


        String requestBody =
                "{"
                        + "\"taskId\":\""
                        + escapeJson(currentTaskId)
                        + "\","
                        + "\"employeeId\":\""
                        + escapeJson(employeeId)
                        + "\","
                        + "\"employeeCode\":\""
                        + escapeJson(employeeCode)
                        + "\","
                        + "\"deviceId\":\""
                        + escapeJson(deviceId)
                        + "\""
                        + "}";


        System.out.println("=================================");
        System.out.println("START TASK WORK");
        System.out.println("URL: " + url);
        System.out.println("Task ID: " + currentTaskId);
        System.out.println("Employee ID: " + employeeId);
        System.out.println("Employee Code: " + employeeCode);
        System.out.println("Device ID: " + deviceId);
        System.out.println("Request: " + requestBody);


        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(requestBody)
                            )
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );


            System.out.println(
                    "POST /api/task-work/start -> "
                            + response.statusCode()
            );

            System.out.println("Response:");
            System.out.println(response.body());


            /*
             * -----------------------------------------------------
             * HTTP 403
             * -----------------------------------------------------
             */
            if (response.statusCode() == 403) {

                System.out.println(
                        "TASK WORK START REJECTED: HTTP 403"
                );

                showInformation(
                        "Task Timer - HTTP 403",
                        "The server rejected task start.\n\n"
                                + response.body()
                );

                return;
            }


            /*
             * -----------------------------------------------------
             * OTHER HTTP ERRORS
             * -----------------------------------------------------
             */
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                showInformation(
                        "Task Timer",
                        "Unable to start task. HTTP "
                                + response.statusCode()
                                + "\n\n"
                                + response.body()
                );

                return;
            }


            String responseJson =
                    response.body();


            /*
             * -----------------------------------------------------
             * GET WORK ID
             * -----------------------------------------------------
             */
            currentTaskWorkId =
                    firstNonBlank(
                            extractJsonValue(
                                    responseJson,
                                    "workId"
                            ),
                            extractJsonValue(
                                    responseJson,
                                    "id"
                            )
                    );


            if (currentTaskWorkId == null
                    || currentTaskWorkId.isBlank()) {

                System.out.println(
                        "WARNING: Task work started but no workId was found in response."
                );

                showInformation(
                        "Task Timer",
                        "Task started, but the backend did not return a work ID."
                );

                return;
            }


            /*
             * -----------------------------------------------------
             * CURRENT TASK TITLE
             * -----------------------------------------------------
             */
            if (taskCombo != null
                    && taskCombo.getSelectedItem() != null) {

                currentTaskTitle =
                        taskCombo
                                .getSelectedItem()
                                .toString()
                                .trim();
            }


            String startedAtJson =
                    extractJsonValue(
                            responseJson,
                            "startedAt"
                    );


            currentTaskStartedAtMillis =
                    parseInstantMillis(
                            startedAtJson
                    );


            /*
             * -----------------------------------------------------
             * CLOSE MANUAL IDLE SEGMENT
             * -----------------------------------------------------
             *
             * This is the important change.
             *
             * BREAK_ENDED
             *      Ã¢â€ â€œ
             * IDLE_STARTED
             *      Ã¢â€ â€œ
             * wait
             *      Ã¢â€ â€œ
             * START TIMER
             *      Ã¢â€ â€œ
             * IDLE_ENDED
             *      Ã¢â€ â€œ
             * WORKING
             *
             * Same for Lunch and manual Stop Timer.
             */
            if (idleAfterBreakLunch
                    && idleSegmentStartNanos > 0L) {

                long currentIdleSegmentSeconds =
                        Math.max(
                                0L,
                                (System.nanoTime()
                                        - idleSegmentStartNanos)
                                        / 1_000_000_000L
                        );


                idleAccumulatedSeconds +=
                        currentIdleSegmentSeconds;


                /*
                 * Store IDLE_ENDED in attendance.
                 */
                sendAttendanceEvent(
                        AttendanceEventType.IDLE_ENDED
                );
            }


            /*
             * Reset manual idle segment.
             */
            idleAfterBreakLunch = false;
            idleSegmentStartNanos = 0L;


            /*
             * -----------------------------------------------------
             * START WORK SEGMENT
             * -----------------------------------------------------
             */
            workAccumulatedSeconds = 0L;

            /*
             * Keep existing behavior:
             * current task start time is the actual local start.
             */
            currentTaskStartedAtMillis =
                    System.currentTimeMillis();


            long segmentNowNanos =
                    System.nanoTime();


            workSegmentStartNanos =
                    segmentNowNanos;

            employeeWorkSegmentStartNanos =
                    segmentNowNanos;


            workRunning = true;

            AgentApplication.setTaskWorkRunning(
                    true
            );

            localStatus = "WORKING";


            updateStatus("WORKING");
            updateLocalUi();


            System.out.println(
                    "TASK WORK STARTED"
            );

            System.out.println(
                    "Work ID: "
                            + currentTaskWorkId
            );


        } catch (Exception ex) {

            System.out.println(
                    "Failed to start task work: "
                            + ex.getMessage()
            );

            ex.printStackTrace();


            showInformation(
                    "Task Timer",
                    "Failed to start task.\n\n"
                            + ex.getMessage()
            );
        }
    }


    // =========================================================
    // EMPLOYEE WORK TIME - CLOSE CURRENT WORK SEGMENT
    // =========================================================

    private void closeEmployeeWorkSegment() {

        if (employeeWorkSegmentStartNanos <= 0L) {
            return;
        }

        long nowNanos =
                System.nanoTime();

        long currentEmployeeWorkSeconds =
                Math.max(
                        0L,
                        (
                                nowNanos
                                        - employeeWorkSegmentStartNanos
                        ) / 1_000_000_000L
                );

        employeeWorkAccumulatedSeconds +=
                currentEmployeeWorkSeconds;

        employeeWorkSegmentStartNanos = 0L;
    }


    // =========================================================
    // TASK WORK - STOP
    // =========================================================

    private boolean stopTaskWork() {

        if (!workRunning) {
            return true;
        }


        if (currentTaskWorkId == null
                || currentTaskWorkId.isBlank()) {

            System.out.println("No backend task-work session ID is available. Stopping local offline timer instead.");
            stopOfflineWorkTimer();
            return true;
        }


        String workId =
                currentTaskWorkId;


        String url =
                serverUrl
                        + "/api/task-work/"
                        + workId
                        + "/stop";


        /*
         * TaskWorkStopRequest requires employeeId.
         */
        String requestBody =
                "{"
                        + "\"employeeId\":\""
                        + escapeJson(employeeId)
                        + "\""
                        + "}";


        System.out.println("=================================");
        System.out.println("STOP TASK WORK");
        System.out.println("Work ID: " + workId);
        System.out.println("Employee ID: " + employeeId);
        System.out.println("URL: " + url);
        System.out.println(
                "Stop Request Body: "
                        + requestBody
        );


        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(requestBody)
                            )
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );


            System.out.println(
                    "POST /api/task-work/"
                            + workId
                            + "/stop -> "
                            + response.statusCode()
            );

            System.out.println("Response:");
            System.out.println(response.body());


            /*
             * -----------------------------------------------------
             * HTTP 403
             * -----------------------------------------------------
             */
            if (response.statusCode() == 403) {

                System.out.println(
                        "TASK WORK STOP REJECTED: HTTP 403"
                );

                showInformation(
                        "Task Timer - HTTP 403",
                        "The server rejected task stop.\n\n"
                                + response.body()
                );

                return false;
            }


            /*
             * -----------------------------------------------------
             * OTHER HTTP ERRORS
             * -----------------------------------------------------
             */
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                showInformation(
                        "Task Timer",
                        "Unable to stop task. HTTP "
                                + response.statusCode()
                                + "\n\n"
                                + response.body()
                );

                return false;
            }


            /*
             * -----------------------------------------------------
             * STOP WORKING STATE
             * -----------------------------------------------------
             */
            closeEmployeeWorkSegment();

            workRunning = false;

            AgentApplication.setTaskWorkRunning(
                    false
            );

            workSegmentStartNanos = 0L;

            currentTaskWorkId = null;

            workAccumulatedSeconds = 0L;


            /*
             * -----------------------------------------------------
             * START MANUAL IDLE SEGMENT
             * -----------------------------------------------------
             *
             * WORKING
             *    Ã¢â€ â€œ
             * STOP TIMER
             *    Ã¢â€ â€œ
             * IDLE_STARTED
             *    Ã¢â€ â€œ
             * PAUSED / IDLE
             */
            idleSegmentStartNanos =
                    System.nanoTime();

            idleAfterBreakLunch = true;


            /*
             * Store manual IDLE_STARTED.
             */
            sendAttendanceEvent(
                    AttendanceEventType.IDLE_STARTED
            );


            localStatus = "IDLE";

            updateStatus("IDLE");
            updateLocalUi();


            System.out.println(
                    "TASK WORK STOPPED SUCCESSFULLY"
            );

            System.out.println(
                    "Work ID: "
                            + workId
            );

            System.out.println(
                    "Employee ID: "
                            + employeeId
            );


            return true;


        } catch (Exception ex) {

            System.out.println(
                    "Failed to stop task work: "
                            + ex.getMessage()
            );

            ex.printStackTrace();


            showInformation(
                    "Task Timer",
                    "Failed to stop task.\n\n"
                            + ex.getMessage()
            );


            return false;
        }
    }

    private String extractJsonValue(
            String json,
            String key
    ) {

        if (json == null || key == null) {
            return null;
        }

        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);

        if (start == -1) {
            return null;
        }

        start += search.length();
        int end = json.indexOf('"', start);

        if (end == -1) {
            return null;
        }

        return json.substring(start, end);
    }


    private String firstNonBlank(
            String first,
            String second
    ) {

        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }


    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }



// =========================================================
// BREAK
// =========================================================

    private void toggleLunch() {

        // -----------------------------------------------------
        // BREAK IS ACTIVE
        // -----------------------------------------------------

        if (breakRunning) {

            showInformation(
                    "Lunch",
                    "Please end Break before starting Lunch."
            );

            return;
        }


        // -----------------------------------------------------
        // START LUNCH
        // -----------------------------------------------------

        if (!lunchRunning) {

            /*
             * ONLINE MODE
             *
             * Stop current backend Task Work
             * before entering Lunch.
             */
            if (!AgentApplication.isOfflineMode()) {

                if (workRunning) {

                    stopTaskWork();

                    if (workRunning) {
                        return;
                    }
                } else {
                    closeEmployeeWorkSegment();
                }
            }


            /*
             * OFFLINE MODE
             */
            if (AgentApplication.isOfflineMode()) {

                closeEmployeeWorkSegment();

                workRunning = false;
                workSegmentStartNanos = 0L;

                AgentApplication.setTaskWorkRunning(
                        false
                );
            }


            // -------------------------------------------------
            // START LUNCH TIMER
            // -------------------------------------------------

            lunchSegmentStartNanos =
                    System.nanoTime();

            lunchRunning = true;


            /*
             * Task Timer must not run during Lunch.
             */
            AgentApplication.setTaskWorkRunning(
                    false
            );


            /*
             * Tell AgentApplication Lunch is active.
             */
            AgentApplication.setLunchRunning(
                    true
            );


            /*
             * Update UI.
             */
            updateStatus("LUNCH");


            /*
             * Send / queue LUNCH_STARTED.
             */
            sendAttendanceEvent(
                    AttendanceEventType.LUNCH_STARTED
            );


            System.out.println(
                    "LUNCH STARTED"
                            + (
                            AgentApplication.isOfflineMode()
                                    ? " [OFFLINE]"
                                    : " [ONLINE]"
                    )
            );
        }


        // -----------------------------------------------------
        // END LUNCH
        // -----------------------------------------------------

        else {

            /*
             * Store completed Lunch duration.
             */
            if (lunchSegmentStartNanos > 0L) {

                lunchAccumulatedSeconds +=
                        Math.max(
                                0L,
                                (System.nanoTime()
                                        - lunchSegmentStartNanos)
                                        / 1_000_000_000L
                        );
            }


            lunchSegmentStartNanos = 0L;

            lunchRunning = false;

            AgentApplication.setLunchRunning(
                    false
            );

            AgentApplication.setTaskWorkRunning(
                    false
            );


            /*
             * Employee Work Time does NOT automatically resume.
             *
             * User must explicitly start the timer again.
             */
            workRunning = false;

            workSegmentStartNanos = 0L;

            employeeWorkSegmentStartNanos = 0L;


            /*
             * -------------------------------------------------
             * ENTER IDLE AFTER LUNCH
             * -------------------------------------------------
             */
            idleSegmentStartNanos =
                    System.nanoTime();

            idleAfterBreakLunch = true;

            localStatus = "IDLE";

            updateStatus("IDLE");


            /*
             * -------------------------------------------------
             * LUNCH ENDED
             * -------------------------------------------------
             */
            sendAttendanceEvent(
                    AttendanceEventType.LUNCH_ENDED
            );


            /*
             * -------------------------------------------------
             * NEW:
             * LUNCH ENDED -> IDLE STARTED
             * -------------------------------------------------
             */
            sendAttendanceEvent(
                    AttendanceEventType.IDLE_STARTED
            );


            System.out.println(
                    "LUNCH ENDED -> IDLE"
                            + (
                            AgentApplication.isOfflineMode()
                                    ? " [OFFLINE]"
                                    : " [ONLINE]"
                    )
            );
        }


        /*
         * Refresh all UI timers.
         */
        updateLocalUi();
    }


    // =========================================================
    // STATUS
    // =========================================================

    private void updateStatus(
            String status
    ) {

        if (currentStatusBadge == null) {
            return;
        }


        String normalized =
                safe(status)
                        .toUpperCase();


        if (normalized.isEmpty()) {
            normalized =
                    "WORKING";
        }


        localStatus =
                normalized;


        currentStatusBadge.setText(
                normalized
        );


        // =====================================================
        // WORKING
        // =====================================================

        if ("WORKING".equals(normalized)) {

            currentStatusBadge.setForeground(
                    SUCCESS_GREEN
            );


            currentStatusBadge.setBackground(
                    MINI_GREEN_BG
            );


            currentStatusDotColor =
                    SUCCESS_GREEN;
        }


        // =====================================================
        // IDLE
        // =====================================================

        else if ("IDLE".equals(normalized)) {

            currentStatusBadge.setForeground(
                    WARNING_ORANGE
            );


            currentStatusBadge.setBackground(
                    MINI_ORANGE_BG
            );


            currentStatusDotColor =
                    WARNING_ORANGE;
        }


        // =====================================================
        // BREAK
        // =====================================================

        else if ("BREAK".equals(normalized)) {

            currentStatusBadge.setForeground(
                    WARNING_ORANGE
            );


            currentStatusBadge.setBackground(
                    MINI_ORANGE_BG
            );


            currentStatusDotColor =
                    WARNING_ORANGE;
        }


        // =====================================================
        // LUNCH
        // =====================================================

        else if ("LUNCH".equals(normalized)) {

            currentStatusBadge.setForeground(
                    PURPLE_TEXT
            );


            currentStatusBadge.setBackground(
                    MINI_PURPLE_BG
            );


            currentStatusDotColor =
                    PURPLE_TEXT;
        }


        else {

            currentStatusBadge.setForeground(
                    TEXT_MUTED
            );


            currentStatusBadge.setBackground(
                    BORDER_CARD
            );


            currentStatusDotColor =
                    TEXT_MUTED;
        }


        // =====================================================
        // UPDATE DOT
        // =====================================================

        if (statusDotPanel != null) {

            statusDotPanel.putClientProperty(
                    "dotColor",
                    currentStatusDotColor
            );


            statusDotPanel.repaint();
        }


        currentStatusBadge.repaint();
    }


    // =========================================================
    // SEND ATTENDANCE EVENT
    // =========================================================

    private void sendAttendanceEvent(
            AttendanceEventType eventType
    ) {

        Thread thread = new Thread(() -> {

            AttendanceEvent event = null;

            try {

                event = eventFactory.create(eventType);

                // Persist the exact same event before sending.
                AgentApplication.persistAttendanceState(event);

                AttendanceSendResult result =
                        eventSender.sendEvent(event);

                System.out.println(
                        "Workspace attendance event: "
                                + eventType
                                + " -> "
                                + result
                                + " | deviceId="
                                + deviceId
                );

                if (result == AttendanceSendResult.SUCCESS) {
                    System.out.println(
                            "Attendance event sent successfully: "
                                    + eventType
                    );
                    return;
                }

                if (result == AttendanceSendResult.REJECTED) {
                    System.out.println(
                            "Attendance event rejected by server: "
                                    + eventType
                                    + ". Event will NOT be queued."
                    );
                    return;
                }

                // RETRY/network failure: queue the exact same event.
                System.out.println(
                        "Attendance event failed to send. "
                                + "Queuing for retry: "
                                + eventType
                );

                AgentApplication.queueAttendanceEvent(event);

            } catch (Exception ex) {

                System.out.println(
                        "Unable to send "
                                + eventType
                                + ": "
                                + ex.getMessage()
                );

                // The event already has its original ID/timestamp. Preserve it.
                if (event != null) {
                    AgentApplication.queueAttendanceEvent(event);
                }
            }
        });

        thread.setName(
                "Workspace-Attendance-"
                        + eventType
        );

        thread.setDaemon(true);
        thread.start();
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private void handleLogout() {

        if (logoutInProgress) {
            return;
        }

        logoutInProgress = true;

        System.out.println("=================================");
        System.out.println("LOGOUT REQUESTED");

        // Do not leave a RUNNING task-work record behind.
        // WORK_ENDED is sent only after task stop succeeds.
        if (workRunning) {

            if (AgentApplication.isOfflineMode()) {

                stopOfflineWorkTimer();

            } else {

                boolean stopped =
                        stopTaskWork();

                if (!stopped) {

                    logoutInProgress = false;

                    System.out.println(
                            "Logout cancelled because the active task could not be stopped."
                    );

                    return;
                }
            }
        }

        // Break/Lunch are separate attendance states. Do not send
        // WORK_ENDED while one of them is still active.
        if (breakRunning || lunchRunning) {

            logoutInProgress = false;

            showInformation(
                    "Logout",
                    "Please end Break or Lunch before logging out."
            );

            return;
        }

        System.out.println(
                "Clearing remembered credentials on logout."
        );
        org.example.security.WindowsCredentialManager.clearCredentials();

        // This sends WORK_ENDED. AgentApplication stops the complete
        // monitoring session only after the server confirms success.
        sendWorkEndedAndStopSession();
    }


    // =========================================================
    // SEND WORK END AND STOP SESSION
    // =========================================================

    private void sendWorkEndedAndStopSession() {

        Thread thread = new Thread(() -> {

            AttendanceEvent event = null;

            try {

                event = eventFactory.create(
                        AttendanceEventType.WORK_ENDED
                );

                // Persist the exact same WORK_ENDED event before sending.
                AgentApplication.persistAttendanceState(event);

                AttendanceSendResult result =
                        eventSender.sendEvent(event);

                System.out.println(
                        "WORK_ENDED -> " + result
                );

                if (result == AttendanceSendResult.SUCCESS) {

                    System.out.println(
                            "WORK_ENDED event sent successfully."
                    );

                    AgentApplication.stopAgentSession();
                    return;
                }

                if (result == AttendanceSendResult.REJECTED) {

                    System.out.println(
                            "WORK_ENDED event was rejected by server."
                                    + " Agent session will remain active."
                                    + " Event will NOT be queued."
                    );
                    logoutInProgress = false;
                    return;
                }

                // RETRY/network failure: queue the exact same event.
                System.out.println(
                        "WORK_ENDED event failed to send. "
                                + "Queueing for retry."
                );

                AgentApplication.queueAttendanceEvent(event);

                System.out.println(
                        "WORK_ENDED queued. "
                                + "Stopping agent session."
                );
                AgentApplication.stopAgentSession();

            } catch (Exception ex) {

                System.out.println(
                        "Unable to send WORK_ENDED: "
                                + ex.getMessage()
                );

                if (event != null) {
                    System.out.println(
                            "WORK_ENDED could not be sent. "
                                    + "Queueing event for retry."
                    );
                    AgentApplication.queueAttendanceEvent(event);
                }

                System.out.println(
                        "WORK_ENDED queued. "
                                + "Stopping agent session."
                );
                AgentApplication.stopAgentSession();
            }
        });

        thread.setName("Attendance-WORK_ENDED");
        thread.setDaemon(true);
        thread.start();
    }





    // =========================================================
    // FORMAT CLOCK-IN TIME
    // =========================================================

    private String formatMillisTime(
            long millis
    ) {

        if (millis <= 0L) {
            return "Ã¢â‚¬â€";
        }


        return Instant
                .ofEpochMilli(millis)
                .atZone(
                        ZoneId.systemDefault()
                )
                .format(
                        DateTimeFormatter.ofPattern(
                                "hh:mm a"
                        )
                );
    }


    // =========================================================
    // DYNAMIC DATA
    // =========================================================

    private void loadDynamicWorkspaceData() {

        SwingWorker<Void, Void> worker =
                new SwingWorker<>() {

                    private List<String> projects =
                            Collections.emptyList();


                    private List<String> tasks =
                            Collections.emptyList();


                    @Override
                    protected Void doInBackground() {

                        projects =
                                fetchProjectOptions();


                        tasks =
                                fetchTaskOptions();


                        return null;
                    }


                    @Override
                    protected void done() {

                        populateCombo(
                                projectCombo,
                                projects
                        );


                        populateCombo(
                                taskCombo,
                                tasks
                        );


                        // populateCombo selects the first task. Keep the
                        // corresponding Mongo task id in local state so the
                        // Start Timer button can immediately use it.
                        if (!workRunning
                                && taskCombo != null
                                && taskCombo.getSelectedItem() != null) {

                            currentTaskTitle =
                                    taskCombo.getSelectedItem()
                                            .toString()
                                            .trim();

                            currentTaskId =
                                    taskIdByTitle.get(
                                            currentTaskTitle
                                    );

                            if (currentTaskLabel != null) {
                                currentTaskLabel.setText(
                                        currentTaskTitle
                                );
                            }
                        }


                        updateWelcomeLabel();


                        updateDateLabel();

                        // Backend is the source of truth. Restore any task
                        // that was RUNNING before this agent restarted.
                        restoreRunningTaskAsync();
                    }
                };


        worker.execute();
    }


    private void populateCombo(
            JComboBox<String> combo,
            List<String> values
    ) {

        if (combo == null) {
            return;
        }


        combo.removeAllItems();


        if (values == null) {
            return;
        }


        for (String value : values) {

            if (value != null
                    && !value
                    .trim()
                    .isEmpty()) {

                combo.addItem(
                        value.trim()
                );
            }
        }


        if (combo.getItemCount() > 0) {

            combo.setSelectedIndex(
                    0
            );
        }
    }


    private void updateWelcomeLabel() {

        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "<html>Welcome, <b>"
                            + employeeDisplayName
                            + "</b></html>"
            );
        }


        if (empCodeLabel != null) {

            empCodeLabel.setText(
                    "Employee Code: "
                            + employeeCode
            );
        }
    }


    private void updateDateLabel() {

        if (dateLabel != null) {

            String curDate =
                    LocalDate
                            .now()
                            .format(
                                    DateTimeFormatter.ofPattern(
                                            "EEEE, dd MMM yyyy"
                                    )
                            );


            dateLabel.setText(
                    curDate
            );
            dateLabel.setIcon(
                    createWorkspaceIcon(
                            IconType.CALENDAR,
                            TEXT_MUTED
                    )
            );
        }


        if (timeHeaderLabel != null) {

            timeHeaderLabel.setText(
                    formatTime(
                            LocalTime.now()
                    )
            );
        }
    }


    // =========================================================
    // FETCH PROJECT OPTIONS
    // =========================================================

    private List<String> fetchProjectOptions() {

        try {

            /*
             * Existing project API in the current agent.
             *
             * At this stage we keep the same endpoint
             * already used by the project.
             */
            String url =
                    serverUrl
                            + "/api/tasks";


            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );


            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                return Collections.emptyList();
            }


            String json =
                    response.body();


            Set<String> unique =
                    new LinkedHashSet<>();


            int index = 0;


            while (index < json.length()) {

                int fieldIndex =
                        json.indexOf(
                                "\"taskType\":\"",
                                index
                        );


                if (fieldIndex == -1) {
                    break;
                }


                int valueStart =
                        fieldIndex
                                + "\"taskType\":\""
                                .length();


                int valueEnd =
                        json.indexOf(
                                '"',
                                valueStart
                        );


                if (valueEnd == -1) {
                    break;
                }


                String value =
                        json.substring(
                                valueStart,
                                valueEnd
                        ).trim();


                if (!value.isEmpty()) {

                    unique.add(
                            value
                    );
                }


                index =
                        valueEnd + 1;
            }


            return new ArrayList<>(
                    unique
            );


        } catch (Exception ex) {

            System.out.println(
                    "Failed to fetch projects: "
                            + ex.getMessage()
            );


            return Collections.emptyList();
        }
    }


    // =========================================================
    // FETCH TASK OPTIONS
    // =========================================================

    private List<String> fetchTaskOptions() {

        try {

            taskIdByTitle.clear();

            String url =
                    serverUrl
                            + "/api/tasks";


            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                            .header(
                                    "Accept",
                                    "application/json"
                            )
                            .GET()
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );


            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                return Collections.emptyList();
            }


            String json =
                    response.body();


            Set<String> unique =
                    new LinkedHashSet<>();


            int index = 0;


            while (index < json.length()) {

                int titleIndex =
                        json.indexOf(
                                "\"title\":\"",
                                index
                        );


                if (titleIndex == -1) {
                    break;
                }


                int titleStart =
                        titleIndex
                                + "\"title\":\""
                                .length();


                int titleEnd =
                        json.indexOf(
                                '"',
                                titleStart
                        );


                if (titleEnd == -1) {
                    break;
                }


                String title =
                        json.substring(
                                titleStart,
                                titleEnd
                        ).trim();


                String taskId =
                        extractTaskIdNearTitle(
                                json,
                                titleIndex
                        );


                int assignedIndex =
                        json.indexOf(
                                "\"assignedTo\":\"",
                                titleEnd
                        );


                String assignedTo =
                        "";


                if (assignedIndex != -1) {

                    int assignedStart =
                            assignedIndex
                                    + "\"assignedTo\":\""
                                    .length();


                    int assignedEnd =
                            json.indexOf(
                                    '"',
                                    assignedStart
                            );


                    if (assignedEnd != -1) {

                        assignedTo =
                                json.substring(
                                        assignedStart,
                                        assignedEnd
                                ).trim();
                    }
                }


                boolean assignedToEmployee =
                        assignedTo.isEmpty()
                                || employeeDisplayName
                                .equalsIgnoreCase(
                                        assignedTo
                                )
                                || employeeId.equals(
                                assignedTo
                        )
                                || employeeCode.equals(
                                assignedTo
                        );


                if (!title.isEmpty()
                        && assignedToEmployee) {

                    unique.add(
                            title
                    );

                    if (taskId != null
                            && !taskId.isBlank()) {

                        taskIdByTitle.put(
                                title,
                                taskId
                        );
                    }
                }


                index =
                        titleEnd + 1;
            }


            return new ArrayList<>(
                    unique
            );


        } catch (Exception ex) {

            System.out.println(
                    "Failed to fetch tasks: "
                            + ex.getMessage()
            );


            return Collections.emptyList();
        }
    }


    // =========================================================
    // RESTORE RUNNING TASK FROM BACKEND
    // =========================================================

    private void restoreRunningTaskAsync() {

        if (employeeId == null || employeeId.isBlank()) {
            taskWorkStateLoading = false;
            return;
        }

        taskWorkStateLoading = true;

        if (workTimerButton != null) {
            workTimerButton.setEnabled(false);
            workTimerButton.setText("Checking Timer...");
        }

        System.out.println("=================================");
        System.out.println("CHECK RUNNING TASK WORK");
        System.out.println("Employee ID: " + employeeId);

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            @Override
            protected String doInBackground() {

                try {
                    String url =
                            serverUrl
                                    + "/api/task-work/employee/"
                                    + employeeId
                                    + "/running";

                    HttpRequest request =
                            HttpRequest.newBuilder()
                                    .uri(URI.create(url))
                            .header("Authorization", "Bearer " + org.example.config.AgentTokenHolder.getToken())
                                    .header("Accept", "application/json")
                                    .GET()
                                    .build();

                    HttpResponse<String> response =
                            httpClient.send(
                                    request,
                                    HttpResponse.BodyHandlers.ofString()
                            );

                    System.out.println(
                            "GET /api/task-work/employee/"
                                    + employeeId
                                    + "/running -> "
                                    + response.statusCode()
                    );
                    System.out.println("Running task response:");
                    System.out.println(response.body());

                    if (response.statusCode() < 200
                            || response.statusCode() >= 300) {
                        return null;
                    }

                    String body = response.body();

                    if (body == null
                            || body.trim().isEmpty()
                            || "null".equalsIgnoreCase(body.trim())) {
                        return null;
                    }

                    return body.trim();

                } catch (Exception ex) {

                    System.out.println(
                            "Failed to check running task: "
                                    + ex.getMessage()
                    );

                    return null;
                }
            }

            @Override
            protected void done() {

                try {
                    String json = get();

                    if (json != null) {
                        restoreRunningTaskFromJson(json);
                    } else {
                        System.out.println(
                                "No running task found for employee "
                                        + employeeId
                        );
                    }

                } catch (Exception ex) {

                    System.out.println(
                            "Running task restore failed: "
                                    + ex.getMessage()
                    );

                } finally {

                    taskWorkStateLoading = false;
                    updateActionButtonText();

                    if (workTimerButton != null) {
                        workTimerButton.setEnabled(true);
                    }

                    updateLocalUi();
                }
            }
        };

        worker.execute();
    }


    /*
     * Calculate completed task-work time that belongs to the current
     * employee workspace session.
     *
     * This deliberately does NOT sum all historical Mongo records.
     * A task can span midnight, so the overlap between the task-work
     * interval and this workspace session is calculated instead.
     */
    private long sumTaskWorkForCurrentWorkspace(
            String json,
            long workspaceStartMillis
    ) {

        if (json == null || json.isBlank()) {
            return 0L;
        }

        long total = 0L;

        Pattern objectPattern =
                Pattern.compile(
                        "\\{[^{}]*\\}",
                        Pattern.DOTALL
                );

        Matcher objectMatcher =
                objectPattern.matcher(json);

        while (objectMatcher.find()) {

            String object =
                    objectMatcher.group();

            String status =
                    extractJsonValue(
                            object,
                            "status"
                    );

            if (!"STOPPED".equalsIgnoreCase(status)) {
                continue;
            }

            long startedAtMillis =
                    parseInstantMillis(
                            extractJsonValue(
                                    object,
                                    "startedAt"
                            )
                    );

            long endedAtMillis =
                    parseInstantMillis(
                            extractJsonValue(
                                    object,
                                    "endedAt"
                            )
                    );

            if (startedAtMillis <= 0L
                    || endedAtMillis <= 0L
                    || endedAtMillis <= startedAtMillis) {
                continue;
            }

            long overlapStart =
                    Math.max(
                            startedAtMillis,
                            workspaceStartMillis
                    );

            if (endedAtMillis <= overlapStart) {
                continue;
            }

            long overlapSeconds =
                    java.time.Duration
                            .ofMillis(
                                    endedAtMillis
                                            - overlapStart
                            )
                            .getSeconds();

            if (overlapSeconds > 0L) {
                total += overlapSeconds;
            }
        }

        return total;
    }


    private long parseLongJsonValue(
            String json,
            String key
    ) {

        if (json == null || key == null) {
            return -1L;
        }

        String search =
                "\"" + key + "\":";

        int index =
                json.indexOf(search);

        if (index < 0) {
            return -1L;
        }

        int start =
                index + search.length();

        while (start < json.length()
                && Character.isWhitespace(
                json.charAt(start))) {

            start++;
        }

        int end = start;

        while (end < json.length()
                && Character.isDigit(
                json.charAt(end))) {

            end++;
        }

        if (end == start) {
            return -1L;
        }

        try {
            return Long.parseLong(
                    json.substring(start, end)
            );
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }


    private long parseInstantMillis(String value) {

        if (value == null || value.isBlank()) {
            return 0L;
        }

        try {
            return Instant.parse(value.trim())
                    .toEpochMilli();

        } catch (Exception ex) {

            System.out.println(
                    "Unable to parse Instant: "
                            + value
            );

            return 0L;
        }
    }


    private void restoreRunningTaskFromJson(String json) {

        String restoredWorkId =
                firstNonBlank(
                        extractJsonValue(json, "id"),
                        extractJsonValue(json, "workId")
                );

        String restoredTaskId =
                extractJsonValue(json, "taskId");

        String restoredTaskTitle =
                extractJsonValue(json, "taskTitle");

        String restoredStartedAt =
                extractJsonValue(json, "startedAt");

        String restoredStatus =
                extractJsonValue(json, "status");

        if (restoredWorkId == null
                || restoredWorkId.isBlank()
                || restoredTaskId == null
                || restoredTaskId.isBlank()
                || !"RUNNING".equalsIgnoreCase(restoredStatus)) {

            System.out.println(
                    "Running task response did not contain a valid RUNNING task."
            );
            return;
        }

        currentTaskWorkId = restoredWorkId;
        currentTaskId = restoredTaskId;
        currentTaskTitle =
                restoredTaskTitle == null
                        ? ""
                        : restoredTaskTitle.trim();

        if (taskCombo != null && !currentTaskTitle.isBlank()) {
            for (int i = 0; i < taskCombo.getItemCount(); i++) {
                String item = taskCombo.getItemAt(i);

                if (item != null
                        && item.trim().equalsIgnoreCase(currentTaskTitle)) {
                    taskCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        if (currentTaskLabel != null) {
            currentTaskLabel.setText(
                    currentTaskTitle.isBlank()
                            ? currentTaskId
                            : currentTaskTitle
            );
        }

        workRunning = true;
        AgentApplication.setTaskWorkRunning(true);
        localStatus = "WORKING";

        currentTaskStartedAtMillis =
                parseInstantMillis(restoredStartedAt);

        long elapsedSeconds = 0L;

        if (restoredStartedAt != null
                && !restoredStartedAt.isBlank()) {

            try {
                Instant startedAt = Instant.parse(restoredStartedAt);

                elapsedSeconds = Math.max(
                        0L,
                        java.time.Duration.between(
                                startedAt,
                                Instant.now()
                        ).getSeconds()
                );

            } catch (Exception ex) {
                System.out.println(
                        "Unable to parse restored task start time: "
                                + restoredStartedAt
                );
            }
        }

        workAccumulatedSeconds = 0L;
        long restoreNowNanos =
                System.nanoTime();

        workSegmentStartNanos =
                restoreNowNanos
                        - (elapsedSeconds * 1_000_000_000L);

        employeeWorkSegmentStartNanos =
                restoreNowNanos;

        updateStatus("WORKING");

        System.out.println(
                "RUNNING TASK RESTORED"
                        + " | Task: " + currentTaskTitle
                        + " | Task ID: " + currentTaskId
                        + " | Work ID: " + currentTaskWorkId
                        + " | Elapsed: " + elapsedSeconds + " seconds"
        );
    }


    // =========================================================
    // EXTRACT TASK ID
    // =========================================================

    private String extractTaskIdNearTitle(
            String json,
            int titleIndex
    ) {

        if (json == null || titleIndex < 0) {
            return null;
        }

        int objectStart =
                json.lastIndexOf('{', titleIndex);

        if (objectStart == -1) {
            return null;
        }

        String idKey = "\"id\":\"";

        int idIndex =
                json.indexOf(
                        idKey,
                        objectStart
                );

        if (idIndex == -1 || idIndex > titleIndex) {
            return null;
        }

        int idStart =
                idIndex + idKey.length();

        int idEnd =
                json.indexOf('"', idStart);

        if (idEnd == -1 || idEnd > titleIndex) {
            return null;
        }

        return json.substring(
                idStart,
                idEnd
        ).trim();
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // FORMAT TIME
    // =========================================================

    private String formatTime(
            LocalTime time
    ) {

        return time.format(
                DateTimeFormatter.ofPattern(
                        "hh:mm a"
                )
        );
    }


    private String formatTimeWithSeconds(
            LocalTime time
    ) {

        return time.format(
                DateTimeFormatter.ofPattern(
                        "hh:mm:ss a"
                )
        );
    }


    // =========================================================
    // FORMAT HH:MM:SS
    // =========================================================

    private String formatDuration(
            long totalSeconds
    ) {

        long hours =
                totalSeconds / 3600;


        long minutes =
                (
                        totalSeconds % 3600
                ) / 60;


        long seconds =
                totalSeconds % 60;


        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
        );
    }


    // =========================================================
    // FORMAT WORK HOURS
    // =========================================================

    private String formatShortHoursMinutes(
            long totalSeconds
    ) {

        long hours =
                totalSeconds / 3600;


        long minutes =
                (
                        totalSeconds % 3600
                ) / 60;


        long seconds =
                totalSeconds % 60;


        return String.format(
                "%dh %02dm %02ds",
                hours,
                minutes,
                seconds
        );
    }


    // =========================================================
    // FORMAT MINUTES
    // =========================================================

    private String formatShortMinutes(
            long totalSeconds
    ) {

        long minutes =
                totalSeconds / 60;


        long seconds =
                totalSeconds % 60;


        return String.format(
                "%dm %02ds",
                minutes,
                seconds
        );
    }


    // =========================================================
    // INFORMATION DIALOG
    // =========================================================

    private void showInformation(
            String title,
            String message
    ) {

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    EmployeeWorkspaceWindow window =
                            new EmployeeWorkspaceWindow(
                                    null,
                                    "TEST-DEVICE-001"
                            );


                    window.setVisible(
                            true
                    );
                }
        );
    }
}
