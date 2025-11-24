package com.vaatu.tripmate.data.remote.network;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vaatu.tripmate.utils.TripModel;
import com.vaatu.tripmate.utils.ai.AiTripPlan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiTravelService {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String GEMINI_API_KEY = "";

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public GeminiTravelService() {
        client = new OkHttpClient.Builder()
                .callTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public interface GeminiCallback {
        void onSuccess(AiTripPlan aiTripPlan);

        void onError(String errorMessage);
    }

    public void generatePlan(TripModel trip, GeminiCallback callback) {
        if (trip == null) {
            callback.onError("Missing trip data.");
            return;
        }

        if (TextUtils.isEmpty(GEMINI_API_KEY) || GEMINI_API_KEY.contains("YOUR_GEMINI_API_KEY_HERE")) {
            callback.onError("Gemini API key is not configured.");
            return;
        }

        String requestJson;
        try {
            requestJson = buildRequestBody(trip);
        } catch (JSONException jsonException) {
            callback.onError("Failed to prepare Gemini request.");
            return;
        }

        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("key", GEMINI_API_KEY)
                .build();

        RequestBody body = RequestBody.create(requestJson, MEDIA_TYPE_JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Gemini error: " + response.code());
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                AiTripPlan plan = parsePlan(responseBody);
                if (plan == null || plan.isEmpty()) {
                    callback.onError("Unable to parse AI response.");
                } else {
                    callback.onSuccess(plan);
                }
            }
        });
    }

    private String buildRequestBody(TripModel trip) throws JSONException {
        JSONObject root = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        String prompt = buildPrompt(trip);
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);

        root.put("contents", contents);
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        root.put("generationConfig", generationConfig);
        return root.toString();
    }

    private String buildPrompt(TripModel trip) {
        int days = estimateTripDuration(trip);
        String startDate = safeText(trip.getDate());
        String notes = buildNotes(trip.getNotes());

        return "You are an AI travel planner. The user is travelling from "
                + safeText(trip.getStartloc()) + " to " + safeText(trip.getEndloc())
                + " for " + days + " days, starting on " + startDate + ". Optional notes: "
                + notes + ".\n"
                + "Return ONLY valid JSON. Do not include markdown or explanations.\n"
                + "The JSON must have this exact structure:\n"
                + "{\n"
                + "  \"packing_list\": [ { \"item\": \"...\",\"category\": \"...\",\"reason\": \"...\" } ],\n"
                + "  \"day_plan\": [ { \"day_number\": 1, \"title\": \"...\",\"description\": \"...\",\"places_to_visit\":[{\"name\":\"...\",\"time_of_day\":\"morning/afternoon/evening\",\"notes\":\"...\"}]} ],\n"
                + "  \"general_tips\": [\"...\"]\n"
                + "}\n"
                + "Adjust packing and plans according to destination weather, common attractions and reasonable budget.";
    }

    private int estimateTripDuration(TripModel trip) {
        if (trip == null || TextUtils.isEmpty(trip.getDate())) {
            return 3;
        }

        String dateText = trip.getDate();
        if (dateText.contains("-")) {
            String[] parts = dateText.split("-");
            if (parts.length >= 2) {
                String first = parts[0].replaceAll("[^0-9]", "");
                String second = parts[1].replaceAll("[^0-9]", "");
                if (!TextUtils.isEmpty(first) && !TextUtils.isEmpty(second)) {
                    try {
                        int start = Integer.parseInt(first);
                        int end = Integer.parseInt(second);
                        int diff = Math.abs(end - start) + 1;
                        if (diff > 0 && diff <= 30) {
                            return diff;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 3;
    }

    private String buildNotes(List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return "None";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < notes.size(); i++) {
            builder.append(notes.get(i));
            if (i < notes.size() - 1) {
                builder.append("; ");
            }
        }
        return builder.toString();
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "unknown" : value;
    }

    private AiTripPlan parsePlan(String responseBody) {
        try {
            GeminiResponse geminiResponse = gson.fromJson(responseBody, GeminiResponse.class);
            if (geminiResponse == null || geminiResponse.candidates == null || geminiResponse.candidates.isEmpty()) {
                return null;
            }

            GeminiResponse.Candidate candidate = geminiResponse.candidates.get(0);
            if (candidate.content == null || candidate.content.parts == null || candidate.content.parts.isEmpty()) {
                return null;
            }

            String jsonPayload = candidate.content.parts.get(0).text;
            if (TextUtils.isEmpty(jsonPayload)) {
                return null;
            }

            return gson.fromJson(jsonPayload, AiTripPlan.class);
        } catch (JsonSyntaxException jsonSyntaxException) {
            return null;
        }
    }

    private static class GeminiResponse {
        List<Candidate> candidates;

        static class Candidate {
            Content content;
        }

        static class Content {
            List<Part> parts;
        }

        static class Part {
            String text;
        }
    }
}
