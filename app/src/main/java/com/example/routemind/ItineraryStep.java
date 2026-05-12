package com.example.routemind;

public class ItineraryStep {
    private String time;
    private String activity;
    private String location;

    public ItineraryStep(String time, String activity, String location) {
        this.time = time;
        this.activity = activity;
        this.location = location;
    }

    public String getTime() { return time; }
    public String getActivity() { return activity; }
    public String getLocation() { return location; }
}
