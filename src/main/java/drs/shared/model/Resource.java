package drs.shared.model;

import drs.shared.enums.ResourceStatus;
import drs.shared.enums.ResourceType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A reusable physical resource (vehicle, medical kit, generator, etc.)
 * tracked by the system. The {@code quantityAvailable} field is the
 * subject of atomic UPDATE...WHERE operations to prevent over-allocation.
  
 */
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    private int resourcePk;
    private String resourceCode;
    private String resourceName;
    private ResourceType resourceType;
    private int quantityTotal;
    private int quantityAvailable;
    private Integer homeLocationPk;        // nullable
    private String homeLocationDisplay;    // hydrated for display
    private ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Resource() {
        // No-arg for JDBC/Serialization
        this.status = ResourceStatus.AVAILABLE;
    }

    public Resource(String resourceCode, String resourceName,
                    ResourceType resourceType, int quantityTotal,
                    Integer homeLocationPk) {
        this();
        this.resourceCode = resourceCode;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.quantityTotal = quantityTotal;
        this.quantityAvailable = quantityTotal;
        this.homeLocationPk = homeLocationPk;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public int getResourcePk() { return resourcePk; }
    public void setResourcePk(int resourcePk) { this.resourcePk = resourcePk; }

    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public int getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(int quantityTotal) { this.quantityTotal = quantityTotal; }

    public int getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(int quantityAvailable) { this.quantityAvailable = quantityAvailable; }

    public Integer getHomeLocationPk() { return homeLocationPk; }
    public void setHomeLocationPk(Integer homeLocationPk) { this.homeLocationPk = homeLocationPk; }

    public String getHomeLocationDisplay() { return homeLocationDisplay; }
    public void setHomeLocationDisplay(String homeLocationDisplay) { this.homeLocationDisplay = homeLocationDisplay; }

    public ResourceStatus getStatus() { return status; }
    public void setStatus(ResourceStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resource)) return false;
        return resourcePk == ((Resource) o).resourcePk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourcePk);
    }

    @Override
    public String toString() {
        return "Resource{" + resourceCode + ", " + resourceName + ", "
             + resourceType + ", " + quantityAvailable + "/" + quantityTotal + "}";
    }
}
