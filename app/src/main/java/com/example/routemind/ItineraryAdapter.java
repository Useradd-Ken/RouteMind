package com.example.routemind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private List<ItineraryItem> itineraryItems;

    public ItineraryAdapter(List<ItineraryItem> itineraryItems) {
        this.itineraryItems = itineraryItems;
    }

    public void setItineraryItems(List<ItineraryItem> itineraryItems) {
        this.itineraryItems = itineraryItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (itineraryItems == null || position >= itineraryItems.size()) return;
        
        ItineraryItem item = itineraryItems.get(position);
        holder.tvTime.setText(item.getTime());
        holder.tvTitle.setText(item.getTitle());
        holder.tvCost.setText(item.getCost());
        holder.tvLocation.setText(item.getLocation());
        
        // Remove listener before setting state to avoid trigger
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isSelected());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));
    }

    @Override
    public int getItemCount() {
        return itineraryItems == null ? 0 : itineraryItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvCost, tvLocation;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCost = itemView.findViewById(R.id.tv_cost);
            tvLocation = itemView.findViewById(R.id.tv_location);
            checkBox = itemView.findViewById(R.id.cb_select);
        }
    }
}