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
    private static final int DATABASE_VERSION = 22; // Incremented version

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
    public static final String COLUMN_TRIP_USER = "username";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_BUDGET = "budget";
    public static final String COLUMN_INTERESTS = "interests";
    public static final String COLUMN_ITINERARY = "itinerary";

    // Destinations Table
    public static final String TABLE_DESTINATIONS = "destinations";
    public static final String COLUMN_DEST_NAME = "name";

    // Expenses Table
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_EXP_ID = "id";
    public static final String COLUMN_EXP_USER = "username";
    public static final String COLUMN_EXP_CATEGORY = "category";
    public static final String COLUMN_EXP_AMOUNT = "amount";
    public static final String COLUMN_EXP_TIMESTAMP = "timestamp";

    // Booked Itineraries Table (Local Storage)
    public static final String TABLE_BOOKED = "booked_itineraries";
    public static final String COLUMN_BOOKED_ID = "id";
    public static final String COLUMN_BOOKED_USER = "username";
    public static final String COLUMN_BOOKED_TITLE = "title";
    public static final String COLUMN_BOOKED_DESC = "description";
    public static final String COLUMN_BOOKED_DETAILS = "itinerary_details";
    public static final String COLUMN_BOOKED_IMAGE = "image_url";
    public static final String COLUMN_BOOKED_CAT = "category";
    public static final String COLUMN_BOOKED_PRICE = "price";
    public static final String COLUMN_BOOKED_BREAKDOWN = "price_breakdown";
    public static final String COLUMN_BOOKED_DEST = "destination";
    public static final String COLUMN_BOOKED_TIMESTAMP = "timestamp";
    public static final String COLUMN_BOOKED_USAGE = "usage_count"; // Added column

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + "(" + COLUMN_USERNAME + " TEXT PRIMARY KEY, " + COLUMN_EMAIL + " TEXT, " + COLUMN_PASSWORD + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_PROFILE + "(" + COLUMN_PROFILE_EMAIL + " TEXT PRIMARY KEY, " + COLUMN_PROFILE_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_TRIPS + "(" + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_TRIP_USER + " TEXT, " + COLUMN_DESTINATION + " TEXT, " + COLUMN_START_DATE + " TEXT, " + COLUMN_END_DATE + " TEXT, " + COLUMN_BUDGET + " TEXT, " + COLUMN_INTERESTS + " TEXT, " + COLUMN_ITINERARY + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_DESTINATIONS + "(" + COLUMN_DEST_NAME + " TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + "(" + COLUMN_EXP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_EXP_USER + " TEXT, " + COLUMN_EXP_CATEGORY + " TEXT, " + COLUMN_EXP_AMOUNT + " REAL, " + COLUMN_EXP_TIMESTAMP + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_BOOKED + "(" + COLUMN_BOOKED_ID + " TEXT PRIMARY KEY, " + COLUMN_BOOKED_USER + " TEXT, " + COLUMN_BOOKED_TITLE + " TEXT, " + COLUMN_BOOKED_DESC + " TEXT, " + COLUMN_BOOKED_DETAILS + " TEXT, " + COLUMN_BOOKED_IMAGE + " TEXT, " + COLUMN_BOOKED_CAT + " TEXT, " + COLUMN_BOOKED_PRICE + " REAL, " + COLUMN_BOOKED_BREAKDOWN + " TEXT, " + COLUMN_BOOKED_DEST + " TEXT, " + COLUMN_BOOKED_TIMESTAMP + " INTEGER, " + COLUMN_BOOKED_USAGE + " INTEGER DEFAULT 0)");
        seedDestinations(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DESTINATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKED);
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

    public String getUsernameByEmail(String email) {
        if (email == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USERNAME}, COLUMN_EMAIL + "=?", new String[]{email}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting username by email", e);
        }
        return null;
    }

    public void addUserProfile(String email, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_EMAIL, email);
        values.put(COLUMN_PROFILE_NAME, name);
        db.insertWithOnConflict(TABLE_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getName(String email) {
        if (email == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_PROFILE, new String[]{COLUMN_PROFILE_NAME}, COLUMN_PROFILE_EMAIL + "=?", new String[]{email}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting name by email", e);
        }
        return null;
    }

    public Boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password});
        boolean match = cursor.getCount() > 0;
        cursor.close();
        return match;
    }

    public boolean saveTrip(String username, String destination, String startDate, String endDate, String budget, String interests, String itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TRIP_USER, username);
        cv.put(COLUMN_DESTINATION, destination);
        cv.put(COLUMN_START_DATE, startDate);
        cv.put(COLUMN_END_DATE, endDate);
        cv.put(COLUMN_BUDGET, budget);
        cv.put(COLUMN_INTERESTS, interests);
        cv.put(COLUMN_ITINERARY, itinerary);
        long result = db.insert(TABLE_TRIPS, null, cv);
        return result != -1;
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS, null);
    }

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

    // Expense Management Methods
    public boolean addExpense(String username, String category, double amount, long timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_EXP_USER, username);
        cv.put(COLUMN_EXP_CATEGORY, category);
        cv.put(COLUMN_EXP_AMOUNT, amount);
        cv.put(COLUMN_EXP_TIMESTAMP, timestamp);
        long result = db.insert(TABLE_EXPENSES, null, cv);
        return result != -1;
    }

    public Cursor getAllExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COLUMN_EXP_TIMESTAMP + " DESC", null);
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COLUMN_EXP_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void clearAllExpenses() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, null, null);
    }

    public double getCategoryTotal(String category) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_EXP_AMOUNT + ") FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_EXP_CATEGORY + " = ?", new String[]{category});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    // Booked Itinerary Methods (Local Storage)
    public boolean saveBookedItinerary(Itinerary itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_BOOKED_ID, itinerary.getId());
        cv.put(COLUMN_BOOKED_USER, itinerary.getUsername());
        cv.put(COLUMN_BOOKED_TITLE, itinerary.getTitle());
        cv.put(COLUMN_BOOKED_DESC, itinerary.getDescription());
        cv.put(COLUMN_BOOKED_DETAILS, itinerary.getItineraryDetails());
        cv.put(COLUMN_BOOKED_IMAGE, itinerary.getImageUrl());
        cv.put(COLUMN_BOOKED_CAT, itinerary.getCategory());
        cv.put(COLUMN_BOOKED_PRICE, itinerary.getPrice());
        cv.put(COLUMN_BOOKED_BREAKDOWN, itinerary.getPriceBreakdown());
        cv.put(COLUMN_BOOKED_DEST, itinerary.getDestination());
        cv.put(COLUMN_BOOKED_TIMESTAMP, System.currentTimeMillis());
        cv.put(COLUMN_BOOKED_USAGE, itinerary.getUsageCount());
        long result = db.insertWithOnConflict(TABLE_BOOKED, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public Cursor getAllBookedItineraries() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BOOKED + " ORDER BY " + COLUMN_BOOKED_TIMESTAMP + " DESC", null);
    }
}
