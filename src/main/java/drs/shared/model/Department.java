package drs.shared.model;

import drs.shared.enums.DepartmentType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An external agency or internal team category (Fire & Rescue, Hospital,
 * Police, etc.). Owns one or more {@link ResponseTeam}s.
  
 */
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    private int departmentPk;
    private String departmentCode;
    private String name;
    private DepartmentType departmentType;
    private boolean active;
    private LocalDateTime createdAt;

    public Department() {
        // No-arg for JDBC/Serialization
    }

    public Department(String departmentCode, String name,
                      DepartmentType departmentType) {
        this.departmentCode = departmentCode;
        this.name = name;
        this.departmentType = departmentType;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public int getDepartmentPk() { return departmentPk; }
    public void setDepartmentPk(int departmentPk) { this.departmentPk = departmentPk; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DepartmentType getDepartmentType() { return departmentType; }
    public void setDepartmentType(DepartmentType departmentType) { this.departmentType = departmentType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        return departmentPk == ((Department) o).departmentPk;
    }

    @Override
    public int hashCode() {
        return Objects.hash(departmentPk);
    }

    @Override
    public String toString() {
        return "Department{" + departmentCode + ", " + name + "}";
    }
}
