package org.skysongdev.skysongEconomy.config;

/**
 * Something read from one {@code <id>.yml} file in a {@link YamlFolder}, such as a random table
 * or a farm type, and named by that file.
 */
public interface FileEntry {
    /** @return the file name without {@code .yml}, which other files and commands refer to */
    String id();
}
