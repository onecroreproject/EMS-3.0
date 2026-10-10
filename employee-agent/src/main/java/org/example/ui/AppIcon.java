package org.example.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized app icon loader.
 *
 * Loads the icon from the classpath resource and provides
 * multiple sizes so the OS (Windows taskbar, Alt+Tab switcher)
 * always picks the sharpest available size.
 *
 * Usage:
 *   AppIcon.apply(myJFrame);
 */
public final class AppIcon {

    private AppIcon() {}

    private static final String RESOURCE_PATH = "/app-icon.png";

    /** Sizes requested by Windows for taskbar / Alt+Tab / System-tray. */
    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    /** Cached icon list – loaded once. */
    private static List<Image> iconImages = null;

    /**
     * Apply the app icon to a JFrame (taskbar, Alt+Tab, window title-bar).
     */
    public static void apply(JFrame frame) {
        List<Image> icons = getIcons();
        if (icons != null && !icons.isEmpty()) {
            frame.setIconImages(icons);
        }
    }

    /**
     * Return a single 32×32 Image suitable for a system-tray TrayIcon.
     * Returns null if the icon could not be loaded.
     */
    public static Image getTrayImage() {
        List<Image> icons = getIcons();
        if (icons == null || icons.isEmpty()) return null;
        // Find closest to 32
        for (Image img : icons) {
            if (img.getWidth(null) == 32) return img;
        }
        return icons.get(0);
    }

    private static synchronized List<Image> getIcons() {
        if (iconImages != null) return iconImages;
        try {
            InputStream is = AppIcon.class.getResourceAsStream(RESOURCE_PATH);
            if (is == null) {
                System.err.println("AppIcon: resource not found: " + RESOURCE_PATH);
                return null;
            }
            Image base = ImageIO.read(is);
            is.close();

            List<Image> list = new ArrayList<>();
            for (int size : SIZES) {
                list.add(base.getScaledInstance(size, size, Image.SCALE_SMOOTH));
            }
            iconImages = list;
            System.out.println("AppIcon: icon loaded successfully (" + SIZES.length + " sizes).");
        } catch (Exception e) {
            System.err.println("AppIcon: failed to load icon – " + e.getMessage());
        }
        return iconImages;
    }
}
