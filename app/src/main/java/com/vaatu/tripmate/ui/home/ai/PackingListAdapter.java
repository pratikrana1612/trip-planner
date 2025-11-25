package com.vaatu.tripmate.ui.home.ai;

import android.graphics.Paint;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.ai.PackingItem;

import java.util.ArrayList;
import java.util.List;

class PackingListAdapter extends RecyclerView.Adapter<PackingListAdapter.PackingViewHolder> {

    private final List<PackingItem> items = new ArrayList<>();
    private final SparseBooleanArray checkedStates = new SparseBooleanArray();

    @NonNull
    @Override
    public PackingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_packing_list, parent, false);
        return new PackingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackingViewHolder holder, int position) {
        PackingItem item = items.get(position);
        holder.nameText.setText(TextUtils.isEmpty(item.getItem())
                ? holder.itemView.getContext().getString(R.string.ai_trip_plan_value_unknown)
                : item.getItem());
        String category = TextUtils.isEmpty(item.getCategory())
                ? holder.itemView.getContext().getString(R.string.ai_trip_plan_value_unknown)
                : item.getCategory();
        holder.categoryText.setText(holder.itemView.getContext().getString(
                R.string.ai_trip_plan_item_category_template, category));
        String reason = TextUtils.isEmpty(item.getReason())
                ? holder.itemView.getContext().getString(R.string.ai_trip_plan_day_no_description)
                : item.getReason();
        holder.reasonText.setText(holder.itemView.getContext().getString(
                R.string.ai_trip_plan_item_reason_template, reason));

        holder.itemCheckBox.setOnCheckedChangeListener(null);
        boolean isChecked = checkedStates.get(position, false);
        holder.itemCheckBox.setChecked(isChecked);
        applyCheckedState(holder, isChecked);

        holder.itemCheckBox.setOnCheckedChangeListener((buttonView, checked) -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                checkedStates.put(adapterPosition, checked);
                applyCheckedState(holder, checked);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.itemCheckBox.performClick());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void submitList(List<PackingItem> newItems) {
        items.clear();
        checkedStates.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    private void applyCheckedState(PackingViewHolder holder, boolean isChecked) {
        if (isChecked) {
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.nameText.setAlpha(0.6f);
        } else {
            holder.nameText.setPaintFlags(holder.nameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.nameText.setAlpha(1f);
        }
        holder.categoryText.setAlpha(isChecked ? 0.5f : 1f);
        holder.reasonText.setAlpha(isChecked ? 0.6f : 1f);
    }

    static class PackingViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView categoryText;
        final TextView reasonText;
        final CheckBox itemCheckBox;

        PackingViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.itemNameText);
            categoryText = itemView.findViewById(R.id.itemCategoryText);
            reasonText = itemView.findViewById(R.id.itemReasonText);
            itemCheckBox = itemView.findViewById(R.id.itemCheckBox);
        }
    }
}
