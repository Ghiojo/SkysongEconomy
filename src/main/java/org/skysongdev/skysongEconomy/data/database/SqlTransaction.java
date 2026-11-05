package org.skysongdev.skysongEconomy.data.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Runs a group of statements as one transaction: all of them are saved, or none. */
final class SqlTransaction {
    /** Statements to run inside the transaction. */
    @FunctionalInterface
    interface Work {
        void run(Connection connection) throws SQLException;
    }

    private SqlTransaction() {
    }

    /**
     * @param failure what to say when it fails, e.g. {@code "Failed to save businesses"}
     * @throws StorageException when a statement fails; nothing is saved then
     */
    static void run(ConnectionProvider connections, String failure, Work work) {
        try {
            Connection connection = connections.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new StorageException(failure, e);
        }
    }
}
