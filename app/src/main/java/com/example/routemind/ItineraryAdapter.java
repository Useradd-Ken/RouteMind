package com.example.routemind;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ViewHolder> {

    private final List<ItineraryStep> steps;
    private boolean isEditMode = false;
    private final OnStepRemovedListener removeListener;

    // A set of vibrant, pastel colors for the cards
    private final String[] colors = {
        "#E3F2FD", // Light Blue
        "#F1F8E9", // Light Green
        "#FFF3E0", // Light Orange
        "#F3E5F5", // Light Purple
        "#E8F5E9", // Mint
        "#E0F7FA", // Cyan
        "#F9FBE7", // Lime
        "#FFFDE7", // Yellow
        "#FCE4EC", // Pink
        "#EFEBE9"  // Light Brown
    };

    public interface OnStepRemovedListener {
        void onStepRemoved(int position);
    }

    public ItineraryAdapter(List<ItineraryStep> steps, OnStepRemovedListener removeListener) {
        this.steps = steps;
        this.removeListener = removeListener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItineraryStep step = steps.get(position);
        holder.tvTime.setText(step.getTime());
        holder.tvActivity.setText(step.getActivity());
        holder.tvLocation.setText(step.getLocation());

        // Set dynamic background color
        int color = Color.parseColor(colors[position % colors.length]);
        holder.cardContainer.setCardBackgroundColor(color);

        // Show/Hide delete button in edit mode
        holder.btnRemove.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onStepRemoved(holder.getAdapterPosition());
            }
        });

        // Hide timeline line for the last item
        holder.timelineLine.setVisibility(position == steps.size() - 1 ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvActivity, tvLocation;
        View timelineLine;
        MaterialCardView cardContainer;
        ImageView btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_step_time);
            tvActivity = itemView.findViewById(R.id.tv_step_activity);
            tvLocation = itemView.findViewById(R.id.tv_step_location);
            timelineLine = itemView.findViewById(R.id.timeline_line);
            cardContainer = itemView.findViewById(R.id.cv_step_container);
            btnRemove = itemView.findViewById(R.id.btn_remove_step);
        }
    }
}
