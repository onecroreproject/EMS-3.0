package org.example.activity;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class UserActivityMonitor {

    private interface Kernel32 extends Library {

        Kernel32 INSTANCE =
                Native.load("kernel32", Kernel32.class);

        int GetTickCount();
    }

    private interface User32 extends Library {

        User32 INSTANCE =
                Native.load("user32", User32.class);

        boolean GetLastInputInfo(LASTINPUTINFO result);
    }

    public static class LASTINPUTINFO
            extends Structure {

        public int cbSize;
        public int dwTime;

        @Override
        protected List<String> getFieldOrder() {

            return Arrays.asList(
                    "cbSize",
                    "dwTime"
            );
        }
    }

    public UserActivityMonitor() {

        System.out.println(
                "System-wide activity monitor started."
        );
    }

    public long getInactiveDurationSeconds() {

        LASTINPUTINFO lastInputInfo =
                new LASTINPUTINFO();

        lastInputInfo.cbSize =
                lastInputInfo.size();

        boolean success =
                User32.INSTANCE.GetLastInputInfo(
                        lastInputInfo
                );

        if (!success) {

            System.out.println(
                    "Unable to read Windows last input information."
            );

            return 0;
        }

        long currentTick =
                Integer.toUnsignedLong(
                        Kernel32.INSTANCE.GetTickCount()
                );

        long lastInputTick =
                Integer.toUnsignedLong(
                        lastInputInfo.dwTime
                );

        long idleMilliseconds =
                currentTick - lastInputTick;

        if (idleMilliseconds < 0) {

            idleMilliseconds +=
                    0x1_0000_0000L;
        }

        return idleMilliseconds / 1000;
    }

    public boolean hasRecentActivity() {

        return getInactiveDurationSeconds() < 1;
    }
}