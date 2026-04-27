package com.example.routemind;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TravelGuidePageActivity extends AppCompatActivity {

    private RecyclerView rvGuide;
    private GuideAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_guide_page);

        rvGuide = findViewById(R.id.rv_guide);
        rvGuide.setLayoutManager(new LinearLayoutManager(this));

        // Get the selected activities from the intent
        List<ItineraryItem> selectedItems = (List<ItineraryItem>) getIntent().getSerializableExtra("selected_items");
        
        if (selectedItems != null && !selectedItems.isEmpty()) {
            setupRecyclerView(selectedItems);
        }
    }

    private void setupRecyclerView(List<ItineraryItem> items) {
        // Sort items by day
        Collections.sort(items, new Comparator<ItineraryItem>() {
            @Override
            public int compare(ItineraryItem o1, ItineraryItem o2) {
                return Integer.compare(o1.getDay(), o2.getDay());
            }
        });

        List<Object> displayList = new ArrayList<>();
        int currentDay = -1;

        for (ItineraryItem item : items) {
            if (item.getDay() != currentDay) {
                currentDay = item.getDay();
                displayList.add("Day " + currentDay);
            }
            displayList.add(item);
        }

        adapter = new GuideAdapter(displayList);
        rvGuide.setAdapter(adapter);
    }
}
