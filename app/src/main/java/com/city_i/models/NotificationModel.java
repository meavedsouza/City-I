package com.city_i.models;

import java.util.Date;

/**
 * Data model for notifications (FCM + local storage).
 */
public class NotificationModel {
    private int id;
    private String title;
    private String message;
    private String issueId;
    private String type;
    private String priority;
    private Date timestamp;
    private boolean isRead;

    public NotificationModel() {
        // Required for Firebase/serialization
    }

    public NotificationModel(int id, String title, String message, String issueId, String type,
                             String priority, Date timestamp, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.issueId = issueId;
        this.type = type;
        this.priority = priority;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}

