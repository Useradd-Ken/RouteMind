package com.example.routemind;

import java.io.Serializable;

/**
 * Model class for an itinerary item.
 */
public class ItineraryItem implements Serializable {
    private int day;
    private String time;
    private String cost;
    private String activityTitle;
    private String location;
    private boolean isSelected;

    public ItineraryItem(int day, String time, String cost, String activityTitle, String location) {
        this.day = day;
        this.time = time;
        this.cost = cost;
        this.activityTitle = activityTitle;
        this.location = location;
        this.isSelected = false;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle) {
        this.activityTitle = activityTitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
