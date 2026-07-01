package drs.shared.model;

import java.io.Serializable;

/**
 * Immutable latitude/longitude pair. Used as a value object embedded in
 * other entities (or returned from {@code calculateDistance} contexts).
  
 */
public class GeoLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final double latitude;
    private final double longitude;

    public GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoLocation)) return false;
        GeoLocation other = (GeoLocation) o;
        return Double.compare(other.latitude, latitude) == 0
            && Double.compare(other.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
    }

    @Override
    public String toString() {
        return String.format("(%.5f, %.5f)", latitude, longitude);
    }
}
