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
import com.vaatu.tripmate.data.remote.network.GeminiTravelService;
import com.vaatu.tripmate.utils.TripModel;
import com.vaatu.tripmate.utils.ai.AiTripPlan;

import java.util.Collections;
import java.util.List;

public class AiTripPlanActivity extends AppCompatActivity {

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
    private TripModel tripModel;

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
        bindViews();
        initLists();

        tripModel = (TripModel) getIntent().getSerializableExtra(EXTRA_TRIP);
        if (tripModel == null) {
            Toast.makeText(this, R.string.ai_trip_plan_error_missing_trip, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        populateTripSummary(tripModel);
        fetchAiPlan();
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
        retryButton.setOnClickListener(v -> fetchAiPlan());
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

    private void fetchAiPlan() {
        showLoading();
        geminiTravelService.generatePlan(tripModel, new GeminiTravelService.GeminiCallback() {
            @Override
            public void onSuccess(AiTripPlan aiTripPlan) {
                runOnUiThread(() -> {
                    showContent();
                    updateUi(aiTripPlan);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> showError(errorMessage));
            }
        });
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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

