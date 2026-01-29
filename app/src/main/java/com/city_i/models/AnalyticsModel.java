package com.city_i.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple analytics snapshot model (counts and breakdowns).
 */
public class AnalyticsModel {
    private String id;
    private Date generatedAt;
    private int totalIssues;
    private int openIssues;
    private int resolvedIssues;
    private long avgResolutionTimeMs;
    private Map<String, Integer> issuesByCategory;

    public AnalyticsModel() {
        // Required for Firebase/serialization
        this.issuesByCategory = new HashMap<>();
    }

    public AnalyticsModel(String id, Date generatedAt, int totalIssues, int openIssues,
                          int resolvedIssues, long avgResolutionTimeMs,
                          Map<String, Integer> issuesByCategory) {
        this.id = id;
        this.generatedAt = generatedAt;
        this.totalIssues = totalIssues;
        this.openIssues = openIssues;
        this.resolvedIssues = resolvedIssues;
        this.avgResolutionTimeMs = avgResolutionTimeMs;
        this.issuesByCategory = issuesByCategory != null ? issuesByCategory : new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(int totalIssues) {
        this.totalIssues = totalIssues;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(int openIssues) {
        this.openIssues = openIssues;
    }

    public int getResolvedIssues() {
        return resolvedIssues;
    }

    public void setResolvedIssues(int resolvedIssues) {
        this.resolvedIssues = resolvedIssues;
    }

    public long getAvgResolutionTimeMs() {
        return avgResolutionTimeMs;
    }

    public void setAvgResolutionTimeMs(long avgResolutionTimeMs) {
        this.avgResolutionTimeMs = avgResolutionTimeMs;
    }

    public Map<String, Integer> getIssuesByCategory() {
        return issuesByCategory;
    }

    public void setIssuesByCategory(Map<String, Integer> issuesByCategory) {
        this.issuesByCategory = issuesByCategory != null ? issuesByCategory : new HashMap<>();
    }
}

