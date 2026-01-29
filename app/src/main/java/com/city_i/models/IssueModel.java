package com.city_i.models;

import java.util.Date;

/**
 * Data model for a reported civic issue.
 * Designed to be compatible with Firebase/serialization (no-arg constructor + getters/setters).
 */
public class IssueModel {
    private String id;
    private String title;
    private String description;
    private String category;
    private String status;
    private String imagePath;
    private String departmentId;
    private String reportedByUserId;
    private double latitude;
    private double longitude;
    private int priority;
    private Date createdAt;
    private Date updatedAt;

    public IssueModel() {
        // Required for Firebase/serialization
    }

    public IssueModel(
            String id,
            String title,
            String description,
            String category,
            String status,
            String imagePath,
            String departmentId,
            String reportedByUserId,
            double latitude,
            double longitude,
            int priority,
            Date createdAt,
            Date updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.imagePath = imagePath;
        this.departmentId = departmentId;
        this.reportedByUserId = reportedByUserId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(String reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

