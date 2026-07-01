package drs.client.net;

import drs.shared.protocol.ProtocolConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the client's drs.properties for host + port. Same lookup order
 * as the server: working directory first, then classpath, then defaults.
  
 */
public final class ClientConfiguration {

    private static final Properties PROPS = new Properties();
    private static volatile boolean loaded;

    private ClientConfiguration() {
        // Static utility
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        File workingFile = new File("drs.properties");
        if (workingFile.isFile()) {
            try (FileInputStream in = new FileInputStream(workingFile)) {
                PROPS.load(in);
            } catch (Exception ignored) {
                // Fall through
            }
        }
        if (PROPS.isEmpty()) {
            try (InputStream in = ClientConfiguration.class
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

    public static String serverHost() {
        ensureLoaded();
        return PROPS.getProperty("server.host",
                ProtocolConstants.DEFAULT_SERVER_HOST);
    }

    public static int serverPort() {
        ensureLoaded();
        return Integer.parseInt(PROPS.getProperty("server.port",
                String.valueOf(ProtocolConstants.DEFAULT_SERVER_PORT)));
    }
}
