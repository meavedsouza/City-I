package com.city_i.models;

import java.util.Date;

/**
 * Basic user profile model.
 */
public class UserModel {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String role; // e.g., "citizen", "staff", "admin"
    private String departmentId;
    private boolean active;
    private Date createdAt;

    public UserModel() {
        // Required for Firebase/serialization
    }

    public UserModel(String id, String name, String email, String phone, String role,
                     String departmentId, boolean active, Date createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.departmentId = departmentId;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

