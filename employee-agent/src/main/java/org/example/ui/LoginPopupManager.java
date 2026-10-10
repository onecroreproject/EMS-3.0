package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * =========================================================
 * LOGIN POPUP MANAGER
 * =========================================================
 *
 * Controls the EXISTING LoginWindow.
 *
 * IMPORTANT:
 * - Never creates another LoginWindow.
 * - No continuous polling.
 * - No 500ms foreground forcing.
 * - Uses event-based application switching.
 *
 * Behavior:
 *
 * 1. Agent starts.
 * 2. Existing LoginWindow is displayed.
 * 3. After 1 minute, remind employee to login.
 * 4. If employee minimizes LoginWindow, arm the next
 *    application-switch reminder.
 * 5. When employee switches to another application,
 *    bring LoginWindow back ONCE.
 * 6. Employee can minimize again and the next switch
 *    will trigger another reminder.
 * 7. After successful login, everything stops.
 */
public class LoginPopupManager {

    // =========================================================
    // CONFIGURATION
    // =========================================================

    private int reminderIntervalMs = 60_000;

    public void setReminderIntervalMs(int ms) {
        this.reminderIntervalMs = ms;
    }

    /**
     * Small guard period used while bringing the window
     * to the foreground.
     */
    private static final int RESTORE_GUARD_MS = 800;


    // =========================================================
    // FIELDS
    // =========================================================

    /**
     * The EXISTING LoginWindow.
     *
     * We NEVER create a new LoginWindow.
     */
    private LoginWindow loginWindow;

    /**
     * 1-minute reminder timer.
     */
    private Timer reminderTimer;

    /**
     * Manager active state.
     */
    private volatile boolean running = false;

    /**
     * True while our own foreground operation is running.
     *
     * Prevents our own toFront()/requestFocus() operation
     * from triggering another reminder.
     */
    private volatile boolean restoringFocus = false;

    /**
     * Indicates that the next application focus loss
     * should trigger a reminder.
     *
     * IMPORTANT:
     *
     * This is NOT a continuous trigger.
     *
     * After one popup it becomes false.
     *
     * It becomes true again only when the employee
     * minimizes the LoginWindow.
     */
    private volatile boolean applicationSwitchArmed = true;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public LoginPopupManager(String deviceId) {

        /*
         * deviceId is intentionally not used.
         *
         * This manager only controls the existing
         * LoginWindow.
         */
    }


    // =========================================================
    // REGISTER EXISTING LOGIN WINDOW
    // =========================================================

    public synchronized void setLoginWindow(
            LoginWindow loginWindow
    ) {

        this.loginWindow = loginWindow;

        if (loginWindow == null) {
            return;
        }


        // =====================================================
        // MINIMIZE EVENT
        // =====================================================

        /*
         * IMPORTANT:
         *
         * DO NOT bring the window back immediately.
         *
         * The employee intentionally minimized the
         * LoginWindow.
         *
         * We only ARM the next application-switch reminder.
         *
         * Example:
         *
         * Minimize
         *      ↓
         * Open VS Code
         *      ↓
         * LoginWindow comes back
         */
        loginWindow.addWindowListener(
                new WindowAdapter() {

                    @Override
                    public void windowIconified(
                            WindowEvent e
                    ) {

                        if (!running) {
                            return;
                        }

                        /*
                         * Arm ONE application-switch reminder.
                         */
                        applicationSwitchArmed = true;

                        System.out.println(
                                "LOGIN REMINDER: "
                                        + "LoginWindow minimized. "
                                        + "Waiting for application switch."
                        );
                    }
                }
        );


        // =====================================================
        // APPLICATION SWITCH / FOCUS EVENT
        // =====================================================

        /*
         * Detect when the LoginWindow loses focus.
         *
         * There is NO polling timer.
         */
        loginWindow.addWindowFocusListener(
                new WindowFocusListener() {

                    @Override
                    public void windowGainedFocus(
                            WindowEvent e
                    ) {

                        // Nothing required.
                    }


                    @Override
                    public void windowLostFocus(
                            WindowEvent e
                    ) {

                        if (!running) {
                            return;
                        }


                        /*
                         * Ignore focus changes generated by
                         * our own foreground operation.
                         */
                        if (restoringFocus) {
                            return;
                        }


                        /*
                         * If application-switch reminder is
                         * not armed, do nothing.
                         *
                         * This is the main protection against
                         * continuous popup behavior.
                         */
                        if (!applicationSwitchArmed) {
                            return;
                        }


                        /*
                         * Consume the reminder BEFORE bringing
                         * the window forward.
                         *
                         * This prevents:
                         *
                         * focus lost
                         *      ↓
                         * popup
                         *      ↓
                         * focus lost
                         *      ↓
                         * popup
                         *      ↓
                         * LOOP
                         */
                        applicationSwitchArmed = false;


                        bringLoginWindowToFront(
                                "Employee switched application"
                        );
                    }
                }
        );
    }


    // =========================================================
    // START
    // =========================================================

    public synchronized void start() {

        if (running) {
            return;
        }


        if (loginWindow == null) {

            System.out.println(
                    "ERROR: LoginWindow was not registered."
            );

            return;
        }


        running = true;


        /*
         * At application startup, allow the first
         * application-switch reminder.
         */
        applicationSwitchArmed = true;


        // =====================================================
        // 1-MINUTE LOGIN REMINDER
        // =====================================================

        reminderTimer =
                new Timer(
                        reminderIntervalMs,
                        e -> {

                            if (!running) {
                                return;
                            }


                            /*
                             * The 1-minute reminder is independent
                             * of the application-switch reminder.
                             *
                             * We consume the current focus reminder
                             * so the foreground operation itself
                             * cannot cause another popup.
                             */
                            applicationSwitchArmed = false;


                            System.out.println(
                                    "================================="
                            );

                            System.out.println(
                                    "LOGIN REMINDER"
                            );

                            System.out.println(
                                    "Employee has not logged in "
                                            + "for 1 minute."
                            );

                            System.out.println(
                                    "Bringing existing LoginWindow "
                                            + "to foreground."
                            );

                            System.out.println(
                                    "================================="
                            );


                            bringLoginWindowToFront(
                                    "1-minute login reminder"
                            );
                        }
                );


        /*
         * Repeat every minute until successful login.
         */
        reminderTimer.setRepeats(true);

        reminderTimer.start();


        // =====================================================
        // START LOG
        // =====================================================

        System.out.println(
                "================================="
        );

        System.out.println(
                "LOGIN REMINDER STARTED"
        );

        System.out.println(
                "Login reminder interval: 1 minute"
        );

        System.out.println(
                "LoginWindow monitoring: EVENT-BASED"
        );

        System.out.println(
                "Continuous foreground polling: DISABLED"
        );

        System.out.println(
                "Minimize event: ARM ONLY"
        );

        System.out.println(
                "Application switch: ONE-TIME POPUP"
        );

        System.out.println(
                "Only ONE LoginWindow is used."
        );

        System.out.println(
                "Reminder stops after successful login."
        );

        System.out.println(
                "================================="
        );
    }


    // =========================================================
    // BRING LOGIN WINDOW TO FRONT
    // =========================================================

    private void bringLoginWindowToFront(
            String reason
    ) {

        if (!running) {
            return;
        }


        if (loginWindow == null) {
            return;
        }


        /*
         * Do not start another foreground operation while
         * one is already running.
         */
        if (restoringFocus) {
            return;
        }


        restoringFocus = true;


        SwingUtilities.invokeLater(() -> {

            try {

                if (!running) {
                    restoringFocus = false;
                    return;
                }


                if (loginWindow == null) {
                    restoringFocus = false;
                    return;
                }


                if (!loginWindow.isDisplayable()) {
                    restoringFocus = false;
                    return;
                }


                // =============================================
                // RESTORE IF MINIMIZED
                // =============================================

                if (loginWindow.getState()
                        == Frame.ICONIFIED) {

                    loginWindow.setState(
                            Frame.NORMAL
                    );
                }


                // =============================================
                // MAKE SURE WINDOW IS VISIBLE
                // =============================================

                if (!loginWindow.isVisible()) {

                    loginWindow.setVisible(true);
                }


                // =============================================
                // BRING EXISTING WINDOW TO FRONT
                // =============================================

                /*
                 * Temporarily use AlwaysOnTop to make sure
                 * Windows brings the existing LoginWindow
                 * forward.
                 */
                loginWindow.setAlwaysOnTop(true);

                loginWindow.toFront();

                loginWindow.requestFocus();

                loginWindow.requestFocusInWindow();


                // =============================================
                // LOG
                // =============================================

                System.out.println(
                        "LOGIN REMINDER: Existing LoginWindow "
                                + "brought to foreground. Reason: "
                                + reason
                );


                // =============================================
                // RELEASE FOREGROUND GUARD
                // =============================================

                Timer releaseTimer =
                        new Timer(
                                RESTORE_GUARD_MS,
                                event -> {

                                    if (loginWindow != null) {

                                        loginWindow
                                                .setAlwaysOnTop(false);
                                    }


                                    restoringFocus = false;
                                }
                        );


                releaseTimer.setRepeats(false);

                releaseTimer.start();


            } catch (Exception ex) {

                /*
                 * Always release the guard if something
                 * unexpected happens.
                 */
                restoringFocus = false;


                System.err.println(
                        "ERROR: Unable to bring LoginWindow "
                                + "to foreground: "
                                + ex.getMessage()
                );
            }
        });
    }


    // =========================================================
    // STOP
    // =========================================================

    public synchronized void stop() {

        if (!running) {
            return;
        }


        /*
         * IMPORTANT:
         *
         * Once login succeeds, ALL login reminder behavior
         * is disabled.
         */
        running = false;


        // =====================================================
        // STOP 1-MINUTE TIMER
        // =====================================================

        if (reminderTimer != null) {

            reminderTimer.stop();

            reminderTimer = null;
        }


        // =====================================================
        // DISABLE APPLICATION SWITCH REMINDER
        // =====================================================

        applicationSwitchArmed = false;


        // =====================================================
        // RESET FOREGROUND GUARD
        // =====================================================

        restoringFocus = false;


        // =====================================================
        // RELEASE ALWAYS-ON-TOP
        // =====================================================

        if (loginWindow != null) {

            loginWindow.setAlwaysOnTop(false);
        }


        // =====================================================
        // LOG
        // =====================================================

        System.out.println(
                "================================="
        );

        System.out.println(
                "LOGIN REMINDER STOPPED"
        );

        System.out.println(
                "Employee successfully logged in."
        );

        System.out.println(
                "1-minute reminder stopped."
        );

        System.out.println(
                "Application switch reminder stopped."
        );

        System.out.println(
                "No more login reminders."
        );

        System.out.println(
                "================================="
        );
    }


    // =========================================================
    // STATUS
    // =========================================================

    public boolean isRunning() {

        return running;
    }
}