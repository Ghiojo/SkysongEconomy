package org.skysongdev.skysongEconomy.data.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the MySQL connection shared by every repository. The connection is opened lazily and
 * reopened when MySQL has closed it (for example after an idle timeout).
 */
public final class ConnectionProvider implements AutoCloseable {
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DatabaseSettings settings;
    private final Logger logger;
    private Connection connection;

    public ConnectionProvider(DatabaseSettings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed() && connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
            return connection;
        }
        connection = DriverManager.getConnection(settings.jdbcUrl(), settings.user(), settings.password());
        logger.info("Connected to the database");
        return connection;
    }

    @Override
    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close the database connection", e);
        }
        connection = null;
    }
}
