package org.example.ui.theme;

import java.awt.Color;
import java.awt.GraphicsEnvironment;

public class UITheme {

    public static final Color NAVY_BANNER = new Color(0, 93, 158);
    public static final Color BG_CANVAS = new Color(238, 244, 250);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color TEXT_DARK = new Color(15, 23, 42);
    public static final Color TEXT_BODY = new Color(51, 65, 85);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);
    public static final Color BORDER_CARD = new Color(226, 232, 240);
    public static final Color BORDER_INPUT = new Color(203, 213, 225);
    public static final Color PRIMARY_BLUE = new Color(2, 132, 199);
    public static final Color DANGER_RED = new Color(220, 38, 38);
    public static final Color DANGER_RED_HOVER = new Color(185, 28, 28);
    public static final Color SUCCESS_GREEN = new Color(22, 163, 74);
    public static final Color WARNING_ORANGE = new Color(234, 88, 12);
    public static final Color PURPLE_TEXT = new Color(124, 58, 237);

    public static final Color MINI_GREEN_BG = new Color(236, 253, 245);
    public static final Color MINI_GREEN_BORDER = new Color(187, 247, 208);
    public static final Color MINI_BLUE_BG = new Color(239, 246, 255);
    public static final Color MINI_BLUE_BORDER = new Color(191, 219, 254);
    public static final Color MINI_ORANGE_BG = new Color(255, 251, 235);
    public static final Color MINI_ORANGE_BORDER = new Color(254, 230, 138);
    public static final Color MINI_PURPLE_BG = new Color(245, 243, 255);
    public static final Color MINI_PURPLE_BORDER = new Color(221, 214, 254);
    public static final Color MINI_ROSE_BG = new Color(255, 241, 242);
    public static final Color MINI_ROSE_BORDER = new Color(254, 205, 211);

    public static final Color BREAK_BG = new Color(240, 248, 255);
    public static final Color BREAK_BORDER = new Color(186, 224, 247);
    public static final Color LUNCH_BG = new Color(248, 244, 255);
    public static final Color LUNCH_BORDER = new Color(224, 203, 254);

    public static final String FONT_FAMILY = getApplicationFont();

    private static String getApplicationFont() {
        String[] fonts = {
                "Segoe UI", "Public Sans", "Inter", "Roboto", "Helvetica Neue", "Arial"
        };
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
}
