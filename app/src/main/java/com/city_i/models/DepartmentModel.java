package com.city_i.models;

import java.util.Date;

/**
 * Represents a municipal department responsible for issues.
 */
public class DepartmentModel {
    private String id;
    private String name;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private Date createdAt;

    public DepartmentModel() {
        // Required for Firebase/serialization
    }

    public DepartmentModel(String id, String name, String description,
                           String contactEmail, String contactPhone, Date createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

