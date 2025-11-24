package com.vaatu.tripmate.ui.home.ai;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.ai.DayPlan;
import com.vaatu.tripmate.utils.ai.PlaceToVisit;

import java.util.ArrayList;
import java.util.List;

class DayPlanAdapter extends RecyclerView.Adapter<DayPlanAdapter.DayPlanViewHolder> {

    private final List<DayPlan> dayPlans = new ArrayList<>();

    @NonNull
    @Override
    public DayPlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_plan, parent, false);
        return new DayPlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayPlanViewHolder holder, int position) {
        DayPlan dayPlan = dayPlans.get(position);
        String title = TextUtils.isEmpty(dayPlan.getTitle()) ? holder.itemView.getContext().getString(R.string.ai_trip_plan_value_unknown) : dayPlan.getTitle();
        holder.titleText.setText(holder.itemView.getContext().getString(
                R.string.ai_trip_plan_day_title_template, dayPlan.getDayNumber(), title));

        String description = TextUtils.isEmpty(dayPlan.getDescription())
                ? holder.itemView.getContext().getString(R.string.ai_trip_plan_day_no_description)
                : dayPlan.getDescription();
        holder.descriptionText.setText(description);

        holder.placesText.setText(buildPlacesText(holder.itemView.getContext(), dayPlan.getPlacesToVisit()));
    }

    private CharSequence buildPlacesText(android.content.Context context, List<PlaceToVisit> places) {
        if (places == null || places.isEmpty()) {
            return context.getString(R.string.ai_trip_plan_day_no_places);
        }
        StringBuilder builder = new StringBuilder();
        for (PlaceToVisit place : places) {
            builder.append("- ");
            if (!TextUtils.isEmpty(place.getTimeOfDay())) {
                builder.append(place.getTimeOfDay()).append(": ");
            }
            if (!TextUtils.isEmpty(place.getName())) {
                builder.append(place.getName());
            } else {
                builder.append(context.getString(R.string.ai_trip_plan_value_unknown));
            }
            if (!TextUtils.isEmpty(place.getNotes())) {
                builder.append(" (").append(place.getNotes()).append(")");
            }
            builder.append("\n");
        }
        return builder.toString().trim();
    }

    @Override
    public int getItemCount() {
        return dayPlans.size();
    }

    void submitList(List<DayPlan> newItems) {
        dayPlans.clear();
        if (newItems != null) {
            dayPlans.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    static class DayPlanViewHolder extends RecyclerView.ViewHolder {

        final TextView titleText;
        final TextView descriptionText;
        final TextView placesText;

        DayPlanViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.dayTitleText);
            descriptionText = itemView.findViewById(R.id.dayDescriptionText);
            placesText = itemView.findViewById(R.id.dayPlacesText);
        }
    }
}

