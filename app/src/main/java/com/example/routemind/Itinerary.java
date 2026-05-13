package com.example.routemind;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;

@IgnoreExtraProperties
public class Itinerary implements Serializable {
    private String id;
    private String userId;
    private String username;
    private String title;
    private String description;
    private String itineraryDetails;
    private String imageUrl;
    private String category;
    private double price;
    private String priceBreakdown;
    private long timestamp;
    private String destination;
    private int usageCount;

    public Itinerary() {
        // Required for Firebase
    }

    public Itinerary(String id, String userId, String username, String title, String description, String itineraryDetails, 
                     String imageUrl, String category, double price, String priceBreakdown, String destination) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.title = title;
        this.description = description;
        this.itineraryDetails = itineraryDetails;
        this.imageUrl = imageUrl;
        this.category = category;
        this.price = price;
        this.priceBreakdown = priceBreakdown;
        this.destination = destination;
        this.timestamp = System.currentTimeMillis();
        this.usageCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getItineraryDetails() { return itineraryDetails; }
    public void setItineraryDetails(String itineraryDetails) { this.itineraryDetails = itineraryDetails; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPriceBreakdown() { return priceBreakdown; }
    public void setPriceBreakdown(String priceBreakdown) { this.priceBreakdown = priceBreakdown; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
}
