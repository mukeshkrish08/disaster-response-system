package drs.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A geographic location reusable across many incidents (e.g. "Sydney CBD
 * NSW 2000"). Stores postcode, suburb, state, GPS coordinates, and an
 * optional risk zone label.
  
 */
public class Location implements Serializable {

    private static final long serialVersionUID = 1L;

    private int locationPk;
    private String locationCode;
    private String postcode;
    private String suburb;
    private String state;
    private double latitude;
    private double longitude;
    private String riskZone;
    private String displayName;
    private boolean active = true;
    private LocalDateTime createdAt;

    public Location() {
        // No-arg for JDBC/Serialization
    }

    public Location(String locationCode, String postcode, String suburb,
                    String state, double latitude, double longitude,
                    String riskZone, String displayName) {
        this.locationCode = locationCode;
        this.postcode = postcode;
        this.suburb = suburb;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riskZone = riskZone;
        this.displayName = displayName;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public int getLocationPk() { return locationPk; }
    public void setLocationPk(int locationPk) { this.locationPk = locationPk; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getSuburb() { return suburb; }
    public void setSuburb(String suburb) { this.suburb = suburb; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getRiskZone() { return riskZone; }
    public void setRiskZone(String riskZone) { this.riskZone = riskZone; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        return locationPk == ((Location) o).locationPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationPk);
    }

    @Override
    public String toString() {
        return "Location{" + locationCode + ", " + displayName + "}";
    }
}
