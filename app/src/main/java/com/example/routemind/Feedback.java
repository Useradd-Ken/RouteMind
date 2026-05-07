package com.example.routemind;

public class Feedback {
    private String id;
    private String userName;
    private String userProfilePic;
    private String destination;
    private String comment;
    private String imageUrl;
    private float rating;
    private long timestamp;

    public Feedback() {
        // Required for Firebase
    }

    public Feedback(String id, String userName, String userProfilePic, String destination, String comment, String imageUrl, float rating, long timestamp) {
        this.id = id;
        this.userName = userName;
        this.userProfilePic = userProfilePic;
        this.destination = destination;
        this.comment = comment;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getUserName() { return userName; }
    public String getUserProfilePic() { return userProfilePic; }
    public String getDestination() { return destination; }
    public String getComment() { return comment; }
    public String getImageUrl() { return imageUrl; }
    public float getRating() { return rating; }
    public long getTimestamp() { return timestamp; }
}
