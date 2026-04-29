package com.example.routemind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Login.db";

    public DBHelper(Context context) {
        super(context, "Login.db", null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDB) {
        MyDB.execSQL("create Table users(username TEXT primary key, email TEXT, password TEXT)");
        MyDB.execSQL("create Table destinations(name TEXT primary key)");
        MyDB.execSQL("create Table reviews(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, itinerary_id TEXT, rating REAL, review TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        
        // Seed initial data
        seedDestinations(MyDB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int i, int i1) {
        MyDB.execSQL("drop Table if exists users");
        MyDB.execSQL("drop Table if exists destinations");
        MyDB.execSQL("drop Table if exists reviews");
        onCreate(MyDB);
    }

    private void seedDestinations(SQLiteDatabase MyDB) {
        String[] initialDestinations = new String[] {
                "Manila", "Cebu", "Davao", "Palawan", "Bohol", "Boracay",
                "Baguio", "Vigan", "Siargao", "Zamboanga", "Legazpi",
                "Puerto Princesa", "Tagaytay", "Dumaguete", "Iloilo"
        };
        for (String dest : initialDestinations) {
            ContentValues cv = new ContentValues();
            cv.put("name", dest);
            MyDB.insert("destinations", null, cv);
        }
    }

    public Boolean insertData(String username, String email, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("username", username);
        contentValues.put("email", email);
        contentValues.put("password", password);
        long result = MyDB.insert("users", null, contentValues);
        return result != -1;
    }

    public Boolean checkUsername(String username) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ?", new String[]{username});
        return cursor.getCount() > 0;
    }

    public Boolean checkUsernamePassword(String username, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ? and password = ?", new String[]{username, password});
        return cursor.getCount() > 0;
    }

    public Cursor getUserData(String username) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        return MyDB.rawQuery("Select * from users where username = ?", new String[]{username});
    }

    public Cursor getAllUsers() {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        return MyDB.rawQuery("Select * from users", null);
    }

    public List<String> getAllDestinations() {
        List<String> destinations = new ArrayList<>();
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from destinations", null);
        if (cursor.moveToFirst()) {
            do {
                destinations.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return destinations;
    }

    public Boolean insertReview(String username, String itineraryId, float rating, String review) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("username", username);
        contentValues.put("itinerary_id", itineraryId);
        contentValues.put("rating", rating);
        contentValues.put("review", review);
        long result = MyDB.insert("reviews", null, contentValues);
        return result != -1;
    }

    public Boolean updateReview(String username, String itineraryId, float rating, String review) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("rating", rating);
        contentValues.put("review", review);
        long result = MyDB.update("reviews", contentValues, "username = ? and itinerary_id = ?", new String[]{username, itineraryId});
        return result != -1;
    }

    public Cursor getUserReview(String username, String itineraryId) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        return MyDB.rawQuery("Select * from reviews where username = ? and itinerary_id = ?", new String[]{username, itineraryId});
    }

    public Cursor getAllReviews() {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        return MyDB.rawQuery("Select * from reviews order by timestamp DESC", null);
    }
}