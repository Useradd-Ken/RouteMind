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
        saveTripToDb(db, "Palawan, Philippines", "12/05/2024", "16/05/2024", "15000", "Nature, Adventure", 
                "Day 1: Puerto Princesa Underground River Tour.\n" +
                "Day 2: Travel to El Nido, Nacpan Beach sunset.\n" +
                "Day 3: El Nido Tour A (Big Lagoon, Secret Lagoon).\n" +
                "Day 4: El Nido Tour C (Hidden Beach, Helicopter Island).\n" +
                "Day 5: Relax at Las Cabanas Beach and Departure.");

        saveTripToDb(db, "Cebu, Philippines", "20/06/2024", "24/06/2024", "12000", "Culture, Sea", 
                "Day 1: Cebu City Tour (Magellan's Cross, Fort San Pedro).\n" +
                "Day 2: Whale Shark Watching in Oslob and Sumilon Island.\n" +
                "Day 3: Canyoneering at Kawasan Falls.\n" +
                "Day 4: Moalboal Island Hopping (Sardine Run, Sea Turtles).\n" +
                "Day 5: Sirao Garden and Temple of Leah.");
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
