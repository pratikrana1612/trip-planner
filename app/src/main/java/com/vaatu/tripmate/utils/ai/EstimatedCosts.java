package com.vaatu.tripmate.utils.ai;

import com.google.gson.annotations.SerializedName;

/**
 * Simple container for AI suggested estimated costs.
 */
public class EstimatedCosts {

    @SerializedName("food")
    private String food;

    @SerializedName("travel")
    private String travel;

    @SerializedName("stay")
    private String stay;

    // Default constructor for Firebase/Gson
    public EstimatedCosts() {
    }

    public String getFood() {
        return food;
    }

    public void setFood(String food) {
        this.food = food;
    }

    public String getTravel() {
        return travel;
    }

    public void setTravel(String travel) {
        this.travel = travel;
    }

    public String getStay() {
        return stay;
    }

    public void setStay(String stay) {
        this.stay = stay;
    }

    public boolean isEmpty() {
        return (food == null || food.isEmpty())
                && (travel == null || travel.isEmpty())
                && (stay == null || stay.isEmpty());
    }
}


