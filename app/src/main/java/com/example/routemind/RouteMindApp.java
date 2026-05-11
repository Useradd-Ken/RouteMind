package com.example.routemind;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class RouteMindApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Firebase once for the entire app
        FirebaseApp.initializeApp(this);
    }
}
