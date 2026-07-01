package drs.server;

import drs.server.repository.DatabaseBootstrap;
import drs.server.util.DrsConfiguration;
import drs.server.util.IdGenerator;
import drs.shared.util.AesEncryption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * Entry point for the Disaster Response System server.
 * Startup sequence:
 *  1. Initialise AES key (load from properties, or generate + persist)
 *  2. Run database bootstrap (create schema + seed data if needed)
 *  3. Initialise ID generator from current MAX values
 *  4. Build the ServerApplicationContext (wires services + repos)
 *  5. Start DrsServer.accept() loop
 */
public final class DrsServerApplication {

    private static final Logger LOG = LoggerFactory.getLogger(DrsServerApplication.class);

    private DrsServerApplication() {
        // Static main class
    }

    public static void main(String[] args) {
        LOG.info("Disaster Response System server starting...");
        try {
            // 1. Crypto key
            initAesKey();

            // 2. Database
            LOG.info("Verifying schema and seed data...");
            DatabaseBootstrap.bootstrap();

            // 3. ID counters
            IdGenerator.initialiseFromDatabase();

            // 4. Application context
            ServerApplicationContext context = new ServerApplicationContext(
                    DrsConfiguration.sessionTimeoutMins());

            // Configure priority strategy default
            context.getPriorityService()
                   .switchStrategy(DrsConfiguration.priorityStrategyName());

            // 5. Server
            DrsServer server = new DrsServer(
                    DrsConfiguration.serverPort(),
                    DrsConfiguration.threadPoolSize(),
                    context);

            // Graceful shutdown on Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Shutdown signal received");
                server.shutdown();
            }, "drs-shutdown"));

            server.start();
        } catch (Exception e) {
            LOG.error("Server failed to start", e);
            System.exit(1);
        }
    }

    /* Read the AES key from drs.properties, or generate a new one and
     * persist it back if the property is blank.
     */
    private static void initAesKey() {
        String b64 = DrsConfiguration.aesKeyBase64();
        if (b64 == null || b64.isEmpty()) {
            javax.crypto.SecretKey key = AesEncryption.generateKey();
            String generated = Base64.getEncoder().encodeToString(key.getEncoded());
            DrsConfiguration.writeProperty("aes.key.base64", generated);
            AesEncryption.init(key);
            LOG.warn("No AES key found in drs.properties - generated a new "
                    + "256-bit key and wrote it back");
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(b64);
            AesEncryption.init(AesEncryption.keyFromBytes(keyBytes));
            LOG.info("AES key loaded ({} bytes)", keyBytes.length);
        }
    }
}
