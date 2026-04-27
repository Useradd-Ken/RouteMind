package com.example.routemind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private List<ItineraryItem> itineraryItems;

    public ItineraryAdapter(List<ItineraryItem> itineraryItems) {
        this.itineraryItems = itineraryItems;
    }

    public void setItineraryItems(List<ItineraryItem> items) {
        this.itineraryItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItineraryItem item = itineraryItems.get(position);
        holder.tvTime.setText(item.getTime());
        holder.tvTitle.setText(item.getActivityTitle());
        holder.tvLocation.setText(item.getLocation());
        holder.tvCost.setText(item.getCost());

        // Update button text and card style based on selection state
        updateUI(holder, item.isSelected());

        holder.btnSelect.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            updateUI(holder, item.isSelected());
        });
    }

    private void updateUI(ViewHolder holder, boolean isSelected) {
        if (isSelected) {
            holder.btnSelect.setText("Deselect");
            holder.cardView.setStrokeWidth(4);
            holder.cardView.setStrokeColor(ContextCompat.getColor(holder.cardView.getContext(), R.color.menu_background));
        } else {
            holder.btnSelect.setText("Select");
            holder.cardView.setStrokeWidth(0);
        }
    }

    @Override
    public int getItemCount() {
        return itineraryItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvLocation, tvCost;
        Button btnSelect;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvTitle = itemView.findViewById(R.id.tv_activity_title);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvCost = itemView.findViewById(R.id.tv_cost);
            btnSelect = itemView.findViewById(R.id.btn_select);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}
