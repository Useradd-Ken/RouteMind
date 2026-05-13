package com.example.routemind;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Feedback {
    private String id;
    private String userId;
    private String userName;
    private String userProfilePic;
    private String destination;
    private String comment;
    private String imageUrl;
    private float rating; // This will store the average of the provided ratings
    private float foodRating;
    private float accommodationRating;
    private float placesRating;
    private Timestamp timestamp;
    private String review;
    private String itineraryId;
    private String itineraryTitle;
    private boolean isPublic;
    private int usageCount;

    public Feedback() {
        // Required for Firebase
    }

    public Feedback(String id, String userId, String userName, String userProfilePic, String destination, String comment, String imageUrl, float foodRating, float accommodationRating, float placesRating, long timestampMillis) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userProfilePic = userProfilePic;
        this.destination = destination;
        this.comment = comment;
        this.imageUrl = imageUrl;
        this.foodRating = foodRating;
        this.accommodationRating = accommodationRating;
        this.placesRating = placesRating;
        
        // Calculate average only from non-zero ratings
        int count = 0;
        float total = 0;
        if (foodRating > 0) { total += foodRating; count++; }
        if (accommodationRating > 0) { total += accommodationRating; count++; }
        if (placesRating > 0) { total += placesRating; count++; }
        this.rating = count > 0 ? total / count : 0;

        this.timestamp = new Timestamp(new java.util.Date(timestampMillis));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public float getFoodRating() { return foodRating; }
    public void setFoodRating(float foodRating) { this.foodRating = foodRating; }

    public float getAccommodationRating() { return accommodationRating; }
    public void setAccommodationRating(float accommodationRating) { this.accommodationRating = accommodationRating; }

    public float getPlacesRating() { return placesRating; }
    public void setPlacesRating(float placesRating) { this.placesRating = placesRating; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getItineraryId() { return itineraryId; }
    public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }

    public String getItineraryTitle() { return itineraryTitle; }
    public void setItineraryTitle(String itineraryTitle) { this.itineraryTitle = itineraryTitle; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public long getTimestampMillis() {
        return (timestamp != null) ? timestamp.toDate().getTime() : 0;
    }
}
