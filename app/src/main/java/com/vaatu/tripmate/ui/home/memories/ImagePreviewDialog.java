package com.vaatu.tripmate.ui.home.memories;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.vaatu.tripmate.R;

import java.io.File;

public class ImagePreviewDialog extends DialogFragment {

    private static final String ARG_PATH = "image_path";

    public static ImagePreviewDialog newInstance(String path) {
        ImagePreviewDialog dialog = new ImagePreviewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_preview, null, false);
        ImageView imageView = view.findViewById(R.id.preview_image);

        String path = getArguments() != null ? getArguments().getString(ARG_PATH) : null;
        if (path != null) {
            File imageFile = new File(path);
            Glide.with(requireContext())
                    .load(imageFile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .placeholder(R.drawable.ic_photo_placeholder)
                    .into(imageView);
        }

        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.black);
        }
        view.setOnClickListener(v -> dismiss());
        return dialog;
    }
}



