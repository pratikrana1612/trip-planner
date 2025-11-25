package com.vaatu.tripmate.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocalPhotoStore {

    private static final String PREF_NAME = "trip_photos_store";
    private static LocalPhotoStore instance;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    private LocalPhotoStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LocalPhotoStore getInstance(Context context) {
        if (instance == null) {
            instance = new LocalPhotoStore(context);
        }
        return instance;
    }

    private String buildKey(String tripId) {
        return "trip_photos_" + tripId;
    }

    public List<TripPhoto> getPhotos(String tripId) {
        String json = preferences.getString(buildKey(tripId), null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<TripPhoto>>() {}.getType();
        List<TripPhoto> photos = gson.fromJson(json, type);
        if (photos == null) {
            photos = new ArrayList<>();
        }
        return photos;
    }

    public void savePhoto(TripPhoto photo) {
        List<TripPhoto> photos = getPhotos(photo.getTripId());
        photos.add(photo);
        preferences.edit().putString(buildKey(photo.getTripId()), gson.toJson(photos)).apply();
    }

    public TripPhoto getFirstPhoto(String tripId) {
        List<TripPhoto> photos = getPhotos(tripId);
        if (photos.isEmpty()) {
            return null;
        }
        return photos.get(0);
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }
}




