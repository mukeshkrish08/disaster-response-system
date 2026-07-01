package drs.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from {@code drs.properties} on the classpath or
 * the working directory.
 *
 * The lookup order is:
 *  1. Working directory ({@code ./drs.properties}) - for an installed
 *     deployment
 *  2. Classpath resource ({@code /drs.properties}) - for development
 *  3. Default values from the constants in this class
 *
 * The class is mutable (writeProperty) because we may need to write the
 * AES key back to the file the first time it is generated.
  
 */
public final class DrsConfiguration {

    private static final Properties PROPS = new Properties();
    private static volatile File sourceFile;
    private static volatile boolean loaded;

    private DrsConfiguration() {
        // Static utility
    }

    /*   * Lazy initialisation - load once from the first available source.
     */
    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        // Try working directory first
        File workingFile = new File("drs.properties");
        if (workingFile.isFile()) {
            try (FileInputStream in = new FileInputStream(workingFile)) {
                PROPS.load(in);
                sourceFile = workingFile;
            } catch (Exception ignored) {
                // Fall through to classpath
            }
        }
        // Fall back to classpath
        if (sourceFile == null) {
            try (InputStream in = DrsConfiguration.class
                    .getResourceAsStream("/drs.properties")) {
                if (in != null) {
                    PROPS.load(in);
                }
            } catch (Exception ignored) {
                // Use defaults
            }
        }
        loaded = true;
    }

    public static String mysqlHost() {
        ensureLoaded();
        return PROPS.getProperty("mysql.host", "localhost");
    }

    public static int mysqlPort() {
        ensureLoaded();
        return Integer.parseInt(PROPS.getProperty("mysql.port", "3306"));
    }

    public static String mysqlUser() {
        ensureLoaded();
        return PROPS.getProperty("mysql.user", "root");
    }

    public static String mysqlPassword() {
        ensureLoaded();
        return PROPS.getProperty("mysql.password", "");
    }

    public static String mysqlDatabase() {
        ensureLoaded();
        return PROPS.getProperty("mysql.database", "disaster_response_system");
    }

    public static int serverPort() {
        ensureLoaded();
        return Integer.parseInt(PROPS.getProperty("server.port", "5050"));
    }

    public static int threadPoolSize() {
        ensureLoaded();
        return Integer.parseInt(PROPS.getProperty("server.threadPoolSize", "20"));
    }

    public static int sessionTimeoutMins() {
        ensureLoaded();
        return Integer.parseInt(PROPS.getProperty("session.timeoutMins", "30"));
    }

    public static String priorityStrategyName() {
        ensureLoaded();
        return PROPS.getProperty("priority.strategy", "CAP");
    }

    public static double dispatchRadiusKm() {
        ensureLoaded();
        return Double.parseDouble(PROPS.getProperty("dispatch.radius.km", "50.0"));
    }

    public static String aesKeyBase64() {
        ensureLoaded();
        return PROPS.getProperty("aes.key.base64", "");
    }

    /*   * Update the in-memory property and, if loaded from a real file,
     * persist back to that file.
         * @param key   property key
     * @param value new value
     */
    public static synchronized void writeProperty(String key, String value) {
        ensureLoaded();
        PROPS.setProperty(key, value);
        if (sourceFile != null) {
            try (FileOutputStream out = new FileOutputStream(sourceFile)) {
                PROPS.store(out, "Disaster Response System runtime config");
            } catch (Exception ignored) {
                // Non-fatal - caller will retain the in-memory value
            }
        }
    }
}
