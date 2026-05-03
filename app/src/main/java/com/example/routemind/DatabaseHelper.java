package com.example.routemind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "RouteMind.db";
    private static final int DATABASE_VERSION = 2; // Incremented version for new table

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_NAME = "name";

    private static final String TABLE_TRIPS = "trips";
    private static final String COLUMN_TRIP_ID = "id";
    private static final String COLUMN_TRIP_DESTINATION = "destination";
    private static final String COLUMN_TRIP_START_DATE = "start_date";
    private static final String COLUMN_TRIP_END_DATE = "end_date";
    private static final String COLUMN_TRIP_BUDGET = "budget";
    private static final String COLUMN_TRIP_INTERESTS = "interests";
    private static final String COLUMN_TRIP_ITINERARY = "itinerary";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_EMAIL + " TEXT UNIQUE,"
                + COLUMN_USER_NAME + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRIP_DESTINATION + " TEXT,"
                + COLUMN_TRIP_START_DATE + " TEXT,"
                + COLUMN_TRIP_END_DATE + " TEXT,"
                + COLUMN_TRIP_BUDGET + " TEXT,"
                + COLUMN_TRIP_INTERESTS + " TEXT,"
                + COLUMN_TRIP_ITINERARY + " TEXT" + ")";
        db.execSQL(CREATE_TRIPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                    + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_TRIP_DESTINATION + " TEXT,"
                    + COLUMN_TRIP_START_DATE + " TEXT,"
                    + COLUMN_TRIP_END_DATE + " TEXT,"
                    + COLUMN_TRIP_BUDGET + " TEXT,"
                    + COLUMN_TRIP_INTERESTS + " TEXT,"
                    + COLUMN_TRIP_ITINERARY + " TEXT" + ")";
            db.execSQL(CREATE_TRIPS_TABLE);
        }
    }

    public void addUser(String email, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_NAME, name);
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public String getName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_NAME}, COLUMN_USER_EMAIL + "=?",
                new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        return null;
    }

    public long saveTrip(String destination, String start, String end, String budget, String interests, String itinerary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRIP_DESTINATION, destination);
        values.put(COLUMN_TRIP_START_DATE, start);
        values.put(COLUMN_TRIP_END_DATE, end);
        values.put(COLUMN_TRIP_BUDGET, budget);
        values.put(COLUMN_TRIP_INTERESTS, interests);
        values.put(COLUMN_TRIP_ITINERARY, itinerary);
        long id = db.insert(TABLE_TRIPS, null, values);
        db.close();
        return id;
    }
}