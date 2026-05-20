package com.example.routemind;

import java.io.Serializable;

public class ItineraryItem implements Serializable {
    private String time;
    private String activity;
    private String location;
    private int day;

    public ItineraryItem() {}

    public ItineraryItem(String time, String activity, String location, int day) {
        this.time = time;
        this.activity = activity;
        this.location = location;
        this.day = day;
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
}
