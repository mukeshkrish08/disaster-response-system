package drs.server.repository;

import drs.server.util.DrsConfiguration;
import drs.shared.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Per-operation JDBC connection factory. Every call to
 * {@link #getConnection()} returns a fresh {@link Connection} so multiple
 * server threads never share state.
 *
 * Repositories must use try-with-resources to ensure the connection
 * closes after the unit of work, regardless of success/failure.
 *
 * Example:
 * <pre>{@code
 *  try (Connection c = DatabaseConnection.getConnection();
 *       PreparedStatement ps = c.prepareStatement(sql)) {
 *      ...
 *  }
 * }</pre>
  
 */
public final class DatabaseConnection {

    private static volatile boolean driverLoaded;

    private DatabaseConnection() {
        // Static utility
    }

    /*   * Open a new JDBC connection against the configured disaster_response_system
     * database.
         * @return fresh Connection (caller must close)
     * @throws DataAccessException on driver load or connect failure
     */
    public static Connection getConnection() {
        ensureDriver();
        String url = "jdbc:mysql://"
                + DrsConfiguration.mysqlHost() + ":"
                + DrsConfiguration.mysqlPort() + "/"
                + DrsConfiguration.mysqlDatabase()
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8";
        try {
            return DriverManager.getConnection(url,
                    DrsConfiguration.mysqlUser(),
                    DrsConfiguration.mysqlPassword());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to connect to MySQL at " + url, e);
        }
    }

    /*   * Open a connection to the MySQL server WITHOUT specifying a
     * database, used for the bootstrap "CREATE DATABASE IF NOT EXISTS"
     * step.
         * @return fresh Connection (caller must close)
     */
    public static Connection getServerConnection() {
        ensureDriver();
        String url = "jdbc:mysql://"
                + DrsConfiguration.mysqlHost() + ":"
                + DrsConfiguration.mysqlPort() + "/"
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";
        try {
            return DriverManager.getConnection(url,
                    DrsConfiguration.mysqlUser(),
                    DrsConfiguration.mysqlPassword());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to connect to MySQL server at " + url, e);
        }
    }

    private static void ensureDriver() {
        if (driverLoaded) {
            return;
        }
        synchronized (DatabaseConnection.class) {
            if (!driverLoaded) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    driverLoaded = true;
                } catch (ClassNotFoundException e) {
                    throw new DataAccessException(
                            "MySQL JDBC driver not on classpath", e);
                }
            }
        }
    }
}
