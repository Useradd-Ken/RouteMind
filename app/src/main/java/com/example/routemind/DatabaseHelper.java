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
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "RouteMind.db";

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_NAME = "name";

    private static final String TABLE_TRIPS = "trips";
    private static final String COLUMN_TRIP_ID = "id";
    private static final String COLUMN_TRIP_DESTINATION = "destination";
    private static final String COLUMN_TRIP_START_DATE = "start_date";
    private static final String COLUMN_TRIP_END_DATE = "end_date";
    private static final String COLUMN_TRIP_ITINERARY = "itinerary";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_USER_NAME + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                + COLUMN_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TRIP_DESTINATION + " TEXT,"
                + COLUMN_TRIP_START_DATE + " TEXT,"
                + COLUMN_TRIP_END_DATE + " TEXT,"
                + COLUMN_TRIP_ITINERARY + " TEXT" + ")";
        db.execSQL(CREATE_TRIPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addUser(String email, String name) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_EMAIL, email);
            values.put(COLUMN_USER_NAME, name);
            db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error adding user", e);
        }
    }

    public String getName(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String name = null;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_USER_NAME},
                    COLUMN_USER_EMAIL + "=?", new String[]{email}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting name", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return name;
    }

    public long addTrip(String destination, String startDate, String endDate, String itinerary) {
        SQLiteDatabase db = null;
        long id = -1;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_TRIP_DESTINATION, destination);
            values.put(COLUMN_TRIP_START_DATE, startDate);
            values.put(COLUMN_TRIP_END_DATE, endDate);
            values.put(COLUMN_TRIP_ITINERARY, itinerary);
            id = db.insert(TABLE_TRIPS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding trip", e);
        }
        return id;
    }

    public List<String> getAllDestinations() {
        List<String> destinations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(true, TABLE_TRIPS, new String[]{COLUMN_TRIP_DESTINATION},
                    null, null, null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    destinations.add(cursor.getString(0));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting destinations", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return destinations;
    }
}
