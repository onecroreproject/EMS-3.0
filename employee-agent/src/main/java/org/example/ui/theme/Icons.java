package org.example.ui.theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Icons {

    public enum IconType {
        CALENDAR, CLOCK, TASK, DURATION, IDLE, BREAK, LUNCH, START, STOP, SETTINGS, LOGOUT
    }

    public static Icon createWorkspaceIcon(IconType type, Color color) {
        return new WorkspaceIcon(type, color, 17);
    }

    private static class WorkspaceIcon implements Icon {
        private final IconType type;
        private final Color color;
        private final int size;

        WorkspaceIcon(IconType type, Color color, int size) {
            this.type = type;
            this.color = color;
            this.size = size;
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = x + size / 2;
            int cy = y + size / 2;

            switch (type) {
                case CALENDAR -> {
                    g2.drawRoundRect(x + 3, y + 3, size - 6, size - 6, 2, 2);
                    g2.drawLine(x + 3, y + 7, x + size - 3, y + 7);
                    g2.drawLine(x + 6, y + 1, x + 6, y + 4);
                    g2.drawLine(x + size - 6, y + 1, x + size - 6, y + 4);
                    g2.fillRect(x + 5, y + 9, 2, 2);
                    g2.fillRect(x + 8, y + 9, 2, 2);
                    g2.fillRect(x + 11, y + 9, 2, 2);
                    g2.fillRect(x + 5, y + 12, 2, 2);
                    g2.fillRect(x + 8, y + 12, 2, 2);
                }
                case CLOCK -> {
                    g2.drawOval(x + 2, y + 2, size - 4, size - 4);
                    g2.drawLine(cx, cy, cx, y + 5);
                    g2.drawLine(cx, cy, cx + 3, cy + 3);
                    g2.fillOval(cx - 1, cy - 1, 2, 2);
                }
                case DURATION -> {
                    g2.drawOval(x + 2, y + 2, size - 4, size - 4);
                    g2.drawLine(cx, cy, cx, y + 5);
                    g2.drawLine(cx, cy, cx + 3, cy + 3);
                    g2.fillOval(cx - 1, cy - 1, 2, 2);
                }
                case TASK -> {
                    g2.drawRoundRect(x + 4, y + 3, size - 8, size - 5, 2, 2);
                    g2.drawRect(cx - 2, y + 1, 4, 2);
                    java.awt.geom.Path2D check = new java.awt.geom.Path2D.Double();
                    check.moveTo(x + 6, cy + 1);
                    check.lineTo(x + 8, cy + 3);
                    check.lineTo(x + 11, cy - 1);
                    g2.draw(check);
                    g2.drawLine(x + 6, cy + 5, x + 11, cy + 5);
                }
                case IDLE -> {
                    g2.drawLine(x + 5, y + 3, x + 12, y + 3); 
                    g2.drawLine(x + 5, y + 14, x + 12, y + 14);
                    java.awt.geom.Path2D glass = new java.awt.geom.Path2D.Double();
                    glass.moveTo(x + 6, y + 3);
                    glass.curveTo(x + 6, y + 7, cx - 1, cy - 1, cx, cy);
                    glass.curveTo(cx + 1, cy - 1, x + 11, y + 7, x + 11, y + 3);
                    glass.moveTo(x + 6, y + 14);
                    glass.curveTo(x + 6, y + 10, cx - 1, cy + 1, cx, cy);
                    glass.curveTo(cx + 1, cy + 1, x + 11, y + 10, x + 11, y + 14);
                    g2.draw(glass);
                    g2.fillOval(cx - 1, cy + 2, 2, 2); 
                }
                case BREAK -> {
                    java.awt.geom.Path2D cup = new java.awt.geom.Path2D.Double();
                    cup.moveTo(x + 4, y + 6);
                    cup.lineTo(x + 11, y + 6);
                    cup.curveTo(x + 11, y + 12, x + 4, y + 12, x + 4, y + 6);
                    g2.draw(cup);
                    g2.drawArc(x + 10, y + 6, 3, 4, -90, 180);
                    java.awt.geom.Path2D steam = new java.awt.geom.Path2D.Double();
                    steam.moveTo(x + 6, y + 4);
                    steam.curveTo(x + 4, y + 2, x + 8, y + 2, x + 6, y + 0);
                    steam.moveTo(x + 9, y + 4);
                    steam.curveTo(x + 7, y + 2, x + 11, y + 2, x + 9, y + 0);
                    g2.draw(steam);
                }
                case LUNCH -> {
                    g2.drawLine(x + 5, y + 3, x + 5, y + 7);
                    g2.drawLine(x + 7, y + 3, x + 7, y + 7);
                    g2.drawLine(x + 9, y + 3, x + 9, y + 7);
                    g2.drawArc(x + 5, y + 5, 4, 4, 180, 180);
                    g2.drawLine(x + 7, y + 7, x + 7, y + 14);
                    java.awt.geom.Path2D knife = new java.awt.geom.Path2D.Double();
                    knife.moveTo(x + 11, y + 14);
                    knife.lineTo(x + 11, y + 8);
                    knife.curveTo(x + 11, y + 3, x + 13, y + 3, x + 13, y + 8);
                    knife.lineTo(x + 13, y + 14);
                    knife.closePath();
                    g2.draw(knife);
                }
                case START -> {
                    g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                    java.awt.geom.Path2D play = new java.awt.geom.Path2D.Double();
                    play.moveTo(x + 7, y + 6);
                    play.lineTo(x + 12, cy);
                    play.lineTo(x + 7, y + size - 6);
                    play.closePath();
                    g2.fill(play);
                }
                case STOP -> {
                    g2.drawOval(x + 1, y + 1, size - 2, size - 2);
                    g2.fillRoundRect(x + 6, y + 6, size - 12, size - 12, 2, 2);
                }
                case SETTINGS -> {
                    g2.drawOval(cx - 3, cy - 3, 6, 6);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.PI * 2 * i / 6;
                        int x1 = (int) (cx + Math.cos(angle) * 4);
                        int y1 = (int) (cy + Math.sin(angle) * 4);
                        int x2 = (int) (cx + Math.cos(angle) * 7);
                        int y2 = (int) (cy + Math.sin(angle) * 7);
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                case LOGOUT -> {
                    g2.drawRoundRect(x + 5, y + 3, 7, size - 6, 2, 2);
                    g2.drawLine(cx + 1, cy, x + size - 2, cy);
                    g2.drawLine(x + size - 5, cy - 3, x + size - 2, cy);
                    g2.drawLine(x + size - 5, cy + 3, x + size - 2, cy);
                }
            }
            g2.dispose();
        }
    }
}
