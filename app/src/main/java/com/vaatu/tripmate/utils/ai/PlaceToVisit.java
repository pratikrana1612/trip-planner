package com.vaatu.tripmate.utils.ai;

import com.google.gson.annotations.SerializedName;

public class PlaceToVisit {

    @SerializedName("name")
    private String name;

    @SerializedName("time_of_day")
    private String timeOfDay;

    @SerializedName("notes")
    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}


