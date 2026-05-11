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
    private static final int DATABASE_VERSION = 9; // Bumped to 9 to fix downgrade crash

    // Users Credentials Table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";

    // User Profile Table
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

    // Reviews Table
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
        db.execSQL("CREATE TABLE " + TABLE_USERS + "(" + COLUMN_USERNAME + " TEXT PRIMARY KEY, " + COLUMN_EMAIL + " TEXT, " + COLUMN_PASSWORD + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_PROFILE + "(" + COLUMN_PROFILE_EMAIL + " TEXT PRIMARY KEY, " + COLUMN_PROFILE_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_DESTINATION + " TEXT, " + COLUMN_START_DATE + " TEXT, " + COLUMN_END_DATE + " TEXT, " + COLUMN_BUDGET + " TEXT, " + COLUMN_INTERESTS + " TEXT, " + COLUMN_ITINERARY + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_REVIEWS + "(" + COLUMN_REVIEW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_REVIEW_USER + " TEXT, " + COLUMN_REVIEW_ITIN_ID + " TEXT, " + COLUMN_REVIEW_RATING + " REAL, " + COLUMN_REVIEW_TEXT + " TEXT, " + COLUMN_REVIEW_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)");
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

    public boolean insertUserData(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_USERNAME, username);
        contentValues.put(COLUMN_EMAIL, email);
        contentValues.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    public boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

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

    public Boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password});
        boolean match = cursor.getCount() > 0;
        cursor.close();
        return match;
    }

    public boolean saveTrip(String destination, String startDate, String endDate, String budget, String interests, String itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DESTINATION, destination);
        cv.put(COLUMN_START_DATE, startDate);
        cv.put(COLUMN_END_DATE, endDate);
        cv.put(COLUMN_BUDGET, budget);
        cv.put(COLUMN_INTERESTS, interests);
        cv.put(COLUMN_ITINERARY, itinerary);
        long result = db.insert(TABLE_TRIPS, null, cv);
        return result != -1;
    }
}
