package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AppTrayManager {

    private static TrayIcon trayIcon;

    public static void initialize(JFrame mainWindow) {
        if (!SystemTray.isSupported()) {
            System.err.println("SystemTray is not supported on this OS.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = AppIcon.getTrayImage();
        if (image == null) {
            System.err.println("Could not load tray image.");
            return;
        }

        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Open Dashboard");
        openItem.addActionListener(e -> {
            mainWindow.setVisible(true);
            mainWindow.setExtendedState(JFrame.NORMAL);
            mainWindow.toFront();
        });

        MenuItem logoutItem = new MenuItem("Log out");
        logoutItem.addActionListener(e -> {
            mainWindow.setVisible(true);
            mainWindow.setExtendedState(JFrame.NORMAL);
            mainWindow.toFront();
            // Call the logout logic on the main window.
            if (mainWindow instanceof EmployeeWorkspaceWindow) {
                ((EmployeeWorkspaceWindow) mainWindow).handleLogoutExternal();
            }
        });

        MenuItem exitItem = new MenuItem("Exit Agent");
        exitItem.addActionListener(e -> {
            System.exit(0);
        });

        popup.add(openItem);
        popup.add(logoutItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "Tratime", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 1) {
                    mainWindow.setVisible(true);
                    mainWindow.setExtendedState(JFrame.NORMAL);
                    mainWindow.toFront();
                }
            }
        });

        try {
            tray.add(trayIcon);
            System.out.println("System Tray initialized successfully.");
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added: " + e.getMessage());
        }
    }
}
