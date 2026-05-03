package com.example.routemind;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "RouteMind.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRIPS = "trips";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DESTINATION = "destination";
    public static final String COLUMN_START_DATE = "start_date";
    public static final String COLUMN_END_DATE = "end_date";
    public static final String COLUMN_BUDGET = "budget";
    public static final String COLUMN_INTERESTS = "interests";
    public static final String COLUMN_ITINERARY = "itinerary";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_TRIPS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DESTINATION + " TEXT, " +
                    COLUMN_START_DATE + " TEXT, " +
                    COLUMN_END_DATE + " TEXT, " +
                    COLUMN_BUDGET + " TEXT, " +
                    COLUMN_INTERESTS + " TEXT, " +
                    COLUMN_ITINERARY + " TEXT" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        saveTripToDb(db, "Tokyo, Japan", "12/05/2024", "18/05/2024", "50000", "Anime, Food", 
                "Day 1: Shibuya Crossing and Hachiko Statue.\n" +
                "Day 2: Akihabara Electric Town and Maid Cafes.\n" +
                "Day 3: Senso-ji Temple and Nakamise Street.\n" +
                "Day 4: Ghibli Museum and Inokashira Park.\n" +
                "Day 5: Harajuku Takeshita Street and Meiji Jingu Shrine.");

        saveTripToDb(db, "Paris, France", "20/06/2024", "25/06/2024", "80000", "Art, History", 
                "Day 1: Eiffel Tower and Seine River Cruise.\n" +
                "Day 2: Louvre Museum and Tuileries Garden.\n" +
                "Day 3: Montmartre and Sacré-Cœur Basilica.\n" +
                "Day 4: Notre-Dame Cathedral and Latin Quarter.\n" +
                "Day 5: Palace of Versailles Day Trip.");
    }

    private void saveTripToDb(SQLiteDatabase db, String destination, String startDate, String endDate, String budget, String interests, String itinerary) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESTINATION, destination);
        values.put(COLUMN_START_DATE, startDate);
        values.put(COLUMN_END_DATE, endDate);
        values.put(COLUMN_BUDGET, budget);
        values.put(COLUMN_INTERESTS, interests);
        values.put(COLUMN_ITINERARY, itinerary);
        db.insert(TABLE_TRIPS, null, values);
    }

    public long saveTrip(String destination, String startDate, String endDate, String budget, String interests, String itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DESTINATION, destination);
        values.put(COLUMN_START_DATE, startDate);
        values.put(COLUMN_END_DATE, endDate);
        values.put(COLUMN_BUDGET, budget);
        values.put(COLUMN_INTERESTS, interests);
        values.put(COLUMN_ITINERARY, itinerary);

        return db.insert(TABLE_TRIPS, null, values);
    }
}
