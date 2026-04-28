package com.example.routemind;

import java.io.Serializable;

public class ItineraryItem implements Serializable {
    private int id;
    private int day;
    private String time;
    private String cost;
    private String title;
    private String location;
    private boolean isSelected;

    public ItineraryItem(int day, String time, String cost, String title, String location) {
        this.day = day;
        this.time = time;
        this.cost = cost;
        this.title = title;
        this.location = location;
        this.isSelected = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getDay() { return day; }
    public String getTime() { return time; }
    public String getCost() { return cost; }
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}