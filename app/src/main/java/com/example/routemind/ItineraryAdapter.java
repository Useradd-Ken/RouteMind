package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private final List<Itinerary> itineraryList;
    private final Context context;

    public ItineraryAdapter(Context context, List<Itinerary> itineraryList) {
        this.context = context;
        this.itineraryList = itineraryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Itinerary itinerary = itineraryList.get(position);
        holder.tvTitle.setText(itinerary.getTitle());
        holder.tvDate.setText(itinerary.getCategory());
        holder.tvStatus.setText(String.format("₱%.2f", itinerary.getPrice()));
        
        Glide.with(context)
                .load(itinerary.getImageUrl())
                .placeholder(R.drawable.routemind)
                .into(holder.ivIcon);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TourDetailsActivity.class);
            intent.putExtra("TITLE", itinerary.getTitle());
            intent.putExtra("DESCRIPTION", itinerary.getDescription());
            intent.putExtra("ITINERARY", itinerary.getItinerary());
            intent.putExtra("IMAGE_URL", itinerary.getImageUrl());
            intent.putExtra("CATEGORY", itinerary.getCategory());
            intent.putExtra("PRICE", itinerary.getPrice());
            intent.putExtra("PRICE_BREAKDOWN", itinerary.getPriceBreakdown());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itineraryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_trip_title);
            tvDate = itemView.findViewById(R.id.tv_trip_date);
            tvStatus = itemView.findViewById(R.id.tv_trip_status);
            ivIcon = itemView.findViewById(R.id.iv_trip_icon);
        }
    }
}
