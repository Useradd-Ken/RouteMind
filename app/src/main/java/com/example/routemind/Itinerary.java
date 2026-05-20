package com.example.routemind;

import com.google.firebase.firestore.IgnoreExtraProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Itinerary implements Serializable {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String destination;
    private String itinerary; // Raw string (often JSON)
    private String imageUrl;
    private String category;
    private double price;
    private String priceBreakdown;
    private long timestamp;
    private long usageCount;

    // Structured list for better UI rendering
    private List<ItineraryItem> activities = new ArrayList<>();

    public Itinerary() {
        // Required for Firebase
    }

    public Itinerary(String id, String userId, String title, String description, String destination, String itinerary, String imageUrl, String category, double price, String priceBreakdown, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.destination = destination;
        this.itinerary = itinerary;
        this.imageUrl = imageUrl;
        this.category = category;
        this.price = price;
        this.priceBreakdown = priceBreakdown;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getItinerary() { return itinerary; }
    public void setItinerary(String itinerary) { this.itinerary = itinerary; }

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

    public long getUsageCount() { return usageCount; }
    public void setUsageCount(long usageCount) { this.usageCount = usageCount; }

    public List<ItineraryItem> getActivities() { return activities; }
    public void setActivities(List<ItineraryItem> activities) { this.activities = activities; }
}
