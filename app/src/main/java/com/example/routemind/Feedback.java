package com.example.routemind;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Feedback {
    private String id;
    private String userName;
    private String userProfilePic;
    private String destination;
    private String comment;
    private String imageUrl;
    private float rating;
    private Timestamp timestamp;
    private String review;
    private String itineraryId;

    public Feedback() {
        // Required for Firebase
    }

    public Feedback(String id, String userName, String userProfilePic, String destination, String comment, String imageUrl, float rating, long timestampMillis) {
        this.id = id;
        this.userName = userName;
        this.userProfilePic = userProfilePic;
        this.destination = destination;
        this.comment = comment;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.timestamp = new Timestamp(new java.util.Date(timestampMillis));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserProfilePic() { return userProfilePic; }
    public void setUserProfilePic(String userProfilePic) { this.userProfilePic = userProfilePic; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getItineraryId() { return itineraryId; }
    public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }

    public long getTimestampMillis() {
        return (timestamp != null) ? timestamp.toDate().getTime() : 0;
    }
}
