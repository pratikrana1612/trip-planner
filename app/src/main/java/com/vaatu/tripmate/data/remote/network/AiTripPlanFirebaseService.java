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
        Log.d(TAG, "loadAiTripPlan called with tripId: " + tripId);
        
        if (TextUtils.isEmpty(tripId)) {
            Log.e(TAG, "Invalid trip ID provided");
            callback.onError("Invalid trip ID");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            callback.onError("User not authenticated");
            return;
        }
        
        Log.d(TAG, "User authenticated: " + currentUser.getUid());

        DatabaseReference planRef = getAiTripPlanRef(tripId);
        if (planRef == null) {
            Log.e(TAG, "Failed to get Firebase reference");
            callback.onError("Failed to get Firebase reference");
            return;
        }

        Log.d(TAG, "Loading AI plan from Firebase. Path: ai_trip_plans/" + mAuth.getCurrentUser().getUid() + "/" + tripId);
        
        planRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Firebase data snapshot received. Exists: " + dataSnapshot.exists());
                
                if (dataSnapshot.exists()) {
                    try {
                        // Log the raw data to see what Firebase returns
                        Object rawValue = dataSnapshot.getValue();
                        Log.d(TAG, "Raw Firebase data: " + (rawValue != null ? rawValue.toString() : "null"));
                        
                        AiTripPlan plan = dataSnapshot.getValue(AiTripPlan.class);
                        Log.d(TAG, "Deserialized plan: " + (plan != null ? "not null" : "null"));
                        
                        if (plan != null) {
                            Log.d(TAG, "Plan details - PackingList size: " + 
                                (plan.getPackingList() != null ? plan.getPackingList().size() : 0) +
                                ", DayPlan size: " + 
                                (plan.getDayPlan() != null ? plan.getDayPlan().size() : 0) +
                                ", Tips size: " + 
                                (plan.getGeneralTips() != null ? plan.getGeneralTips().size() : 0));
                        }
                        
                        if (plan != null && !plan.isEmpty()) {
                            Log.d(TAG, "Successfully loaded cached AI plan for trip: " + tripId);
                            callback.onSuccess(plan);
                        } else {
                            Log.w(TAG, "Cached plan exists but is empty or null for trip: " + tripId);
                            callback.onPlanNotFound();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing cached plan", e);
                        e.printStackTrace();
                        callback.onError("Failed to parse cached plan: " + e.getMessage());
                    }
                } else {
                    Log.d(TAG, "No cached plan found in Firebase for trip: " + tripId);
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
            Log.w(TAG, "Cannot save: invalid tripId or empty plan. tripId: " + tripId + ", plan null: " + (plan == null));
            return;
        }

        DatabaseReference planRef = getAiTripPlanRef(tripId);
        if (planRef == null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Log.w(TAG, "Cannot save: user not authenticated. Current user: " + (currentUser != null ? currentUser.getUid() : "null"));
            return;
        }

        // Set createdAt timestamp
        plan.setCreatedAt(System.currentTimeMillis());
        
        Log.d(TAG, "Saving AI plan to Firebase. Path: ai_trip_plans/" + mAuth.getCurrentUser().getUid() + "/" + tripId);
        Log.d(TAG, "Plan details - PackingList size: " + 
            (plan.getPackingList() != null ? plan.getPackingList().size() : 0) +
            ", DayPlan size: " + 
            (plan.getDayPlan() != null ? plan.getDayPlan().size() : 0) +
            ", Tips size: " + 
            (plan.getGeneralTips() != null ? plan.getGeneralTips().size() : 0));

        planRef.setValue(plan)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully saved AI plan for trip: " + tripId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save AI plan", e);
                    e.printStackTrace();
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

