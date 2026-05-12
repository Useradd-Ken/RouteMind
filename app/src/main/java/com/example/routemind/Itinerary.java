package com.example.routemind;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Itinerary {
    private String id;
    private String title;
    private String description;
    private String itinerary;
    private String imageUrl;
    private String category;
    private double price;
    private String priceBreakdown;

    public Itinerary() {
        // Required for Firebase
    }

    public Itinerary(String id, String title, String description, String itinerary, String imageUrl, String category, double price, String priceBreakdown) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.itinerary = itinerary;
        this.imageUrl = imageUrl;
        this.category = category;
        this.price = price;
        this.priceBreakdown = priceBreakdown;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
}
