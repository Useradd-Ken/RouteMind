package com.example.routemind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "RouteMind.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_ITINERARY = "itinerary";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DAY = "day";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_COST = "cost";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_LOCATION = "location";

    private static final String TABLE_PLACES = "places";
    private static final String COLUMN_PLACE_NAME = "name";
    private static final String COLUMN_PLACE_DESC = "description";
    private static final String COLUMN_PLACE_CAT = "category";
    private static final String COLUMN_PLACE_RATING = "rating";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ITINERARY_TABLE = "CREATE TABLE " + TABLE_ITINERARY + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DAY + " INTEGER,"
                + COLUMN_TIME + " TEXT,"
                + COLUMN_COST + " TEXT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_LOCATION + " TEXT" + ")";
        db.execSQL(CREATE_ITINERARY_TABLE);

        String CREATE_PLACES_TABLE = "CREATE TABLE " + TABLE_PLACES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PLACE_NAME + " TEXT,"
                + COLUMN_PLACE_DESC + " TEXT,"
                + COLUMN_PLACE_CAT + " TEXT,"
                + COLUMN_PLACE_RATING + " INTEGER" + ")";
        db.execSQL(CREATE_PLACES_TABLE);
        
        seedPlaces(db);
    }

    private void seedPlaces(SQLiteDatabase db) {
        String[][] places = {
            {"Magellan's Cross", "Historic religious site", "Culture", "5"},
            {"Basilica del Santo Niño", "Oldest Roman Catholic church", "Religion", "5"},
            {"Fort San Pedro", "Military defense structure", "History", "4"},
            {"Taoist Temple", "Built by Cebu's Chinese community", "Religion", "4"},
            {"Temple of Leah", "Cebu's Taj Mahal", "Landmark", "4"},
            {"Sirao Garden", "Little Amsterdam of Cebu", "Nature", "4"},
            {"10,000 Roses Cafe", "Instagrammable garden of white roses", "Cafe", "3"},
            {"Oslob Whale Sharks", "Swimming with whale sharks", "Adventure", "5"},
            {"Kawasan Falls", "Turquoise water waterfalls", "Nature", "5"},
            {"Cebu Ocean Park", "Largest oceanarium in PH", "Attraction", "4"},
            {"Simala Parish Church", "Castle-like church in Sibonga", "Religion", "5"},
            {"Yap-Sandiego Ancestral House", "One of the oldest houses in PH", "History", "4"},
            {"Colon Street", "Oldest national road in PH", "History", "3"},
            {"Cebu Safari and Adventure Park", "Wildlife sanctuary in Carmen", "Adventure", "5"},
            {"Mactan Shrine", "Site of the Battle of Mactan", "History", "4"},
            {"Tops Lookout", "Panoramic view of Cebu City", "Landmark", "4"},
            {"Casa Gorordo Museum", "Lifestyle of 19th century Cebuanos", "Culture", "4"},
            {"Mountain View Nature's Park", "Scenic views and retreat", "Nature", "3"},
            {"Moalboal Sardine Run", "Millions of sardines near the shore", "Adventure", "5"},
            {"Pescador Island", "Famous diving and snorkeling spot", "Nature", "5"}
        };

        for (String[] place : places) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PLACE_NAME, place[0]);
            values.put(COLUMN_PLACE_DESC, place[1]);
            values.put(COLUMN_PLACE_CAT, place[2]);
            values.put(COLUMN_PLACE_RATING, Integer.parseInt(place[3]));
            db.insert(TABLE_PLACES, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITINERARY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
        onCreate(db);
    }

    public void addItineraryItem(ItineraryItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DAY, item.getDay());
        values.put(COLUMN_TIME, item.getTime());
        values.put(COLUMN_COST, item.getCost());
        values.put(COLUMN_TITLE, item.getTitle());
        values.put(COLUMN_LOCATION, item.getLocation());

        db.insert(TABLE_ITINERARY, null, values);
        db.close();
    }

    public void deleteItineraryItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITINERARY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<ItineraryItem> getAllItineraryItems() {
        List<ItineraryItem> itemList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_ITINERARY;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ItineraryItem item = new ItineraryItem(
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );
                item.setId(cursor.getInt(0));
                itemList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return itemList;
    }

    public String getPlacesContext() {
        StringBuilder sb = new StringBuilder();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PLACES + " ORDER BY " + COLUMN_PLACE_RATING + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                sb.append(cursor.getString(1)).append(" (").append(cursor.getString(3)).append(", Rating: ")
                  .append(cursor.getInt(4)).append("/5): ")
                  .append(cursor.getString(2)).append("\n");
            } while (cursor.moveToNext());
        }
        cursor.close();
        return sb.toString();
    }

    public void clearItinerary() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_ITINERARY);
        db.close();
    }
}