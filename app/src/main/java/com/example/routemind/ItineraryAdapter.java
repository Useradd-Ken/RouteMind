package com.example.routemind;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private final List<Itinerary> itineraryList;
    private final Context context;
    private boolean isBooked = false;

    private static final int TYPE_CURRENT = 0;
    private static final int TYPE_BOOKED = 1;

    public ItineraryAdapter(Context context, List<Itinerary> itineraryList) {
        this.context = context;
        this.itineraryList = itineraryList;
    }

    public void setBooked(boolean booked) {
        this.isBooked = booked;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isBooked ? TYPE_BOOKED : TYPE_CURRENT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == TYPE_BOOKED) ? R.layout.item_itinerary : R.layout.item_itinerary_current;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Itinerary itinerary = itineraryList.get(position);
        holder.tvTitle.setText(itinerary.getTitle());
        holder.tvCategory.setText(itinerary.getCategory());
        holder.tvPrice.setText(String.format("₱%.2f", itinerary.getPrice()));
        
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(itinerary.getDescription());
        }

        String details = itinerary.getItinerary();
        List<String> validLines = getItineraryLines(details);

        if (getItemViewType(position) == TYPE_CURRENT) {
            holder.rowsContainer.removeAllViews();
            if (!validLines.isEmpty()) {
                LayoutInflater inflater = LayoutInflater.from(context);
                int limit = Math.min(validLines.size(), 3); // Preview only 3 rows
                for (int i = 0; i < limit; i++) {
                    String cleanLine = validLines.get(i);
                    View rowView = inflater.inflate(R.layout.item_activity_row, holder.rowsContainer, false);
                    
                    TextView tvTime = rowView.findViewById(R.id.tv_activity_time);
                    TextView tvActTitle = rowView.findViewById(R.id.tv_activity_title);
                    TextView tvSubtitle = rowView.findViewById(R.id.tv_activity_subtitle);
                    View lineTop = rowView.findViewById(R.id.line_top);
                    View lineBottom = rowView.findViewById(R.id.line_bottom);

                    if (i == 0) lineTop.setVisibility(View.INVISIBLE);
                    if (i == limit - 1) lineBottom.setVisibility(View.INVISIBLE);

                    if (cleanLine.toLowerCase().startsWith("day")) {
                        tvTime.setVisibility(View.INVISIBLE);
                        tvActTitle.setText(cleanLine);
                        tvActTitle.setTextSize(16);
                        tvActTitle.setTextColor(context.getResources().getColor(R.color.white));
                        rowView.findViewById(R.id.dot).setBackgroundTintList(android.content.res.ColorStateList.valueOf(context.getResources().getColor(R.color.white)));
                    } else if (cleanLine.contains("•")) {
                        String[] parts = cleanLine.split("•", 2);
                        tvTime.setText(parts[0].trim());
                        tvActTitle.setText(parts[1].trim());
                    } else {
                        tvTime.setText("--:--");
                        tvActTitle.setText(cleanLine);
                    }
                    
                    tvSubtitle.setVisibility(View.GONE);
                    holder.rowsContainer.addView(rowView);
                }
            }
        } else {
            if (holder.tvPreview != null) {
                StringBuilder preview = new StringBuilder();
                int count = 0;
                for (String l : validLines) {
                    preview.append(l).append("\n");
                    count++;
                    if (count >= 3) break;
                }
                holder.tvPreview.setText(preview.toString().trim());
                holder.tvPreview.setVisibility(preview.length() > 0 ? View.VISIBLE : View.GONE);
            }
            
            if (holder.ivIcon != null && holder.ivIcon.getVisibility() != View.GONE) {
                Glide.with(context)
                        .load(itinerary.getImageUrl())
                        .placeholder(R.drawable.routemind)
                        .into(holder.ivIcon);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TourDetailsActivity.class);
            intent.putExtra("TITLE", itinerary.getTitle());
            intent.putExtra("DESCRIPTION", itinerary.getDescription());
            intent.putExtra("ITINERARY", itinerary.getItinerary());
            intent.putExtra("IMAGE_URL", itinerary.getImageUrl());
            intent.putExtra("CATEGORY", itinerary.getCategory());
            intent.putExtra("PRICE", itinerary.getPrice());
            intent.putExtra("PRICE_BREAKDOWN", itinerary.getPriceBreakdown());
            intent.putExtra("IS_BOOKED", isBooked);
            context.startActivity(intent);
        });
    }

    private List<String> getItineraryLines(String details) {
        List<String> lines = new ArrayList<>();
        if (details == null || details.isEmpty()) return lines;

        try {
            JSONArray array = null;
            if (details.trim().startsWith("[")) {
                array = new JSONArray(details);
            } else if (details.trim().startsWith("{")) {
                JSONObject root = new JSONObject(details);
                array = root.optJSONArray("itinerary");
                if (array == null) array = root.optJSONArray("activities");
            }

            if (array != null) {
                int lastDay = -1;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    int day = obj.optInt("day", 1);
                    if (day != lastDay) {
                        lines.add("Day " + day);
                        lastDay = day;
                    }
                    String time = obj.optString("time", "--:--");
                    String act = obj.optString("activity", "");
                    if (!act.isEmpty()) {
                        lines.add(time + " • " + act);
                    }
                }
                return lines;
            }
        } catch (Exception ignored) {}

        String[] rawLines = details.split("\n");
        for (String l : rawLines) {
            if (!l.trim().isEmpty()) lines.add(l.trim());
        }
        return lines;
    }

    @Override
    public int getItemCount() {
        return itineraryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvPrice, tvPreview, tvDescription;
        ImageView ivIcon;
        LinearLayout rowsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_trip_title);
            tvCategory = itemView.findViewById(R.id.tv_trip_category);
            tvPrice = itemView.findViewById(R.id.tv_trip_price);
            tvPreview = itemView.findViewById(R.id.tv_itinerary_preview);
            tvDescription = itemView.findViewById(R.id.tv_trip_description);
            ivIcon = itemView.findViewById(R.id.iv_trip_icon);
            rowsContainer = itemView.findViewById(R.id.itinerary_rows_container);
        }
    }
}
