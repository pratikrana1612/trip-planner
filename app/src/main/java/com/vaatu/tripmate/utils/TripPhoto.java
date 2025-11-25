package com.vaatu.tripmate.utils;

import java.io.Serializable;

public class TripPhoto implements Serializable {
    private String id;
    private String tripId;
    private String filePath;
    private long createdAt;

    public TripPhoto() {
        // Required for Firebase
    }

    public TripPhoto(String id, String tripId, String filePath, long createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}



