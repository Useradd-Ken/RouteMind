package com.example.routemind;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItineraryPageActivity extends AppCompatActivity {

    private RecyclerView rvItinerary;
    private ItineraryAdapter adapter;
    private LinearLayout layoutDaySelector;
    private List<ItineraryItem> allItems;
    private int selectedDay = 1;
    private Button btnGenerateGuide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_page);

        layoutDaySelector = findViewById(R.id.layout_day_selector);
        rvItinerary = findViewById(R.id.rv_itinerary);
        btnGenerateGuide = findViewById(R.id.btn_generate_guide);
        rvItinerary.setLayoutManager(new LinearLayoutManager(this));

        // Restore static sample data
        allItems = createSampleItinerary();

        // Initialize adapter with the first day's items
        adapter = new ItineraryAdapter(filterItemsByDay(selectedDay));
        rvItinerary.setAdapter(adapter);

        // Setup the day selector buttons
        setupDaySelector();

        btnGenerateGuide.setOnClickListener(v -> {
            ArrayList<ItineraryItem> selectedItems = new ArrayList<>();
            for (ItineraryItem item : allItems) {
                if (item.isSelected()) {
                    selectedItems.add(item);
                }
            }

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please select at least one activity", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(ItineraryPageActivity.this, TravelGuidePageActivity.class);
                intent.putExtra("selected_items", (Serializable) selectedItems);
                startActivity(intent);
            }
        });
    }

    private List<ItineraryItem> createSampleItinerary() {
        List<ItineraryItem> items = new ArrayList<>();
        // Day 1
        items.add(new ItineraryItem(1, "08:00 AM", "Free", "Magellan's Cross", "P. Burgos St, Cebu City"));
        items.add(new ItineraryItem(1, "09:00 AM", "Free", "Basilica del Santo Niño", "Cebu City"));
        items.add(new ItineraryItem(1, "10:30 AM", "₱ 30.00", "Fort San Pedro", "A. Pigafetta Street"));
        items.add(new ItineraryItem(1, "12:00 PM", "₱ 250.00", "Lunch at House of Lechon", "Acacia St, Cebu City"));
        
        // Day 2
        items.add(new ItineraryItem(2, "09:00 AM", "₱ 100.00", "Temple of Leah", "Busay, Cebu City"));
        items.add(new ItineraryItem(2, "11:00 AM", "₱ 50.00", "Sirao Garden", "Busay, Cebu City"));
        items.add(new ItineraryItem(2, "01:00 PM", "₱ 300.00", "Lunch at Tops Lookout", "Busay, Cebu City"));

        // Day 3
        items.add(new ItineraryItem(3, "08:00 AM", "₱ 1,500.00", "Oslob Whale Shark Watching", "Oslob, Cebu"));
        items.add(new ItineraryItem(3, "12:00 PM", "₱ 500.00", "Kawasan Falls", "Badian, Cebu"));
        
        return items;
    }

    private void setupDaySelector() {
        layoutDaySelector.removeAllViews();
        List<Integer> days = new ArrayList<>();
        for (ItineraryItem item : allItems) {
            if (!days.contains(item.getDay())) {
                days.add(item.getDay());
            }
        }
        days.sort(Integer::compareTo);

        for (int day : days) {
            MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 16, 0);
            btn.setLayoutParams(params);
            btn.setText("Day " + day);
            


            btn.setOnClickListener(v -> {
                selectedDay = day;
                adapter.setItineraryItems(filterItemsByDay(selectedDay));
                setupDaySelector(); // Refresh buttons to show selection
            });

            layoutDaySelector.addView(btn);
        }
    }


    private List<ItineraryItem> filterItemsByDay(int day) {
        List<ItineraryItem> filtered = new ArrayList<>();
        for (ItineraryItem item : allItems) {
            if (item.getDay() == day) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
