package com.vaatu.tripmate.ui.home.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vaatu.tripmate.R;
import com.vaatu.tripmate.data.remote.network.AiTripPlanFirebaseService;
import com.vaatu.tripmate.data.remote.network.GeminiTravelService;
import com.vaatu.tripmate.utils.TripModel;
import com.vaatu.tripmate.utils.ai.AiTripPlan;

import java.util.Collections;
import java.util.List;

public class AiTripPlanActivity extends AppCompatActivity {

    private static final String TAG = "AiTripPlanActivity";
    public static final String EXTRA_TRIP = "com.vaatu.tripmate.extra.TRIP_MODEL";

    private View loadingGroup;
    private View contentGroup;
    private View errorGroup;
    private TextView errorText;
    private TextView tripTitleText;
    private TextView tripRouteText;
    private TextView tripDateText;
    private TextView tripNotesText;
    private TextView generalTipsText;
    private RecyclerView packingRecyclerView;
    private RecyclerView dayPlanRecyclerView;

    private PackingListAdapter packingListAdapter;
    private DayPlanAdapter dayPlanAdapter;
    private GeminiTravelService geminiTravelService;
    private AiTripPlanFirebaseService aiTripPlanFirebaseService;
    private TripModel tripModel;
    private String tripId;
    private boolean isRefreshing = false;
    private boolean isApiCallInProgress = false;

    public static void start(Context context, TripModel tripModel) {
        Intent intent = new Intent(context, AiTripPlanActivity.class);
        intent.putExtra(EXTRA_TRIP, tripModel);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_trip_plan);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        geminiTravelService = new GeminiTravelService();
        aiTripPlanFirebaseService = new AiTripPlanFirebaseService();
        bindViews();
        initLists();

        tripModel = (TripModel) getIntent().getSerializableExtra(EXTRA_TRIP);
        if (tripModel == null) {
            Toast.makeText(this, R.string.ai_trip_plan_error_missing_trip, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Generate unique tripId from trip properties
        tripId = AiTripPlanFirebaseService.generateTripId(tripModel);
        android.util.Log.d(TAG, "Generated tripId: " + tripId + " for trip: " + 
            (tripModel != null ? tripModel.getTripname() : "null"));
        
        populateTripSummary(tripModel);
        loadAiPlan();
    }

    private void bindViews() {
        loadingGroup = findViewById(R.id.loadingGroup);
        contentGroup = findViewById(R.id.contentGroup);
        errorGroup = findViewById(R.id.errorGroup);
        errorText = findViewById(R.id.errorText);
        tripTitleText = findViewById(R.id.tripTitleText);
        tripRouteText = findViewById(R.id.tripRouteText);
        tripDateText = findViewById(R.id.tripDateText);
        tripNotesText = findViewById(R.id.tripNotesText);
        generalTipsText = findViewById(R.id.generalTipsText);
        packingRecyclerView = findViewById(R.id.packingRecyclerView);
        dayPlanRecyclerView = findViewById(R.id.dayPlanRecyclerView);

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            isRefreshing = false; // Reset refresh flag for retry
            loadAiPlan();
        });
    }

    private void initLists() {
        packingRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        packingRecyclerView.setNestedScrollingEnabled(false);
        packingListAdapter = new PackingListAdapter();
        packingRecyclerView.setAdapter(packingListAdapter);

        dayPlanRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dayPlanRecyclerView.setNestedScrollingEnabled(false);
        dayPlanAdapter = new DayPlanAdapter();
        dayPlanRecyclerView.setAdapter(dayPlanAdapter);
    }

    private void populateTripSummary(TripModel trip) {
        tripTitleText.setText(safe(trip.getTripname()));
        tripRouteText.setText(getString(R.string.ai_trip_plan_route_template,
                safe(trip.getStartloc()), safe(trip.getEndloc())));
        tripDateText.setText(getString(R.string.ai_trip_plan_date_template,
                safe(trip.getDate()), safe(trip.getTime())));

        List<String> notes = trip.getNotes();
        if (notes != null && !notes.isEmpty()) {
            tripNotesText.setVisibility(View.VISIBLE);
            tripNotesText.setText(getString(R.string.ai_trip_plan_notes_template, TextJoiner.join(notes)));
        } else {
            tripNotesText.setVisibility(View.GONE);
        }
    }

    private String safe(String value) {
        return value == null ? getString(R.string.ai_trip_plan_value_unknown) : value;
    }

    /**
     * Load AI plan - first check cache, then generate if not found
     */
    private void loadAiPlan() {
        if (isRefreshing) {
            // Force refresh - skip cache
            fetchAiPlanFromGemini();
            return;
        }

        showLoading();
        
        // First, try to load from cache
        android.util.Log.d(TAG, "Checking cache for trip: " + tripId);
        aiTripPlanFirebaseService.loadAiTripPlan(tripId, new AiTripPlanFirebaseService.AiTripPlanCallback() {
            @Override
            public void onSuccess(AiTripPlan plan) {
                // Found cached plan - display it
                runOnUiThread(() -> {
                    android.util.Log.d(TAG, "Cache hit! Displaying cached plan");
                    showContent();
                    updateUi(plan);
                });
            }

            @Override
            public void onPlanNotFound() {
                // No cached plan - generate new one
                android.util.Log.d(TAG, "Cache miss, calling Gemini API");
                fetchAiPlanFromGemini();
            }

            @Override
            public void onError(String errorMessage) {
                // Error loading cache - try generating new plan anyway
                android.util.Log.w("AiTripPlan", "Cache load error: " + errorMessage + ", will try API");
                fetchAiPlanFromGemini();
            }
        });
    }

    /**
     * Fetch AI plan from Gemini API and save to cache
     */
    private void fetchAiPlanFromGemini() {
        // Prevent multiple simultaneous API calls
        if (isApiCallInProgress) {
            android.util.Log.w(TAG, "API call already in progress, skipping duplicate request");
            return;
        }
        
        isApiCallInProgress = true;
        showLoading();
        android.util.Log.d(TAG, "Calling Gemini API for trip: " + tripId);
        
        geminiTravelService.generatePlan(tripModel, new GeminiTravelService.GeminiCallback() {
            @Override
            public void onSuccess(AiTripPlan aiTripPlan) {
                runOnUiThread(() -> {
                    android.util.Log.d(TAG, "Successfully received plan from Gemini API");
                    showContent();
                    updateUi(aiTripPlan);
                    
                    // Save to Firebase cache
                    if (tripId != null) {
                        android.util.Log.d(TAG, "Saving plan to Firebase cache");
                        aiTripPlanFirebaseService.saveAiTripPlan(tripId, aiTripPlan);
                    }
                    
                    // Reset flags
                    isRefreshing = false;
                    isApiCallInProgress = false;
                });
            }

            @Override
            public void onError(String errorMessage) {
                android.util.Log.e(TAG, "Gemini API error: " + errorMessage);
                android.util.Log.e(TAG, "Error details - tripId: " + tripId + ", isRefreshing: " + isRefreshing);
                runOnUiThread(() -> {
                    // If it's a rate limit error and we're not refreshing, try to load cached plan
                    if (errorMessage != null && errorMessage.contains("RATE_LIMIT_EXCEEDED") && !isRefreshing) {
                        android.util.Log.w(TAG, "Rate limit hit, attempting to load cached plan as fallback");
                        // Try to load from cache as fallback
                        aiTripPlanFirebaseService.loadAiTripPlan(tripId, new AiTripPlanFirebaseService.AiTripPlanCallback() {
                            @Override
                            public void onSuccess(AiTripPlan plan) {
                                runOnUiThread(() -> {
                                    android.util.Log.d(TAG, "Successfully loaded cached plan as fallback");
                                    showContent();
                                    updateUi(plan);
                                    Toast.makeText(AiTripPlanActivity.this, 
                                        "Using cached plan due to rate limit. Please try refreshing later.", 
                                        Toast.LENGTH_LONG).show();
                                });
                            }

                            @Override
                            public void onPlanNotFound() {
                                runOnUiThread(() -> {
                                    android.util.Log.w(TAG, "No cached plan found as fallback");
                                    showError("Rate limit exceeded. No cached plan available. Please try again later.");
                                });
                            }

                            @Override
                            public void onError(String cacheError) {
                                runOnUiThread(() -> {
                                    android.util.Log.e(TAG, "Error loading cached plan: " + cacheError);
                                    showError("Rate limit exceeded. Unable to load cached plan. Please try again later.");
                                });
                            }
                        });
                    } else {
                        android.util.Log.e(TAG, "Showing error to user: " + errorMessage);
                        showError(errorMessage);
                    }
                    // Reset flags even on error
                    isRefreshing = false;
                    isApiCallInProgress = false;
                });
            }
        });
    }

    /**
     * Refresh AI plan - force regenerate
     */
    private void refreshAiPlan() {
        isRefreshing = true;
        loadAiPlan();
    }

    private void updateUi(AiTripPlan aiTripPlan) {
        if (aiTripPlan == null) {
            showError(getString(R.string.ai_trip_plan_error_generic));
            return;
        }

        if (aiTripPlan.getPackingList() != null) {
            packingListAdapter.submitList(aiTripPlan.getPackingList());
        } else {
            packingListAdapter.submitList(Collections.emptyList());
        }

        if (aiTripPlan.getDayPlan() != null) {
            dayPlanAdapter.submitList(aiTripPlan.getDayPlan());
        } else {
            dayPlanAdapter.submitList(Collections.emptyList());
        }

        List<String> tips = aiTripPlan.getGeneralTips();
        if (tips != null && !tips.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String tip : tips) {
                builder.append("- ").append(tip).append("\n");
            }
            generalTipsText.setText(builder.toString().trim());
        } else {
            generalTipsText.setText(R.string.ai_trip_plan_no_tips);
        }
    }

    private void showLoading() {
        loadingGroup.setVisibility(View.VISIBLE);
        contentGroup.setVisibility(View.GONE);
        errorGroup.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingGroup.setVisibility(View.GONE);
        errorGroup.setVisibility(View.GONE);
        contentGroup.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        loadingGroup.setVisibility(View.GONE);
        contentGroup.setVisibility(View.GONE);
        errorGroup.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(message)) {
            errorText.setText(R.string.ai_trip_plan_error_generic);
        } else {
            errorText.setText(message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.ai_trip_plan_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_refresh_ai_plan) {
            refreshAiPlan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class TextJoiner {
        static String join(List<String> values) {
            if (values == null || values.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                builder.append(values.get(i));
                if (i < values.size() - 1) {
                    builder.append(", ");
                }
            }
            return builder.toString();
        }
    }

}

