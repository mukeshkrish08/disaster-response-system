package drs.server.repository;

import drs.shared.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs at server startup. Creates the database if missing, executes
 * {@code schema.sql} to create all 13 tables, and seeds demo data from
 * {@code seed_data.sql} if the {@code users} table is empty.
 *
 * The SQL scripts live in {@code src/main/resources/sql/} so they are
 * easy to edit without recompiling Java.
  
 */
public final class DatabaseBootstrap {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private DatabaseBootstrap() {
        // Static utility
    }

    /*   * Run the full bootstrap sequence:
     *  1. CREATE DATABASE IF NOT EXISTS
     *  2. Execute schema.sql
     *  3. If users table is empty, execute seed_data.sql
     */
    public static void bootstrap() {
        ensureDatabaseExists();
        runScript("/sql/schema.sql");
        if (!hasAnyUser()) {
            LOG.info("No users found - loading seed data");
            runScript("/sql/seed_data.sql");
        } else {
            LOG.info("Seed data already loaded (skipping)");
        }
    }

    private static void ensureDatabaseExists() {
        try (Connection c = DatabaseConnection.getServerConnection();
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS disaster_response_system "
                    + "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            LOG.info("Database disaster_response_system ready");
        } catch (Exception e) {
            throw new DataAccessException("Failed to create database", e);
        }
    }

    private static void runScript(String classpathResource) {
        String script = readResource(classpathResource);
        List<String> statements = splitStatements(script);
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement()) {
            for (String sql : statements) {
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
            LOG.info("Executed {} ({} statements)", classpathResource, statements.size());
        } catch (Exception e) {
            throw new DataAccessException(
                    "Failed to execute " + classpathResource, e);
        }
    }

    private static String readResource(String path) {
        try (InputStream in = DatabaseBootstrap.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new DataAccessException("SQL resource not found: " + path);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new DataAccessException("Failed to read " + path, e);
        }
    }

    /*   * Split a SQL script into individual statements on semicolons. Skips
     * lines that begin with "--" (comments) and lines that consist of
     * only whitespace. Naive but sufficient for our own scripts.
         * @param script raw script text
     * @return list of executable statements
     */
    private static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String stmt = current.toString().trim();
                // Remove trailing semicolon (JDBC doesn't want it)
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                }
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            }
        }
        return statements;
    }

    private static boolean hasAnyUser() {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            // If users table doesn't exist yet, we haven't seeded
            return false;
        }
    }
}
