package com.vaatu.tripmate.ui.home.memories;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.LocalPhotoStore;
import com.vaatu.tripmate.utils.TripModel;
import com.vaatu.tripmate.utils.TripPhoto;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MemoriesAdapter extends RecyclerView.Adapter<MemoriesAdapter.ViewHolder> {

    private List<TripModel> trips;
    private Map<TripModel, String> tripKeyMap;
    private Context context;
    private LocalPhotoStore photoStore;

    public MemoriesAdapter(List<TripModel> trips, Map<TripModel, String> tripKeyMap, Context context) {
        this.trips = trips;
        this.tripKeyMap = tripKeyMap;
        this.context = context;
        this.photoStore = LocalPhotoStore.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memories_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TripModel trip = trips.get(position);
        String tripKey = tripKeyMap.get(trip);

        holder.tripName.setText(trip.getTripname());
        holder.tripDestination.setText(trip.getEndloc());
        holder.tripDate.setText(trip.getDate());

        // Load thumbnail if available
        loadThumbnail(tripKey, holder.thumbnail);

        holder.cardView.setOnClickListener(v -> {
            if (context instanceof MemoriesActivity) {
                ((MemoriesActivity) context).onTripClicked(tripKey, trip);
            }
        });
    }

    private void loadThumbnail(String tripId, ImageView imageView) {
        // Set placeholder first
        imageView.setImageResource(R.drawable.ic_photo_placeholder);

        TripPhoto photo = photoStore.getFirstPhoto(tripId);
        if (photo != null && photo.getFilePath() != null) {
            File imageFile = new File(photo.getFilePath());
            if (imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .into(imageView);
            }
        }
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView thumbnail;
        TextView tripName;
        TextView tripDestination;
        TextView tripDate;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            tripName = itemView.findViewById(R.id.trip_name);
            tripDestination = itemView.findViewById(R.id.trip_destination);
            tripDate = itemView.findViewById(R.id.trip_date);
        }
    }
}

