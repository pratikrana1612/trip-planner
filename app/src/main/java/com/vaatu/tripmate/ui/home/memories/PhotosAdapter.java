package com.vaatu.tripmate.ui.home.memories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.TripPhoto;

import java.io.File;
import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {

    public interface OnPhotoClickListener {
        void onPhotoClicked(String filePath);
    }

    private List<TripPhoto> photos;
    private TripMemoriesActivity context;
    private OnPhotoClickListener listener;

    public PhotosAdapter(List<TripPhoto> photos, TripMemoriesActivity context, OnPhotoClickListener listener) {
        this.photos = photos;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TripPhoto photo = photos.get(position);
        String filePath = photo.getFilePath();
        if (filePath != null) {
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .into(holder.photoImage);
                holder.photoImage.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(filePath);
                    }
                });
                return;
            }
        }
        holder.photoImage.setImageResource(R.drawable.ic_photo_placeholder);
        holder.photoImage.setOnClickListener(null);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImage;

        ViewHolder(View itemView) {
            super(itemView);
            photoImage = itemView.findViewById(R.id.photo_image);
        }
    }
}



