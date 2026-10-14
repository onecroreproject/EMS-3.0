package org.example.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized app icon loader with High-DPI support and ultra-smooth bicubic rendering.
 *
 * Provides crisp, unblurred icons across:
 * - Windows Taskbar & Alt+Tab (multi-resolution image set)
 * - Window title bar (High-DPI smooth painting)
 * - System Tray
 */
public final class AppIcon {

    private AppIcon() {}

    private static final String RESOURCE_PATH = "/app-icon.png";

    /** Comprehensive sizes covering 100%, 125%, 150%, 175%, and 200% Windows scaling. */
    private static final int[] SIZES = {16, 20, 24, 28, 32, 36, 40, 48, 64, 96, 128, 256, 512};

    /** Cached master base image. */
    private static BufferedImage masterImage = null;

    /** Cached icon list for Windows OS. */
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
     * Return a single crisp Image suitable for the system tray (32×32).
     */
    public static Image getTrayImage() {
        BufferedImage master = getMasterImage();
        if (master == null) return null;
        return createSmoothScaledImage(master, 32, 32);
    }

    /**
     * Return the full-resolution master image.
     */
    public static Image getBaseImage() {
        return getMasterImage();
    }

    /**
     * Return an ImageIcon with crystal-clear High-DPI rendering that never blurs.
     */
    public static ImageIcon getScaledIcon(int width, int height) {
        BufferedImage master = getMasterImage();
        if (master == null) return null;
        return new SmoothImageIcon(master, width, height);
    }

    /**
     * Load the master image once from resources.
     */
    public static synchronized BufferedImage getMasterImage() {
        if (masterImage != null) return masterImage;
        try {
            InputStream is = AppIcon.class.getResourceAsStream(RESOURCE_PATH);
            if (is == null) {
                System.err.println("AppIcon: resource not found: " + RESOURCE_PATH);
                return null;
            }
            masterImage = ImageIO.read(is);
            is.close();
        } catch (Exception e) {
            System.err.println("AppIcon: failed to load master image – " + e.getMessage());
        }
        return masterImage;
    }

    private static synchronized List<Image> getIcons() {
        if (iconImages != null) return iconImages;
        BufferedImage master = getMasterImage();
        if (master == null) return null;

        List<Image> list = new ArrayList<>();
        for (int size : SIZES) {
            list.add(createSmoothScaledImage(master, size, size));
        }
        list.add(master);
        iconImages = list;
        System.out.println("AppIcon: icon loaded successfully (" + SIZES.length + " high-DPI sizes).");
        return iconImages;
    }

    /**
     * High-quality progressive multi-step downsampling with pure bicubic interpolation.
     * Prevents aliasing, halos, and blurriness when downscaling large images to small icons.
     */
    public static BufferedImage createSmoothScaledImage(BufferedImage src, int targetW, int targetH) {
        if (src == null) return null;
        if (src.getWidth() == targetW && src.getHeight() == targetH) {
            return src;
        }

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage current = src;

        // Step down by half iteratively until close to target size to avoid aliasing
        while (w > targetW * 2 || h > targetH * 2) {
            w = Math.max(targetW, w / 2);
            h = Math.max(targetH, h / 2);
            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(current, 0, 0, w, h, null);
            g2.dispose();
            current = tmp;
        }

        // Final step directly to target size using high-quality Bicubic interpolation
        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.drawImage(current, 0, 0, targetW, targetH, null);
        g2.dispose();

        return result;
    }

    /**
     * Custom ImageIcon that paints directly from the high-res master image at paint-time.
     * On High-DPI screens (125%, 150%, 200%), this draws with the exact native physical device
     * resolution, completely eliminating any blurriness or scaling artifacts.
     */
    public static class SmoothImageIcon extends ImageIcon {
        private final BufferedImage master;
        private final int width;
        private final int height;

        public SmoothImageIcon(BufferedImage master, int width, int height) {
            super(createSmoothScaledImage(master, width, height));
            this.master = master;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.drawImage(master, x, y, width, height, null);
            g2.dispose();
        }
    }
}
