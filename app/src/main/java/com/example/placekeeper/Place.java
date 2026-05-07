package com.example.placekeeper;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.List;

public class Place {
    private String id;
    private String placeName;
    private String note;
    private double latitude;
    private double longitude;
    private List<String> tags;
    private String imageUrl;
    @ServerTimestamp
    private Timestamp createdAt;

    public Place() {
        // Required for Firestore
    }

    public Place(String placeName, String note, double latitude, double longitude) {
        this.placeName = placeName;
        this.note = note;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}