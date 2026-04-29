package com.example.routemind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

public class FinalItineraryAdapter extends RecyclerView.Adapter<FinalItineraryAdapter.DayViewHolder> {

    private List<Integer> days;
    private Map<Integer, List<ItineraryItem>> groupedItems;

    public FinalItineraryAdapter(List<ItineraryItem> items) {
        groupedItems = new TreeMap<>();
        for (ItineraryItem item : items) {
            if (!groupedItems.containsKey(item.getDay())) {
                groupedItems.put(item.getDay(), new ArrayList<>());
            }
            groupedItems.get(item.getDay()).add(item);
        }
        days = new ArrayList<>(groupedItems.keySet());
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_final_day_card, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        int day = days.get(position);
        holder.tvDayTitle.setText("Day " + day);
        
        holder.llActivitiesContainer.removeAllViews();
        List<ItineraryItem> activities = groupedItems.get(day);
        
        if (activities != null) {
            for (ItineraryItem activity : activities) {
                View row = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_final_activity_row, holder.llActivitiesContainer, false);
                
                TextView tvTime = row.findViewById(R.id.tv_activity_time);
                TextView tvTitle = row.findViewById(R.id.tv_activity_title);
                TextView tvLocation = row.findViewById(R.id.tv_activity_location);
                
                tvTime.setText(activity.getTime());
                tvTitle.setText(activity.getTitle());
                tvLocation.setText(activity.getLocation());
                
                holder.llActivitiesContainer.addView(row);
            }
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayTitle;
        LinearLayout llActivitiesContainer;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayTitle = itemView.findViewById(R.id.tv_day_title);
            llActivitiesContainer = itemView.findViewById(R.id.ll_activities_container);
        }
    }
}
