package org.skysongdev.skysongEconomy.integration.skysongnpcs;

/**
 * A plugin API's version, such as SkysongNPCs' {@code 0.23.0}, to check that the installed plugin
 * is new enough before calling methods an older one does not have.
 */
public record ApiVersion(int major, int minor, int patch) implements Comparable<ApiVersion> {
    /** What an unreadable version counts as: older than everything. */
    public static final ApiVersion UNKNOWN = new ApiVersion(0, 0, 0);

    /**
     * Reads {@code major.minor.patch}. Missing parts count as 0 and anything after the numbers,
     * such as {@code -SNAPSHOT}, is ignored.
     *
     * @return the version, {@link #UNKNOWN} when the text does not start with a number
     */
    public static ApiVersion parse(String text) {
        int[] parts = new int[3];
        String[] written = text.trim().split("\\.", 4);
        for (int i = 0; i < parts.length && i < written.length; i++) {
            String digits = written[i].replaceFirst("^(\\d*).*$", "$1");
            if (digits.isEmpty() || digits.length() > 9) {
                if (i == 0) {
                    return UNKNOWN;
                }
                break;
            }
            parts[i] = Integer.parseInt(digits);
        }
        return new ApiVersion(parts[0], parts[1], parts[2]);
    }

    public boolean isAtLeast(ApiVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(ApiVersion other) {
        int byMajor = Integer.compare(major, other.major);
        if (byMajor != 0) {
            return byMajor;
        }
        int byMinor = Integer.compare(minor, other.minor);
        return byMinor != 0 ? byMinor : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
