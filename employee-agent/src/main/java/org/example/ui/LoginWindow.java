

package org.example.ui;

import org.example.commucnication.AgentLoginService;
import org.example.config.AgentConfig;
import org.example.security.WindowsCredentialManager;
import org.example.security.OfflineAuthorizationManager;
import org.example.ui.theme.UITheme;
import static org.example.ui.theme.UITheme.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.function.Consumer;

public class LoginWindow extends JFrame {

// =========================================================
    // Core Components & State
    // =========================================================

    private JTextField usernameField;
    private JPasswordField passwordField;
    private final String deviceId;
    private final boolean popupMode;
    private JButton loginButton;
    private JLabel statusLabel;
    private EyeToggleLabel passwordToggle;
    private JCheckBox rememberMeCheckBox;

    // Top-right status badge components
    private StatusBadgePanel statusBadge;
    private JLabel statusDot;
    private JLabel statusText;

    private AgentLoginService loginService;
    private Consumer<AgentLoginService.LoginResult> onLoginSuccess;
    private boolean passwordVisible = false;

    // =========================================================
    // Color Palette
    // =========================================================

    private static final Color PRIMARY         = new Color(37, 99, 235);    // #2563EB
    private static final Color PRIMARY_HOVER   = new Color(29, 78, 216);    // #1D4ED8
    private static final Color PRIMARY_ACTIVE  = new Color(30, 64, 175);    // #1E40AF
    private static final Color NAVY            = new Color(15, 23, 42);     // #0F172A
    private static final Color TEXT_DARK       = new Color(30, 41, 59);     // #1E293B
    private static final Color TEXT_MUTED      = new Color(100, 116, 139);  // #64748B
    private static final Color TEXT_LIGHT      = new Color(148, 163, 184);  // #94A3B8
    private static final Color BORDER_LIGHT    = new Color(226, 232, 240);  // #E2E8F0
    private static final Color BORDER_INPUT    = new Color(203, 213, 225);  // #CBD5E1
    private static final Color BG_WINDOW       = new Color(241, 245, 249);  // #F1F5F9
    private static final Color BG_CARD         = Color.WHITE;
    private static final Color SUCCESS_GREEN   = new Color(16, 185, 129);   // #10B981
    private static final Color SUCCESS_BG      = new Color(236, 253, 245);  // #ECFDF5
    private static final Color SUCCESS_BORDER  = new Color(209, 250, 229);  // #D1FAE5
    private static final Color SUCCESS_TEXT    = new Color(4, 120, 87);     // #047857
    private static final Color ERROR_RED       = new Color(220, 38, 38);    // #DC2626
    private static final Color ERROR_BG        = new Color(254, 242, 242);  // #FEF2F2
    private static final Color ERROR_BORDER    = new Color(254, 202, 202);  // #FECACA
    private static final Color BANNER_BG       = new Color(248, 250, 252);  // #F8FAFC
    private static final Color SHIELD_BG       = new Color(219, 234, 254);  // #DBEAFE

    // =========================================================
    // Font Family
    // =========================================================

    private static final String FONT_FAMILY = getApplicationFont();

    private static String getApplicationFont() {
        String[] fonts = {"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String wanted : fonts) {
            for (String current : available) {
                if (current.equalsIgnoreCase(wanted)) {
                    return current;
                }
            }
        }
        return "Segoe UI";
    }

    // =========================================================
    // Constructors
    // =========================================================



    public LoginWindow() {
        this("", result -> {}, false);
    }

    public LoginWindow(
            String deviceId,
            Consumer<AgentLoginService.LoginResult> onLoginSuccess
    ) {
        this(deviceId, onLoginSuccess, false);
    }

    public LoginWindow(
            String deviceId,
            Consumer<AgentLoginService.LoginResult> onLoginSuccess,
            boolean popupMode
    ) {

        this.deviceId = deviceId;
        this.onLoginSuccess = onLoginSuccess;
        this.popupMode = popupMode;

        // this.loginService = new AgentLoginService("http://localhost:8082");

        AgentConfig config = new AgentConfig();
        this.loginService = new AgentLoginService(config.getServerUrl());

        setTitle("Employee Monitoring Agent");
        setSize(980, 640);
        setLocationRelativeTo(null);
        setResizable(false);

        // Normal LoginWindow closes the application.
        // Popup LoginWindow only closes the popup.
        if (popupMode) {
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        } else {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        setUndecorated(true);

        // Set the application icon for the taskbar / Alt+Tab switcher.
        AppIcon.apply(this);

        // Windows 11 style rounded window shape
        setShape(
                new RoundRectangle2D.Double(
                        0,
                        0,
                        980,
                        640,
                        16,
                        16
                )
        );

        // Root container with smooth rounded outer border
        JPanel root = new JPanel(new BorderLayout()) {

            @Override
            protected void paintComponent(Graphics g) {

                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(BORDER_LIGHT);

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

        root.setBackground(BG_WINDOW);

        // Custom Title Bar
        root.add(createTitleBar(), BorderLayout.NORTH);

        // Main Content
        root.add(createMainContent(), BorderLayout.CENTER);

        setContentPane(root);

        // Load previously remembered credentials.
        // This only pre-fills the login form;
        // it does NOT sign in automatically.
        loadRememberedCredentials();

        // Enter key submits login
        getRootPane().setDefaultButton(loginButton);
    }

    // =========================================================
    // Title Bar
    // =========================================================

    private JPanel createTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(980, 42));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));

        // Left Branding: EMS Rounded Badge + Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        left.setOpaque(false);

        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String text = "EMS";
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);
                g2.dispose();
            }
        };
        logoBadge.setPreferredSize(new Dimension(28, 24));
        logoBadge.setOpaque(false);
        left.add(logoBadge);

        JLabel title = new JLabel("Employee Monitoring Agent");
        title.setFont(new Font(FONT_FAMILY, Font.PLAIN, 13));
        title.setForeground(TEXT_DARK);
        left.add(title);

        bar.add(left, BorderLayout.WEST);

        // Right Window Controls: Minimize, Maximize, Close (Drawn via Graphics2D)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);

        JButton minimize = createWindowButton("minimize", false);

        JButton close    = createWindowButton("close", true);

        minimize.addActionListener(e -> setState(JFrame.ICONIFIED));

        close.addActionListener(e -> {
            if (popupMode) {
                dispose();
            } else {
                System.exit(0);
            }
        });
        controls.add(minimize);

        controls.add(close);

        bar.add(controls, BorderLayout.EAST);

        // Drag window support
        MouseAdapter dragAdapter = new MouseAdapter() {
            private Point start;
            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = e.getLocationOnScreen();
                setLocation(current.x - start.x, current.y - start.y);
            }
        };
        bar.addMouseListener(dragAdapter);
        bar.addMouseMotionListener(dragAdapter);

        return bar;
    }

    private JButton createWindowButton(String actionType, boolean isClose) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isHovered = getBackground().equals(new Color(239, 68, 68));
                g2.setColor(isHovered ? Color.WHITE : TEXT_MUTED);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if ("minimize".equals(actionType)) {
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(cx - 5, cy + 1, cx + 5, cy + 1);
                } else if ("maximize".equals(actionType)) {
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(cx - 5, cy - 5, 10, 10, 1, 1);
                } else if ("close".equals(actionType)) {
                    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                    g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                }
                g2.dispose();
            }
        };
        button.setPreferredSize(new Dimension(46, 42));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isClose) {
                    button.setBackground(new Color(239, 68, 68));
                } else {
                    button.setBackground(new Color(241, 245, 249));
                }
                button.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
                button.repaint();
            }
        });

        return button;
    }

    // =========================================================
    // Main Content
    // =========================================================

    private JPanel createMainContent() {
        JPanel container = new JPanel(new GridLayout(1, 2, 0, 0));
        container.setBackground(BG_WINDOW);

        // Left Branding & Illustration
        container.add(createLeftSection());

        // Right Elevated Login Card Panel
        container.add(createRightSection());

        return container;
    }

    // =========================================================
    // Left Section
    // =========================================================

    private JPanel createLeftSection() {
        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Sky blue gradient
                GradientPaint bgGradient = new GradientPaint(
                        0, 0, new Color(235, 244, 255),
                        w, h, new Color(246, 250, 255)
                );
                g2.setPaint(bgGradient);
                g2.fillRect(0, 0, w, h);

                // Subtle light window wave
                g2.setColor(new Color(255, 255, 255, 110));
                Path2D wave = new Path2D.Double();
                wave.moveTo(0, h * 0.55);
                wave.curveTo(w * 0.35, h * 0.60, w * 0.65, h * 0.48, w, h * 0.50);
                wave.lineTo(w, h);
                wave.lineTo(0, h);
                wave.closePath();
                g2.fill(wave);

                // Window pane light ray
                g2.setColor(new Color(255, 255, 255, 75));
                Polygon ray = new Polygon(
                        new int[]{w - 180, w, w, w - 80},
                        new int[]{0, 0, h - 180, h - 220},
                        4
                );
                g2.fill(ray);

                g2.dispose();
            }
        };

        // 1. Logo "EMS"
        JLabel logo = new JLabel("EMS");
        logo.setFont(new Font(FONT_FAMILY, Font.BOLD, 42));
        logo.setForeground(PRIMARY);
        logo.setBounds(46, 24, 200, 48);
        panel.add(logo);

        // 2. Subtitle
        JLabel logoSub = new JLabel("Employee Monitoring System");
        logoSub.setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
        logoSub.setForeground(new Color(71, 85, 105));
        logoSub.setBounds(48, 70, 280, 20);
        panel.add(logoSub);

        // 3. Catchphrase Heading
        JLabel heading = new JLabel("<html><div style='line-height: 1.15;'>"
                + "Secure.<br>"
                + "Productive.<br>"
                + "Together.</div></html>");
        heading.setFont(new Font(FONT_FAMILY, Font.BOLD, 36));
        heading.setForeground(NAVY);
        heading.setBounds(56, 100, 380, 145);
        panel.add(heading);

        // 4. Description
        JLabel desc = new JLabel("<html>A safer and smarter workplace<br>for a better tomorrow.</html>");
        desc.setFont(new Font(FONT_FAMILY, Font.PLAIN, 14));
        desc.setForeground(TEXT_MUTED);
        desc.setBounds(48, 254, 380, 42);
        panel.add(desc);

        // 5. Illustration
        IllustrationPanel illustration = new IllustrationPanel();
        illustration.setBounds(44, 280, 410, 190);
        panel.add(illustration);

        // 6. Active Indicator Bar
        JPanel indicatorBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        indicatorBar.setBounds(48, 485, 32, 5);
        indicatorBar.setOpaque(false);
        panel.add(indicatorBar);

        // 7. Bottom Feature Badges
        JPanel badge1 = createFeatureBadge("shield", "Secure", "Access");
        badge1.setBounds(46, 508, 110, 42);
        panel.add(badge1);

        JPanel badge2 = createFeatureBadge("chart", "Monitor", "Productivity");
        badge2.setBounds(162, 508, 120, 42);
        panel.add(badge2);

        JPanel badge3 = createFeatureBadge("users", "Build a", "Better Workplace");
        badge3.setBounds(290, 508, 150, 42);
        panel.add(badge3);

        return panel;
    }

    private JPanel createFeatureBadge(String iconType, String line1, String line2) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);

        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY);

                if ("shield".equals(iconType)) {
                    Path2D s = new Path2D.Double();
                    s.moveTo(2, 4);
                    s.curveTo(7, 2, 13, 2, 18, 4);
                    s.lineTo(18, 11);
                    s.curveTo(18, 17, 10, 22, 10, 22);
                    s.curveTo(10, 22, 2, 17, 2, 11);
                    s.closePath();
                    g2.fill(s);
                } else if ("chart".equals(iconType)) {
                    g2.fillRoundRect(1, 12, 4, 9, 2, 2);
                    g2.fillRoundRect(7, 7, 4, 14, 2, 2);
                    g2.fillRoundRect(13, 2, 4, 19, 2, 2);
                } else if ("users".equals(iconType)) {
                    g2.fillOval(7, 2, 8, 8);
                    g2.fillRoundRect(4, 11, 14, 10, 5, 5);
                    g2.fillOval(0, 4, 6, 6);
                    g2.fillRoundRect(0, 12, 6, 8, 3, 3);
                }
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(20, 24));
        p.add(icon, BorderLayout.WEST);

        JLabel text = new JLabel("<html><b style='color:#1E293B;'>" + line1 + "</b><br>"
                + "<span style='color:#475569;'>" + line2 + "</span></html>");
        text.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        p.add(text, BorderLayout.CENTER);

        return p;
    }

    // =========================================================
    // Right Section (Compact Elevated Login Card)
    // =========================================================

    private JPanel createRightSection() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_WINDOW);

        // Compact Card: 380px wide x 480px high
        RoundedCard card = new RoundedCard(BG_CARD, 20);
        card.setPreferredSize(new Dimension(380, 480));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 28, 18, 28));

        // 1. "Ready to connect" Status Badge (Top Right)
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusRow.setOpaque(false);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        statusBadge = new StatusBadgePanel(SUCCESS_BG, SUCCESS_BORDER);
        statusDot = new JLabel("●");
        statusDot.setFont(new Font(FONT_FAMILY, Font.BOLD, 9));
        statusDot.setForeground(SUCCESS_GREEN);
        statusBadge.add(statusDot);

        statusText = new JLabel("Ready to connect");
        statusText.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        statusText.setForeground(SUCCESS_TEXT);
        statusBadge.add(statusText);

        statusRow.add(statusBadge);
        card.add(statusRow);

        card.add(Box.createVerticalStrut(6));

        // 2. Heading "Welcome Back"
        JLabel welcome = new JLabel("Welcome Back");
        welcome.setFont(new Font(FONT_FAMILY, Font.BOLD, 26));
        welcome.setForeground(NAVY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(welcome);

        card.add(Box.createVerticalStrut(2));

        JLabel subtitle = new JLabel("Sign in to your employee account");
        subtitle.setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);

        card.add(Box.createVerticalStrut(14));

        // 3. Email Address (Aligned perfectly with input box)
        JLabel emailLabel = new JLabel("Email address");
        emailLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        emailLabel.setForeground(TEXT_DARK);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(emailLabel);

        card.add(Box.createVerticalStrut(5));

        usernameField = new PlaceholderTextField("Enter your email address");
        RoundedInputContainer emailContainer = new RoundedInputContainer(createMailIcon(), usernameField, null);
        emailContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(emailContainer);

        card.add(Box.createVerticalStrut(10));

        // 4. Password (Aligned perfectly with input box)
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font(FONT_FAMILY, Font.BOLD, 12));
        passwordLabel.setForeground(TEXT_DARK);
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordLabel);

        card.add(Box.createVerticalStrut(5));

        passwordField = new PlaceholderPasswordField("Enter your password");
        passwordToggle = new EyeToggleLabel();
        passwordToggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                togglePassword();
            }
        });

        RoundedInputContainer passwordContainer = new RoundedInputContainer(createLockIcon(), passwordField, passwordToggle);
        passwordContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordContainer);

        card.add(Box.createVerticalStrut(8));

        // 5. "Remember me" & "Forgot password?" Row
        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        optionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        rememberMeCheckBox = new ModernCheckBox("Remember me");
        optionsRow.add(rememberMeCheckBox, BorderLayout.WEST);

        JLabel forgotPwd = new JLabel("Forgot password?");
        forgotPwd.setFont(new Font(FONT_FAMILY, Font.BOLD, 11));
        forgotPwd.setForeground(PRIMARY);
        forgotPwd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        optionsRow.add(forgotPwd, BorderLayout.EAST);

        card.add(optionsRow);

        card.add(Box.createVerticalStrut(12));

        // 6. Sign In Button (Compact 40px height)
        loginButton = new ModernButton("→   Sign In");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setPreferredSize(new Dimension(324, 40));
        card.add(loginButton);

        // Hidden / lightweight status label reference
        statusLabel = new JLabel("");
        statusLabel.setVisible(false);
        card.add(statusLabel);

        card.add(Box.createVerticalStrut(10));

        // 7. Divider: —— OR ——
        JPanel orDivider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setColor(BORDER_LIGHT);
                int midY = getHeight() / 2;
                int textW = g2.getFontMetrics().stringWidth("OR");
                int centerX = getWidth() / 2;
                g2.drawLine(0, midY, centerX - textW / 2 - 10, midY);
                g2.drawLine(centerX + textW / 2 + 10, midY, getWidth(), midY);
                g2.setColor(TEXT_LIGHT);
                g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 10));
                g2.drawString("OR", centerX - textW / 2, midY + 4);
                g2.dispose();
            }
        };
        orDivider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        orDivider.setPreferredSize(new Dimension(324, 16));
        orDivider.setOpaque(false);
        orDivider.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(orDivider);

        card.add(Box.createVerticalStrut(10));

        // 8. Security Policy Banner
        JPanel policyBanner = createSecurityBanner();
        policyBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(policyBanner);

        wrapper.add(card);

        // Outer Column Container with Footer
        JPanel columnContainer = new JPanel(new BorderLayout());
        columnContainer.setOpaque(false);
        columnContainer.add(wrapper, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 36, 12, 36));

        JLabel emsFooter = new JLabel("—   Employee Monitoring System   —", SwingConstants.CENTER);
        emsFooter.setFont(new Font(FONT_FAMILY, Font.PLAIN, 11));
        emsFooter.setForeground(TEXT_LIGHT);
        footer.add(emsFooter, BorderLayout.CENTER);

        JLabel version = new JLabel("v" + org.example.AgentVersion.VERSION);
        version.setFont(new Font(FONT_FAMILY, Font.PLAIN, 10));
        version.setForeground(TEXT_LIGHT);
        footer.add(version, BorderLayout.EAST);

        columnContainer.add(footer, BorderLayout.SOUTH);

        // Login Action
        loginButton.addActionListener(e -> handleLogin());

        return columnContainer;
    }

    // =========================================================
    // Security Policy Banner Helper
    // =========================================================

    private JPanel createSecurityBanner() {

        JPanel banner = new JPanel(new BorderLayout(10, 0)) {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 =
                        (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                // Background
                g2.setColor(
                        new Color(248, 250, 252)
                );

                g2.fillRoundRect(
                        0,
                        0,
                        getWidth(),
                        getHeight(),
                        10,
                        10
                );

                // Border
                g2.setColor(
                        BORDER_LIGHT
                );

                g2.setStroke(
                        new BasicStroke(1f)
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

        banner.setOpaque(false);

        banner.setBorder(
                new EmptyBorder(
                        8,
                        10,
                        8,
                        10
                )
        );

        // IMPORTANT:
        // Increased from 44px to 54px
        banner.setPreferredSize(
                new Dimension(
                        324,
                        54
                )
        );

        banner.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        54
                )
        );

        // =========================================================
        // Shield
        // =========================================================

        JComponent shieldIcon = new JComponent() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 =
                        (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                int size = 28;

                int x =
                        (getWidth() - size) / 2;

                int y =
                        (getHeight() - size) / 2;

                // Circle background

                g2.setColor(
                        SHIELD_BG
                );

                g2.fillOval(
                        x,
                        y,
                        size,
                        size
                );

                // Shield

                g2.setColor(
                        PRIMARY
                );

                Path2D shield =
                        new Path2D.Double();

                shield.moveTo(
                        x + 8,
                        y + 8
                );

                shield.curveTo(
                        x + 11,
                        y + 6,
                        x + 17,
                        y + 6,
                        x + 20,
                        y + 8
                );

                shield.lineTo(
                        x + 20,
                        y + 14
                );

                shield.curveTo(
                        x + 20,
                        y + 19,
                        x + 14,
                        y + 22,
                        x + 14,
                        y + 22
                );

                shield.curveTo(
                        x + 14,
                        y + 22,
                        x + 8,
                        y + 19,
                        x + 8,
                        y + 14
                );

                shield.closePath();

                g2.fill(shield);

                // Check mark

                g2.setColor(
                        Color.WHITE
                );

                g2.setStroke(
                        new BasicStroke(
                                1.5f,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND
                        )
                );

                g2.drawLine(
                        x + 11,
                        y + 14,
                        x + 13,
                        y + 16
                );

                g2.drawLine(
                        x + 13,
                        y + 16,
                        x + 17,
                        y + 12
                );

                g2.dispose();
            }
        };

        shieldIcon.setPreferredSize(
                new Dimension(
                        30,
                        38
                )
        );

        banner.add(
                shieldIcon,
                BorderLayout.WEST
        );

        // =========================================================
        // Security Text
        // =========================================================

        JPanel textPanel =
                new JPanel();

        textPanel.setLayout(
                new BoxLayout(
                        textPanel,
                        BoxLayout.Y_AXIS
                )
        );

        textPanel.setOpaque(false);

        JLabel securityText =
                new JLabel(
                        "Your connection is secured by EMS."
                );

        securityText.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        11
                )
        );

        securityText.setForeground(
                TEXT_MUTED
        );

        securityText.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        textPanel.add(
                securityText
        );

        textPanel.add(
                Box.createVerticalStrut(3)
        );

        JLabel policyText =
                new JLabel(
                        "Company security policy applies."
                );

        policyText.setFont(
                new Font(
                        FONT_FAMILY,
                        Font.PLAIN,
                        10
                )
        );

        policyText.setForeground(
                TEXT_LIGHT
        );

        policyText.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        textPanel.add(
                policyText
        );

        banner.add(
                textPanel,
                BorderLayout.CENTER
        );

        return banner;
    }

    // =========================================================
    // Status Badge Component
    // =========================================================

    private static class StatusBadgePanel extends JPanel {
        private Color bgColor;
        private Color borderColor;

        StatusBadgePanel(Color bgColor, Color borderColor) {
            super(new FlowLayout(FlowLayout.CENTER, 5, 3));
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        public void setColors(Color bgColor, Color borderColor) {
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void updateStatus(String message, Color dotColor, Color textColor, Color bgColor, Color borderColor) {
        statusBadge.setColors(bgColor, borderColor);
        statusDot.setForeground(dotColor);
        statusText.setText(message);
        statusText.setForeground(textColor);
        statusLabel.setText(message);
        statusLabel.setForeground(textColor);
        
        if (dotColor == ERROR_RED && !this.isVisible()) {
            this.setVisible(true);
            this.toFront();
        }
    }


    // =========================================================
    // Perfectly Centered Vector Icons
    // =========================================================

    private JComponent createMailIcon() {
        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_LIGHT);
                g2.setStroke(new BasicStroke(1.3f));

                int iconW = 16;
                int iconH = 12;
                int x = (getWidth() - iconW) / 2;
                int y = (getHeight() - iconH) / 2;

                // Envelope outline
                g2.drawRoundRect(x, y, iconW, iconH, 2, 2);

                // Flap fold
                Path2D flap = new Path2D.Double();
                flap.moveTo(x + 1, y + 1);
                flap.lineTo(x + iconW / 2, y + 7);
                flap.lineTo(x + iconW - 1, y + 1);
                g2.draw(flap);

                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(20, 38));
        return icon;
    }

    private JComponent createLockIcon() {
        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_LIGHT);
                g2.setStroke(new BasicStroke(1.3f));

                int w = 14;
                int h = 16;
                int x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;

                // Shackle arch
                g2.drawArc(x + 3, y, 8, 8, 0, 180);
                g2.drawLine(x + 3, y + 4, x + 3, y + 6);
                g2.drawLine(x + 11, y + 4, x + 11, y + 6);

                // Body
                g2.drawRoundRect(x + 1, y + 6, 12, 9, 2, 2);

                // Keyhole
                g2.setColor(TEXT_LIGHT);
                g2.fillOval(x + 6, y + 9, 2, 2);
                g2.drawLine(x + 7, y + 10, x + 7, y + 12);

                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(20, 38));
        return icon;
    }

    // =========================================================
    // Password Visibility Toggle
    // =========================================================

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordField.setEchoChar((char) 0);
        } else {
            passwordField.setEchoChar('•');
        }
        passwordToggle.setVisibleState(passwordVisible);
    }

    // =========================================================
    // Remember Me
    // =========================================================

    // =========================================================
    // Remember Me
    // =========================================================

    /**
     * Loads Remember Me credentials from Windows Credential Manager.
     *
     * Important:
     * - Only pre-fills the login form.
     * - Does NOT automatically sign in.
     * - Employee must still click Sign In.
     */
    private void loadRememberedCredentials() {

        try {

            System.out.println(
                    "Checking Windows Credential Manager for "
                            + "Remember Me credentials..."
            );

            String[] savedCredentials =
                    WindowsCredentialManager.getSavedCredentials();

            if (savedCredentials != null
                    && savedCredentials.length >= 2
                    && savedCredentials[0] != null
                    && !savedCredentials[0].isBlank()
                    && savedCredentials[1] != null
                    && !savedCredentials[1].isEmpty()) {

                String savedUsername =
                        savedCredentials[0];

                String savedPassword =
                        savedCredentials[1];

                usernameField.setText(savedUsername);
                passwordField.setText(savedPassword);
                rememberMeCheckBox.setSelected(true);

                System.out.println(
                        "Remembered employee credentials loaded."
                );

                System.out.println(
                        "Remembered username loaded: "
                                + savedUsername
                );

            } else {

                rememberMeCheckBox.setSelected(false);

                System.out.println(
                        "No Remember Me credentials found."
                );
            }

        } catch (Exception e) {

            rememberMeCheckBox.setSelected(false);

            System.err.println(
                    "Unable to load Remember Me credentials."
            );

            System.err.println(
                    "Reason: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    public boolean attemptAutoLogin() {
        if (!usernameField.getText().trim().isEmpty() && passwordField.getPassword().length > 0) {
            handleLogin();
            return true;
        }
        return false;
    }

// =========================================================
// Login Execution Logic
// =========================================================

    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // =========================================================
        // Validate Input
        // =========================================================

        if (username.isEmpty() || password.isEmpty()) {

            updateStatus(
                    "Please enter credentials",
                    ERROR_RED,
                    ERROR_RED,
                    ERROR_BG,
                    ERROR_BORDER
            );

            return;
        }

        // =========================================================
        // Connecting Status
        // =========================================================

        updateStatus(
                "Connecting to EMS...",
                PRIMARY,
                PRIMARY,
                new Color(239, 246, 255),
                new Color(191, 219, 254)
        );

        loginButton.setEnabled(false);

        // =========================================================
        // Login Thread
        // =========================================================

        Thread loginThread = new Thread(() -> {

            AgentLoginService.LoginResult result =
                    loginService.login(username, password);

            SwingUtilities.invokeLater(() -> {

                // =====================================================
                // ONLINE LOGIN SUCCESS
                // =====================================================

                if (result != null && result.isSuccess()) {

                    // =================================================
                    // Remember Me
                    // =================================================

                    if (rememberMeCheckBox.isSelected()) {

                        boolean saved =
                                WindowsCredentialManager.saveCredentials(
                                        username,
                                        password
                                );

                        if (!saved) {

                            System.err.println(
                                    "Login succeeded, but Remember Me credentials could not be saved."
                            );
                        }

                    } else {

                        // User explicitly unchecked Remember Me.
                        // Remove previously saved credentials.

                        WindowsCredentialManager.clearCredentials();
                    }


                    // =================================================
                    // Save Offline Authorization
                    // =================================================

                    boolean offlineAuthorizationSaved =
                            OfflineAuthorizationManager.saveAuthorization(
                                    result.getEmployeeId(),
                                    result.getEmployeeCode(),
                                    username,
                                    deviceId,
                                    result.getRefreshToken()
                            );

                    if (!offlineAuthorizationSaved) {

                        System.err.println(
                                "WARNING: Offline authorization could not be saved."
                        );

                    } else {

                        System.out.println(
                                "Offline authorization saved successfully."
                        );
                    }


                    // =================================================
                    // Login Successful
                    // =================================================

                    updateStatus(
                            "Login successful",
                            SUCCESS_GREEN,
                            SUCCESS_TEXT,
                            SUCCESS_BG,
                            SUCCESS_BORDER
                    );

                    System.out.println(
                            "Employee ID: "
                                    + result.getEmployeeId()
                    );

                    System.out.println(
                            "Employee Code: "
                                    + result.getEmployeeCode()
                    );


                    // =================================================
                    // Continue to AgentApplication
                    // =================================================

                    if (onLoginSuccess != null) {

                        onLoginSuccess.accept(result);
                    }


                    // =====================================================
                    // SERVER UNAVAILABLE → OFFLINE LOGIN
                    // =====================================================

                } else if (result != null
                        && result.isServerUnavailable()) {

                    System.out.println(
                            "EMS server unavailable."
                    );

                    System.out.println(
                            "Checking offline authorization..."
                    );


                    // =================================================
                    // Load Offline Authorization
                    // =================================================

                    OfflineAuthorizationManager.OfflineAuthorization authorization =
                            OfflineAuthorizationManager.getAuthorization();


                    // =================================================
                    // No Offline Authorization
                    // =================================================

                    if (authorization == null) {

                        System.out.println(
                                "No offline authorization found."
                        );

                        updateStatus(
                                "EMS unavailable - Offline access not authorized",
                                ERROR_RED,
                                ERROR_RED,
                                ERROR_BG,
                                ERROR_BORDER
                        );

                        loginButton.setEnabled(true);
                        
                        if (!isVisible()) {
                            setVisible(true);
                        }

                        return;
                    }


                    // =================================================
                    // Validate Offline Authorization
                    // =================================================

                    boolean authorized =
                            OfflineAuthorizationManager.isAuthorized(
                                    authorization.getEmployeeId(),
                                    deviceId
                            );


                    // =================================================
                    // Offline Authorization Valid
                    // =================================================

                    if (authorized) {

                        System.out.println(
                                "Offline authorization verified."
                        );

                        System.out.println(
                                "Offline Employee ID: "
                                        + authorization.getEmployeeId()
                        );

                        System.out.println(
                                "Offline Employee Code: "
                                        + authorization.getEmployeeCode()
                        );

                        System.out.println(
                                "Offline Device ID: "
                                        + authorization.getDeviceId()
                        );


                        // =============================================
                        // Create Offline Login Result
                        // =============================================

                        AgentLoginService.LoginResult offlineResult =
                                AgentLoginService.LoginResult.offlineSuccess(
                                        authorization.getEmployeeId(),
                                        authorization.getEmployeeCode(),
                                        authorization.getUsername(),
                                        authorization.getRefreshToken()
                                );


                        // =============================================
                        // Offline Login Successful
                        // =============================================

                        updateStatus(
                                "Offline login successful",
                                SUCCESS_GREEN,
                                SUCCESS_TEXT,
                                SUCCESS_BG,
                                SUCCESS_BORDER
                        );


                        // =============================================
                        // Continue to AgentApplication
                        // =============================================

                        if (onLoginSuccess != null) {

                            onLoginSuccess.accept(offlineResult);
                        }


                        // =================================================
                        // Offline Authorization Invalid / Expired
                        // =================================================

                    } else {

                        System.out.println(
                                "Offline authorization is invalid or expired."
                        );

                        updateStatus(
                                "Offline access expired or unauthorized",
                                ERROR_RED,
                                ERROR_RED,
                                ERROR_BG,
                                ERROR_BORDER
                        );

                        loginButton.setEnabled(true);
                        
                        if (!isVisible()) {
                            setVisible(true);
                        }
                    }


                    // =====================================================
                    // INVALID CREDENTIALS / OTHER SERVER ERROR
                    // =====================================================

                } else {

                    if (result != null
                            && result.isInvalidCredentials()) {

                        updateStatus(
                                "Invalid credentials",
                                ERROR_RED,
                                ERROR_RED,
                                ERROR_BG,
                                ERROR_BORDER
                        );

                    } else {

                        updateStatus(
                                "Unable to connect to EMS",
                                ERROR_RED,
                                ERROR_RED,
                                ERROR_BG,
                                ERROR_BORDER
                        );
                    }

                    loginButton.setEnabled(true);
                    
                    if (!isVisible()) {
                        setVisible(true);
                    }
                }
            });

        });

        loginThread.setDaemon(false);
        loginThread.start();
    }


    /**
     * Resets the existing LoginWindow so it can be reused after logout.
     * This does not create a new window and does not perform automatic login.
     */
    public void prepareForNewLogin() {
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(true);

            updateStatus(
                    "Ready to connect",
                    SUCCESS_GREEN,
                    SUCCESS_TEXT,
                    SUCCESS_BG,
                    SUCCESS_BORDER
            );

            if (passwordVisible) {
                passwordVisible = false;
                passwordField.setEchoChar('•');
                passwordToggle.setVisibleState(false);
            }

            usernameField.requestFocusInWindow();
        });
    }

    // =========================================================
    // Custom UI Components
    // =========================================================

    /**
     * Compact Rounded Input Box (Height: 38px)
     */
    private static class RoundedInputContainer extends JPanel {
        private boolean focused = false;
        private final int radius = 8;

        RoundedInputContainer(JComponent leftIcon, JComponent field, JComponent rightIcon) {
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setPreferredSize(new Dimension(324, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            if (leftIcon != null) {
                add(leftIcon, BorderLayout.WEST);
            }
            if (field != null) {
                add(field, BorderLayout.CENTER);
                field.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        focused = true;
                        repaint();
                    }
                    @Override
                    public void focusLost(FocusEvent e) {
                        focused = false;
                        repaint();
                    }
                });
            }
            if (rightIcon != null) {
                add(rightIcon, BorderLayout.EAST);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(1, 1, w - 2, h - 2, radius, radius);

            if (focused) {
                g2.setColor(new Color(37, 99, 235, 25));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, radius, radius);

                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);
            } else {
                g2.setColor(BORDER_INPUT);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Placeholder Text Field
     */
    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(null);
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
            setForeground(TEXT_DARK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setColor(TEXT_LIGHT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 0, y);
                g2.dispose();
            }
        }
    }

    /**
     * Placeholder Password Field
     */
    private static class PlaceholderPasswordField extends JPasswordField {
        private final String placeholder;

        PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(null);
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
            setForeground(TEXT_DARK);
            setEchoChar('•');
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setColor(TEXT_LIGHT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, 0, y);
                g2.dispose();
            }
        }
    }

    /**
     * Centered Eye Toggle Label
     */
    private static class EyeToggleLabel extends JLabel {
        private boolean isVisible = false;

        EyeToggleLabel() {
            setPreferredSize(new Dimension(22, 38));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setToolTipText("Show password");
        }

        void setVisibleState(boolean isVisible) {
            this.isVisible = isVisible;
            setToolTipText(isVisible ? "Hide password" : "Show password");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_LIGHT);
            g2.setStroke(new BasicStroke(1.3f));

            int w = 16;
            int h = 10;
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            // Eye outline
            Path2D eye = new Path2D.Double();
            eye.moveTo(x, y + 5);
            eye.curveTo(x + 3, y, x + 13, y, x + 16, y + 5);
            eye.curveTo(x + 13, y + 10, x + 3, y + 10, x, y + 5);
            g2.draw(eye);

            // Pupil
            g2.fillOval(x + 6, y + 3, 4, 4);

            // Diagonal slash when masked
            if (!isVisible) {
                g2.drawLine(x - 1, y + 11, x + 17, y - 1);
            }

            g2.dispose();
        }
    }

    /**
     * Modern CheckBox
     */
    private static class ModernCheckBox extends JCheckBox {
        ModernCheckBox(String text) {
            super(text);
            setFont(new Font(FONT_FAMILY, Font.PLAIN, 12));
            setForeground(new Color(71, 85, 105));
            setOpaque(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setIcon(new CheckIcon(false));
            setSelectedIcon(new CheckIcon(true));
        }

        private static class CheckIcon implements Icon {
            private final boolean checked;
            CheckIcon(boolean checked) { this.checked = checked; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int boxSize = 13;
                int cy = y + (getIconHeight() - boxSize) / 2;

                if (checked) {
                    g2.setColor(PRIMARY);
                    g2.fillRoundRect(x, cy, boxSize, boxSize, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 3, cy + 6, x + 5, cy + 9);
                    g2.drawLine(x + 5, cy + 9, x + 10, cy + 3);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(x, cy, boxSize, boxSize, 3, 3);
                    g2.setColor(BORDER_INPUT);
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(x, cy, boxSize, boxSize, 3, 3);
                }
                g2.dispose();
            }

            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
        }
    }

    /**
     * Compact Modern Button
     */
    private static class ModernButton extends JButton {
        private boolean hover = false;
        private boolean pressed = false;

        ModernButton(String text) {
            super(text);
            setFont(new Font(FONT_FAMILY, Font.BOLD, 13));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color btnColor = !isEnabled() ? new Color(148, 163, 184)
                    : pressed ? PRIMARY_ACTIVE
                    : hover ? PRIMARY_HOVER : PRIMARY;

            g2.setColor(btnColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    /**
     * Rounded Card Panel
     */
    private static class RoundedCard extends JPanel {
        private final Color bgColor;
        private final int radius;

        RoundedCard(Color bgColor, int radius) {
            this.bgColor = bgColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth() - 1;
            int h = getHeight() - 1;

            // Soft Shadow
            g2.setColor(new Color(15, 23, 42, 10));
            g2.fillRoundRect(2, 3, w - 2, h - 2, radius, radius);

            // Card Body
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, w, h, radius, radius);

            // 1px Border
            g2.setColor(BORDER_LIGHT);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w, h, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * Vector Illustration: Laptop, Dashboard & Plant
     */
    private static class IllustrationPanel extends JPanel {
        IllustrationPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Shadow
            g2.setColor(new Color(15, 23, 42, 22));
            g2.fillOval(30, 142, 230, 24);
            g2.fillOval(252, 148, 65, 18);

            // Laptop Base
            GradientPaint baseGradient = new GradientPaint(
                    30, 130, new Color(203, 213, 225),
                    30, 145, new Color(148, 163, 184)
            );
            g2.setPaint(baseGradient);
            Polygon base = new Polygon(
                    new int[]{42, 240, 255, 25},
                    new int[]{124, 124, 144, 144},
                    4
            );
            g2.fill(base);

            // Front edge shine
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(25, 142, 230, 4, 3, 3);

            // Trackpad
            g2.setColor(new Color(203, 213, 225));
            g2.fillRoundRect(118, 132, 45, 9, 2, 2);

            // Screen Frame
            g2.setColor(NAVY);
            g2.fillRoundRect(50, 24, 185, 105, 10, 10);

            // Display
            g2.setColor(new Color(248, 250, 252));
            g2.fillRoundRect(56, 30, 173, 93, 6, 6);

            // Screen Content: Avatar
            g2.setColor(new Color(203, 213, 225));
            g2.fillOval(68, 42, 22, 22);
            g2.setColor(new Color(148, 163, 184));
            g2.fillOval(74, 46, 10, 10);

            // Screen Content: Skeleton Bars
            g2.setColor(new Color(226, 232, 240));
            g2.fillRoundRect(66, 72, 36, 4, 2, 2);
            g2.fillRoundRect(66, 80, 26, 4, 2, 2);
            g2.fillRoundRect(66, 92, 44, 18, 4, 4);

            // Screen Content: Chart Bars
            g2.setColor(new Color(59, 130, 246));
            g2.fillRoundRect(128, 76, 7, 18, 2, 2);
            g2.fillRoundRect(139, 68, 7, 26, 2, 2);
            g2.fillRoundRect(150, 60, 7, 34, 2, 2);
            g2.fillRoundRect(161, 52, 7, 42, 2, 2);
            g2.fillRoundRect(172, 44, 7, 50, 2, 2);

            // Screen Content: Mini card
            g2.setColor(new Color(226, 232, 240));
            g2.fillRoundRect(126, 100, 56, 14, 3, 3);

            // Ceramic Pot
            GradientPaint potGrad = new GradientPaint(
                    260, 100, Color.WHITE,
                    295, 150, new Color(226, 232, 240)
            );
            g2.setPaint(potGrad);
            g2.fillRoundRect(260, 96, 50, 56, 16, 16);

            // Soil
            g2.setColor(new Color(51, 65, 85));
            g2.fillOval(264, 93, 42, 10);

            // Leaves
            drawLeaf(g2, 276, 96, 250, 48, new Color(22, 163, 74));
            drawLeaf(g2, 282, 94, 272, 28, new Color(34, 197, 94));
            drawLeaf(g2, 288, 96, 316, 42, new Color(21, 128, 61));
            drawLeaf(g2, 284, 97, 298, 62, new Color(74, 222, 128));

            g2.dispose();
        }

        private void drawLeaf(Graphics2D g2, int startX, int startY, int tipX, int tipY, Color color) {
            g2.setColor(color);
            Path2D leaf = new Path2D.Double();
            leaf.moveTo(startX, startY);
            leaf.quadTo((startX + tipX) / 2 - 12, (startY + tipY) / 2, tipX, tipY);
            leaf.quadTo((startX + tipX) / 2 + 12, (startY + tipY) / 2, startX, startY);
            leaf.closePath();
            g2.fill(leaf);
        }
    }



}

