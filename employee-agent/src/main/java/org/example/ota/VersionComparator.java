package org.example.ota;

public final class VersionComparator {

    private VersionComparator() {
    }

    /**
     * Returns:
     * -1 if current < latest
     *  0 if current == latest
     *  1 if current > latest
     */
    public static int compare(
            String currentVersion,
            String latestVersion) {

        if (currentVersion == null || latestVersion == null) {
            throw new IllegalArgumentException(
                    "Version cannot be null"
            );
        }

        String[] current =
                currentVersion.trim().split("\\.");

        String[] latest =
                latestVersion.trim().split("\\.");

        int length =
                Math.max(
                        current.length,
                        latest.length
                );

        for (int i = 0; i < length; i++) {

            int currentPart =
                    i < current.length
                            ? parsePart(current[i])
                            : 0;

            int latestPart =
                    i < latest.length
                            ? parsePart(latest[i])
                            : 0;

            if (currentPart < latestPart) {
                return -1;
            }

            if (currentPart > latestPart) {
                return 1;
            }
        }

        return 0;
    }

    private static int parsePart(String value) {

        try {

            return Integer.parseInt(
                    value.trim()
            );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid version component: "
                            + value,
                    e
            );
        }
    }
}