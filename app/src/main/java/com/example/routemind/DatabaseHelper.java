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
    // Bumped to version 5 to trigger a fresh recreation of all tables
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_NAME = "name";

<<<<<<< Updated upstream
    // Table Trips
    private static final String TABLE_TRIPS = "trips";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DESTINATION = "destination";
    private static final String COLUMN_START_DATE = "start_date";
    private static final String COLUMN_END_DATE = "end_date";
    private static final String COLUMN_BUDGET = "budget";
    private static final String COLUMN_INTERESTS = "interests";
    private static final String COLUMN_ITINERARY = "itinerary";
=======
    public static final String TABLE_TRIPS = "trips";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_BUDGET = "budget";
    public static final String COLUMN_INTERESTS = "interests";
    public static final String COLUMN_ITINERARY = "itinerary";
>>>>>>> Stashed changes

    private static final String TABLE_DESTINATIONS = "destinations";
    private static final String COLUMN_DEST_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
<<<<<<< Updated upstream
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_USER_NAME + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DESTINATION + " TEXT,"
                + COLUMN_START_DATE + " TEXT,"
                + COLUMN_END_DATE + " TEXT,"
                + COLUMN_BUDGET + " TEXT,"
                + COLUMN_INTERESTS + " TEXT,"
                + COLUMN_ITINERARY + " TEXT" + ")";
        db.execSQL(CREATE_TRIPS_TABLE);
=======
        Log.d(TAG, "Creating database tables...");
        db.execSQL("CREATE TABLE " + TABLE_USERS + "(" + COLUMN_USER_EMAIL + " TEXT PRIMARY KEY," + COLUMN_USER_NAME + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_TRIPS + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_DESTINATION + " TEXT," + COLUMN_START_DATE + " TEXT," + COLUMN_END_DATE + " TEXT," + COLUMN_BUDGET + " TEXT," + COLUMN_INTERESTS + " TEXT," + COLUMN_ITINERARY + " TEXT)");
        db.execSQL("CREATE TABLE " + TABLE_DESTINATIONS + "(" + COLUMN_DEST_NAME + " TEXT PRIMARY KEY)");

        seedDestinations(db);
        insertSampleData(db);
>>>>>>> Stashed changes
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from " + oldVersion + " to " + newVersion);
        // Force recreation for any version mismatch to ensure schema integrity
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DESTINATIONS);
        onCreate(db);
    }

<<<<<<< Updated upstream
    // User Methods
=======
    private void seedDestinations(SQLiteDatabase db) {
        String[] destinations = {"Manila", "Cebu", "Davao", "Palawan", "Bohol", "Boracay", "Baguio", "Vigan", "Siargao", "Legazpi"};
        for (String dest : destinations) {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_DEST_NAME, dest);
            db.insert(TABLE_DESTINATIONS, null, cv);
        }
    }

    public List<String> getAllDestinations() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase(); // Use writable to allow recovery if needed
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_DESTINATIONS, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Table 'destinations' missing. Attempting recovery...");
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DESTINATIONS + "(" + COLUMN_DEST_NAME + " TEXT PRIMARY KEY)");
                seedDestinations(db);
            } catch (Exception ignored) {}
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    private void insertSampleData(SQLiteDatabase db) {
        saveTripToDb(db, "Palawan, Philippines", "12/05/2024", "16/05/2024", "15000", "Nature", "Day 1: Underground River.\nDay 2: El Nido Tour.");
        saveTripToDb(db, "Cebu, Philippines", "20/06/2024", "24/06/2024", "12000", "Sea", "Day 1: Magellan's Cross.\nDay 2: Oslob Whale Sharks.");
    }

    private void saveTripToDb(SQLiteDatabase db, String dest, String start, String end, String budget, String interests, String itin) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESTINATION, dest);
        values.put(COLUMN_START_DATE, start);
        values.put(COLUMN_END_DATE, end);
        values.put(COLUMN_BUDGET, budget);
        values.put(COLUMN_INTERESTS, interests);
        values.put(COLUMN_ITINERARY, itin);
        db.insert(TABLE_TRIPS, null, values);
    }

>>>>>>> Stashed changes
    public void addUser(String email, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_NAME, name);
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_NAME}, COLUMN_USER_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        if (cursor != null) cursor.close();
        return null;
    }

<<<<<<< Updated upstream
    // Trip Methods
    public long saveTrip(String destination, String startDate, String endDate, String budget, String interests, String itinerary) {
=======
    public long saveTrip(String dest, String start, String end, String budget, String interests, String itin) {
>>>>>>> Stashed changes
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESTINATION, dest);
        values.put(COLUMN_START_DATE, start);
        values.put(COLUMN_END_DATE, end);
        values.put(COLUMN_BUDGET, budget);
        values.put(COLUMN_INTERESTS, interests);
<<<<<<< Updated upstream
        values.put(COLUMN_ITINERARY, itinerary);
        long id = db.insert(TABLE_TRIPS, null, values);
        db.close();
        return id;
=======
        values.put(COLUMN_ITINERARY, itin);
        return db.insert(TABLE_TRIPS, null, values);
>>>>>>> Stashed changes
    }
}
