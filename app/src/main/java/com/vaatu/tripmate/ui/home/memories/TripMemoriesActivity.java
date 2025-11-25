package com.vaatu.tripmate.ui.home.memories;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.LocalPhotoStore;
import com.vaatu.tripmate.utils.TripPhoto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TripMemoriesActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final int REQUEST_CODE_PERMISSION = 1002;

    private String tripId;
    private String tripName;
    private RecyclerView photosGrid;
    private FloatingActionButton fabAddPhoto;
    private TextView emptyStateText;
    private PhotosAdapter adapter;
    private List<TripPhoto> photos = new ArrayList<>();
    private ProgressDialog progressDialog;
    private LocalPhotoStore photoStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_memories);

        tripId = getIntent().getStringExtra("tripId");
        tripName = getIntent().getStringExtra("tripName");
        String tripDestination = getIntent().getStringExtra("tripDestination");

        if (tripId == null) {
            Toast.makeText(this, "Invalid trip", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Memories – " + (tripName != null ? tripName : "Trip"));
        }

        photoStore = LocalPhotoStore.getInstance(this);

        photosGrid = findViewById(R.id.photos_grid);
        fabAddPhoto = findViewById(R.id.fab_add_photo);
        emptyStateText = findViewById(R.id.empty_state_text);

        adapter = new PhotosAdapter(photos, this, new PhotosAdapter.PhotoActionListener() {
            @Override
            public void onPhotoClicked(String filePath) {
                showImagePreview(filePath);
            }

            @Override
            public void onDeleteClicked(TripPhoto photo) {
                showDeleteConfirmation(photo);
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        photosGrid.setLayoutManager(gridLayoutManager);
        photosGrid.setAdapter(adapter);

        fabAddPhoto.setOnClickListener(v -> pickImage());

        loadPhotos();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadPhotos() {
        photos.clear();
        photos.addAll(photoStore.getPhotos(tripId));
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (photos.isEmpty()) {
            photosGrid.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            photosGrid.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void pickImage() {
        if (checkPermission()) {
            openImagePicker();
        } else {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE_PERMISSION);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied. Cannot select photos.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                uploadImage(imageUri);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        showProgressDialog("Uploading photo...");

        try {
            String photoId = UUID.randomUUID().toString();
            String destinationPath = copyImageToInternalStorage(imageUri, photoId);
            if (destinationPath == null) {
                hideProgressDialog();
                Toast.makeText(this, "Unable to save image", Toast.LENGTH_SHORT).show();
                return;
            }

            savePhotoMetadata(photoId, destinationPath);
        } catch (IOException e) {
            hideProgressDialog();
            Log.e("Upload", "Error saving image locally", e);
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private String copyImageToInternalStorage(Uri sourceUri, String photoId) throws IOException {
        String mimeType = getContentResolver().getType(sourceUri);
        String extFromMime = mimeType != null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) : null;
        String fileExtension = extFromMime != null ? "." + extFromMime : ".jpg";

        File tripDir = new File(getFilesDir(), "trip_photos/" + tripId);
        if (!tripDir.exists() && !tripDir.mkdirs()) {
            return null;
        }

        File destination = new File(tripDir, photoId + fileExtension);
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null) {
                return null;
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
        return destination.getAbsolutePath();
    }

    private void savePhotoMetadata(String photoId, String filePath) {
        TripPhoto photo = new TripPhoto();
        photo.setId(photoId);
        photo.setTripId(tripId);
        photo.setFilePath(filePath);
        photo.setCreatedAt(System.currentTimeMillis());

        photoStore.savePhoto(photo);
        hideProgressDialog();
        Toast.makeText(this, "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
        loadPhotos();
    }

    private void showImagePreview(String path) {
        ImagePreviewDialog.newInstance(path).show(getSupportFragmentManager(), "image_preview");
    }

    private void showDeleteConfirmation(TripPhoto photo) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo_confirmation_title)
                .setMessage(R.string.delete_photo_confirmation_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePhoto(photo))
                .show();
    }

    private void deletePhoto(TripPhoto photo) {
        boolean removed = photoStore.deletePhoto(tripId, photo.getId());
        deletePhotoFile(photo.getFilePath());
        if (removed) {
            Toast.makeText(this, R.string.photo_deleted, Toast.LENGTH_SHORT).show();
            loadPhotos();
        } else {
            Toast.makeText(this, R.string.delete_photo_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void deletePhotoFile(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (file.exists() && !file.delete()) {
            Log.w("TripMemories", "Unable to delete photo file: " + path);
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}


