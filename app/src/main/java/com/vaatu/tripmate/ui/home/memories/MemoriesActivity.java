package com.vaatu.tripmate.ui.home.memories;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vaatu.tripmate.R;
import com.vaatu.tripmate.utils.TripModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoriesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mTripsRef;
    private List<TripModel> tripDetails = new ArrayList<>();
    private Map<TripModel, String> tripKeyMap = new HashMap<>();
    private MemoriesAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memories);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Memories");
        }

        String dbUrl = "https://trip-mate-7fac8-default-rtdb.firebaseio.com/";
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mTripsRef = FirebaseDatabase.getInstance(dbUrl).getReference()
                .child("trip-mate").child(currentUser.getUid()).child("historytrips");

        recyclerView = findViewById(R.id.recycler);
        emptyStateText = findViewById(R.id.empty_state_text);

        adapter = new MemoriesAdapter(tripDetails, tripKeyMap, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        loadTrips();
    }

    private void loadTrips() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tripDetails.clear();
                tripKeyMap.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    TripModel trip = ds.getValue(TripModel.class);
                    if (trip != null) {
                        tripDetails.add(trip);
                        tripKeyMap.put(trip, ds.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Database", "loadTrips:onCancelled", databaseError.toException());
            }
        };
        mTripsRef.addValueEventListener(postListener);
    }

    private void updateEmptyState() {
        if (tripDetails.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    public void onTripClicked(String tripKey, TripModel trip) {
        Intent intent = new Intent(this, TripMemoriesActivity.class);
        intent.putExtra("tripId", tripKey);
        intent.putExtra("tripName", trip.getTripname());
        intent.putExtra("tripDestination", trip.getEndloc());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}




