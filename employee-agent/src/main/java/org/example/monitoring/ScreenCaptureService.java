package org.example.monitoring;

import org.example.attendance.AttendanceEventManager;
import org.example.attendance.AttendanceState;
import org.example.config.AgentConfig;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

public class ScreenCaptureService {

    private final AgentConfig config;
    private final String serverUrl;
    private final String employeeId;
    private final AttendanceEventManager eventManager;
    private volatile boolean running = true;
    private Thread captureThread;

    public ScreenCaptureService(
            AgentConfig config,
            String serverUrl,
            String employeeId,
            AttendanceEventManager eventManager
    ) {
        this.config = config;
        this.serverUrl = serverUrl;
        this.employeeId = employeeId;
        this.eventManager = eventManager;
    }

    public synchronized void start() {
        if (captureThread != null && captureThread.isAlive()) {
            return;
        }
        running = true;
        captureThread = new Thread(this::runCaptureLoop, "ScreenCapture-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
        System.out.println("Dynamic ScreenCaptureService started. Interval: " + config.getCaptureIntervalMinutes() + " min, Quality: " + config.getImageQuality() + "%");
    }

    public synchronized void stop() {
        running = false;
        if (captureThread != null) {
            captureThread.interrupt();
        }
    }

    private void runCaptureLoop() {
        int elapsedSeconds = 0;

        while (running) {
            try {
                Thread.sleep(1000);
                elapsedSeconds++;

                // Dynamically read capture interval from config on every tick!
                int requiredIntervalSeconds = Math.max(1, config.getCaptureIntervalMinutes()) * 60;

                if (elapsedSeconds >= requiredIntervalSeconds) {
                    elapsedSeconds = 0; // Reset timer

                    // Check if capture is enabled dynamically
                    if (!config.isCaptureEnabled()) {
                        continue;
                    }

                    // Check current attendance state
                    AttendanceState state = eventManager != null ? eventManager.getCurrentState() : AttendanceState.WORKING;
                    boolean shouldCapture = false;

                    if (state == AttendanceState.WORKING && config.isCaptureDuringWorking()) {
                        shouldCapture = true;
                    } else if (state == AttendanceState.IDLE && config.isCaptureDuringIdle()) {
                        shouldCapture = true;
                    } else if (state == AttendanceState.BREAK && config.isCaptureDuringBreak()) {
                        shouldCapture = true;
                    } else if (config.isCaptureWhenLoggedOut()) {
                        shouldCapture = true;
                    }

                    if (shouldCapture) {
                        captureAndUpload();
                    }
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception ex) {
                System.err.println("Screen capture tick error: " + ex.getMessage());
            }
        }
    }

    private void captureAndUpload() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screenRectangle = new Rectangle(screenSize);
            Robot robot = new Robot();
            BufferedImage image = robot.createScreenCapture(screenRectangle);

            String format = config.getImageFormat();
            if (format == null || format.trim().isEmpty()) {
                format = "JPEG";
            }
            format = format.toUpperCase().contains("PNG") ? "PNG" : "JPEG";

            byte[] imageBytes = compressImage(image, format, config.getImageQuality());

            boolean uploaded = uploadScreenshot(imageBytes, format);
            if (!uploaded && config.isEnableOfflineCapture()) {
                saveOfflineScreenshot(imageBytes, format);
            }
        } catch (Throwable t) {
            System.err.println("Failed to capture screenshot: " + t.getMessage());
        }
    }

    private byte[] compressImage(BufferedImage image, String format, int qualityPercent) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if ("PNG".equalsIgnoreCase(format)) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }

        // JPEG compression
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                float quality = Math.max(0.1f, Math.min(1.0f, qualityPercent / 100.0f));
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private boolean uploadScreenshot(byte[] imageBytes, String format) {
        String boundary = "===" + UUID.randomUUID().toString() + "===";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            URL url = new URL(serverUrl + "/api/agent/screenshot/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

                // Parameter: employeeId
                writer.append(twoHyphens).append(boundary).append(lineEnd);
                writer.append("Content-Disposition: form-data; name=\"employeeId\"").append(lineEnd);
                writer.append(lineEnd);
                writer.append(employeeId != null ? employeeId : "UNKNOWN").append(lineEnd);
                writer.flush();

                // Parameter: file
                String ext = "PNG".equalsIgnoreCase(format) ? "png" : "jpg";
                String mime = "PNG".equalsIgnoreCase(format) ? "image/png" : "image/jpeg";
                writer.append(twoHyphens).append(boundary).append(lineEnd);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"screenshot_" + System.currentTimeMillis() + "." + ext + "\"").append(lineEnd);
                writer.append("Content-Type: ").append(mime).append(lineEnd);
                writer.append(lineEnd);
                writer.flush();

                outputStream.write(imageBytes);
                outputStream.flush();

                writer.append(lineEnd);
                writer.flush();

                // End boundary
                writer.append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd);
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                System.out.println("Screenshot uploaded successfully (" + (imageBytes.length / 1024) + " KB).");
                return true;
            } else {
                System.err.println("Screenshot upload returned status: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            System.err.println("Screenshot upload failed: " + e.getMessage());
            return false;
        }
    }

    private void saveOfflineScreenshot(byte[] imageBytes, String format) {
        try {
            Path offlineDir = Paths.get("C:\\ProgramData\\EmployeeAgent\\offline_screenshots");
            Files.createDirectories(offlineDir);

            // Limit check (in GB)
            long limitBytes = (long) config.getLocalStorageLimitGb() * 1024L * 1024L * 1024L;
            long currentUsage = 0;
            File[] files = offlineDir.toFile().listFiles();
            if (files != null) {
                for (File f : files) {
                    currentUsage += f.length();
                }
            }

            if (currentUsage + imageBytes.length > limitBytes) {
                System.out.println("Offline screenshot storage limit reached (" + config.getLocalStorageLimitGb() + " GB). Skipping local cache.");
                return;
            }

            String ext = "PNG".equalsIgnoreCase(format) ? "png" : "jpg";
            Path filePath = offlineDir.resolve("shot_" + System.currentTimeMillis() + "." + ext);
            Files.write(filePath, imageBytes);
            System.out.println("Saved screenshot offline: " + filePath.getFileName());
        } catch (Exception e) {
            System.err.println("Failed to cache offline screenshot: " + e.getMessage());
        }
    }
}
