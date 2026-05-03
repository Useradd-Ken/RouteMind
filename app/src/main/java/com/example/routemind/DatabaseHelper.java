package com.example.routemind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "RouteMind.db";
    private static final int DATABASE_VERSION = 7; // Bumped version for merged schema

    // Users Credentials Table (from old DBHelper)
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";

    // User Profile Table (for additional info)
    public static final String TABLE_PROFILE = "users_profile";
    public static final String COLUMN_PROFILE_EMAIL = "email";
    public static final String COLUMN_PROFILE_NAME = "name";

    // Trips Table
    public static final String TABLE_TRIPS = "trips";
    public static final String COLUMN_TRIP_ID = "id";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_BUDGET = "budget";
    public static final String COLUMN_INTERESTS = "interests";
    public static final String COLUMN_ITINERARY = "itinerary";

    // Reviews Table (from old DBHelper)
    public static final String TABLE_REVIEWS = "reviews";
    public static final String COLUMN_REVIEW_ID = "id";
    public static final String COLUMN_REVIEW_USER = "username";
    public static final String COLUMN_REVIEW_ITIN_ID = "itinerary_id";
    public static final String COLUMN_REVIEW_RATING = "rating";
    public static final String COLUMN_REVIEW_TEXT = "review";
    public static final String COLUMN_REVIEW_TIME = "timestamp";

    // Destinations Table
    public static final String TABLE_DESTINATIONS = "destinations";
    public static final String COLUMN_DEST_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating merged database tables...");
        
        // Credentials
        db.execSQL("CREATE TABLE " + TABLE_USERS + "(" + COLUMN_USERNAME + " TEXT PRIMARY KEY, " + COLUMN_EMAIL + " TEXT, " + COLUMN_PASSWORD + " TEXT)");
        
        // Profile
        db.execSQL("CREATE TABLE " + TABLE_PROFILE + "(" + COLUMN_PROFILE_EMAIL + " TEXT PRIMARY KEY, " + COLUMN_PROFILE_NAME + " TEXT)");
        
        // Trips
        db.execSQL("CREATE TABLE " + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_DESTINATION + " TEXT, " + COLUMN_START_DATE + " TEXT, " + COLUMN_END_DATE + " TEXT, " + COLUMN_BUDGET + " TEXT, " + COLUMN_INTERESTS + " TEXT, " + COLUMN_ITINERARY + " TEXT)");
        
        // Reviews
        db.execSQL("CREATE TABLE " + TABLE_REVIEWS + "(" + COLUMN_REVIEW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_REVIEW_USER + " TEXT, " + COLUMN_REVIEW_ITIN_ID + " TEXT, " + COLUMN_REVIEW_RATING + " REAL, " + COLUMN_REVIEW_TEXT + " TEXT, " + COLUMN_REVIEW_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)");
        
        // Destinations
        db.execSQL("CREATE TABLE " + TABLE_DESTINATIONS + "(" + COLUMN_DEST_NAME + " TEXT PRIMARY KEY)");

        seedDestinations(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REVIEWS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DESTINATIONS);
        onCreate(db);
    }

    private void seedDestinations(SQLiteDatabase db) {
        String[] destinations = {"Manila", "Cebu", "Davao", "Palawan", "Bohol", "Boracay", "Baguio", "Vigan", "Siargao", "Legazpi", "Puerto Princesa", "Tagaytay", "Dumaguete", "Iloilo"};
        for (String dest : destinations) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_DEST_NAME, dest);
            db.insert(TABLE_DESTINATIONS, null, cv);
        }
    }

    // --- Login/Auth Methods ---
    public Boolean insertUserData(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USERNAME, username);
        cv.put(COLUMN_EMAIL, email);
        cv.put(COLUMN_PASSWORD, password);
        return db.insert(TABLE_USERS, null, cv) != -1;
    }

    public Boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password});
        boolean match = cursor.getCount() > 0;
        cursor.close();
        return match;
    }

    public Cursor getAllUsers() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_USERS, null);
    }

    // --- Profile Methods ---
    public void addUserProfile(String email, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_EMAIL, email);
        values.put(COLUMN_PROFILE_NAME, name);
        db.insertWithOnConflict(TABLE_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PROFILE, new String[]{COLUMN_PROFILE_NAME}, COLUMN_PROFILE_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    // --- Trip Methods ---
    public long saveTrip(String dest, String start, String end, String budget, String interests, String itin) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESTINATION, dest);
        values.put(COLUMN_START_DATE, start);
        values.put(COLUMN_END_DATE, end);
        values.put(COLUMN_BUDGET, budget);
        values.put(COLUMN_INTERESTS, interests);
        values.put(COLUMN_ITINERARY, itin);
        return db.insert(TABLE_TRIPS, null, values);
    }

    // --- Review Methods ---
    public Boolean insertReview(String username, String itineraryId, float rating, String review) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_REVIEW_USER, username);
        cv.put(COLUMN_REVIEW_ITIN_ID, itineraryId);
        cv.put(COLUMN_REVIEW_RATING, rating);
        cv.put(COLUMN_REVIEW_TEXT, review);
        return db.insert(TABLE_REVIEWS, null, cv) != -1;
    }

    public Boolean updateReview(String username, String itineraryId, float rating, String review) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_REVIEW_RATING, rating);
        cv.put(COLUMN_REVIEW_TEXT, review);
        return db.update(TABLE_REVIEWS, cv, COLUMN_REVIEW_USER + " = ? AND " + COLUMN_REVIEW_ITIN_ID + " = ?", new String[]{username, itineraryId}) != -1;
    }

    public Cursor getUserReview(String username, String itineraryId) {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_REVIEWS + " WHERE " + COLUMN_REVIEW_USER + " = ? AND " + COLUMN_REVIEW_ITIN_ID + " = ?", new String[]{username, itineraryId});
    }

    public Cursor getAllReviews() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_REVIEWS + " ORDER BY " + COLUMN_REVIEW_TIME + " DESC", null);
    }

    // --- Destination Methods ---
    public List<String> getAllDestinations() {
        List<String> destinations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DESTINATIONS, null);
        if (cursor.moveToFirst()) {
            do {
                destinations.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return destinations;
    }
}
