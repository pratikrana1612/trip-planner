package com.vaatu.tripmate.data.remote.network;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.vaatu.tripmate.utils.TripModel;
import com.vaatu.tripmate.utils.ai.AiTripPlan;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AiTripPlanFirebaseService {

    private static final String DB_URL = "https://trip-mate-7fac8-default-rtdb.firebaseio.com/";
    private static final String TAG = "AiTripPlanFirebase";
    
    private final FirebaseDatabase database;
    private final FirebaseAuth mAuth;
    private final Gson gson;

    public AiTripPlanFirebaseService() {
        database = FirebaseDatabase.getInstance(DB_URL);
        mAuth = FirebaseAuth.getInstance();
        gson = new Gson();
    }

    /**
     * Generate a unique tripId from trip properties
     */
    public static String generateTripId(TripModel trip) {
        if (trip == null) {
            return null;
        }
        
        // Create a unique identifier from trip properties
        String uniqueString = (trip.getTripname() != null ? trip.getTripname() : "") + "|" +
                (trip.getStartloc() != null ? trip.getStartloc() : "") + "|" +
                (trip.getEndloc() != null ? trip.getEndloc() : "") + "|" +
                (trip.getDate() != null ? trip.getDate() : "") + "|" +
                (trip.getTime() != null ? trip.getTime() : "");
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(uniqueString.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash code
            return String.valueOf(uniqueString.hashCode());
        }
    }

    /**
     * Get the Firebase path for AI trip plans
     */
    private DatabaseReference getAiTripPlanRef(String tripId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(tripId)) {
            return null;
        }
        return database.getReference()
                .child("ai_trip_plans")
                .child(currentUser.getUid())
                .child(tripId);
    }

    /**
     * Load AI trip plan from Firebase
     */
    public void loadAiTripPlan(String tripId, AiTripPlanCallback callback) {
        if (TextUtils.isEmpty(tripId)) {
            callback.onError("Invalid trip ID");
            return;
        }

        DatabaseReference planRef = getAiTripPlanRef(tripId);
        if (planRef == null) {
            callback.onError("User not authenticated");
            return;
        }

        planRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        AiTripPlan plan = dataSnapshot.getValue(AiTripPlan.class);
                        if (plan != null && !plan.isEmpty()) {
                            Log.d(TAG, "Loaded cached AI plan for trip: " + tripId);
                            callback.onSuccess(plan);
                        } else {
                            Log.d(TAG, "Cached plan exists but is empty for trip: " + tripId);
                            callback.onPlanNotFound();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing cached plan", e);
                        callback.onError("Failed to parse cached plan");
                    }
                } else {
                    Log.d(TAG, "No cached plan found for trip: " + tripId);
                    callback.onPlanNotFound();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading cached plan", databaseError.toException());
                callback.onError("Failed to load cached plan: " + databaseError.getMessage());
            }
        });
    }

    /**
     * Save AI trip plan to Firebase
     */
    public void saveAiTripPlan(String tripId, AiTripPlan plan) {
        if (TextUtils.isEmpty(tripId) || plan == null || plan.isEmpty()) {
            Log.w(TAG, "Cannot save: invalid tripId or empty plan");
            return;
        }

        DatabaseReference planRef = getAiTripPlanRef(tripId);
        if (planRef == null) {
            Log.w(TAG, "Cannot save: user not authenticated");
            return;
        }

        // Set createdAt timestamp
        plan.setCreatedAt(System.currentTimeMillis());

        planRef.setValue(plan)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully saved AI plan for trip: " + tripId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save AI plan", e);
                });
    }

    /**
     * Delete cached AI trip plan
     */
    public void deleteAiTripPlan(String tripId) {
        if (TextUtils.isEmpty(tripId)) {
            return;
        }

        DatabaseReference planRef = getAiTripPlanRef(tripId);
        if (planRef != null) {
            planRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully deleted AI plan for trip: " + tripId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete AI plan", e);
                    });
        }
    }

    public interface AiTripPlanCallback {
        void onSuccess(AiTripPlan plan);
        void onPlanNotFound();
        void onError(String errorMessage);
    }
}

