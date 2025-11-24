package com.vaatu.tripmate.utils.ai;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DayPlan {

    @SerializedName("day_number")
    private int dayNumber;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("places_to_visit")
    private List<PlaceToVisit> placesToVisit = new ArrayList<>();

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PlaceToVisit> getPlacesToVisit() {
        return placesToVisit;
    }

    public void setPlacesToVisit(List<PlaceToVisit> placesToVisit) {
        this.placesToVisit = placesToVisit;
    }
}


