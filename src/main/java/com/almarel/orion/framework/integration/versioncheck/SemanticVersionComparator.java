package com.almarel.orion.framework.integration.versioncheck;

/**
 * Stateless SemVer comparison utility (e.g. "0.1.0" vs "0.2.0").
 * Intentionally NOT a Guice-managed service — no state, no dependencies, no side effects.
 */
public final class SemanticVersionComparator {

    private SemanticVersionComparator() {
    }

    /**
     * @return true if remoteVersion is strictly newer than localVersion.
     */
    public static boolean isNewer(String localVersion, String remoteVersion) {
        if (localVersion == null || remoteVersion == null) {
            return false;
        }

        int[] local = parse(localVersion);
        int[] remote = parse(remoteVersion);

        for (int i = 0; i < 3; i++) {
            if (remote[i] != local[i]) {
                return remote[i] > local[i];
            }
        }
        return false;
    }

    /**
     * @return true if localVersion is older than minSupportedVersion (critically outdated).
     */
    public static boolean isBelowMinimum(String localVersion, String minSupportedVersion) {
        return isNewer(localVersion, minSupportedVersion);
    }

    private static int[] parse(String version) {
        String[] parts = version.trim().split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            result[i] = safeParse(parts[i]);
        }
        return result;
    }

    private static int safeParse(String segment) {
        try {
            return Integer.parseInt(segment.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}