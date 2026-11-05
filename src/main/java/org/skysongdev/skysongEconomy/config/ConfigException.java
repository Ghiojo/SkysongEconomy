package org.skysongdev.skysongEconomy.config;

/**
 * A config file holds a value the plugin can't use. The message names the file and the key, so
 * it can be shown to server owners as it is.
 */
public final class ConfigException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConfigException(String message) {
        super(message);
    }
}
