package drs.shared.util;

/**
 * Application-wide constants used across both client and server.
  
 */
public final class DrsConstants {

    public static final String APP_TITLE = "Disaster Response System";
    public static final String APP_VERSION = "1.0.0";

    /** Earth radius in km, used by the Haversine distance formula. */
    public static final double EARTH_RADIUS_KM = 6371.0;

    /** Maximum length of a citizen-supplied description (chars). */
    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    /** Minimum length of a citizen-supplied description (chars). */
    public static final int MIN_DESCRIPTION_LENGTH = 5;

    /** Minimum password length (chars). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** Default dispatch search radius (km). */
    public static final double DEFAULT_DISPATCH_RADIUS_KM = 50.0;

    /** "GENESIS" marker used as the prev_hash of the first audit row. */
    public static final String AUDIT_GENESIS_HASH = "GENESIS";

    /** AES key length in bytes (32 = 256 bit). */
    public static final int AES_KEY_LENGTH_BYTES = 32;

    /** AES-GCM IV length in bytes. */
    public static final int AES_GCM_IV_LENGTH_BYTES = 12;

    /** AES-GCM authentication tag length in bits. */
    public static final int AES_GCM_TAG_LENGTH_BITS = 128;

    private DrsConstants() {
        // Not instantiable
    }
}
