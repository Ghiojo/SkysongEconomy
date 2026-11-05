package org.skysongdev.skysongEconomy.data.database;

/** The database refused a read or a write. Wraps the {@link java.sql.SQLException} that caused it. */
public final class StorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
