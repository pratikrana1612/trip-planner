package com.vaatu.tripmate.utils.ai;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class AiTripPlan {

    @SerializedName("packing_list")
    private List<PackingItem> packingList = new ArrayList<>();

    @SerializedName("day_plan")
    private List<DayPlan> dayPlan = new ArrayList<>();

    @SerializedName("general_tips")
    private List<String> generalTips = new ArrayList<>();

    public List<PackingItem> getPackingList() {
        return packingList;
    }

    public void setPackingList(List<PackingItem> packingList) {
        this.packingList = packingList;
    }

    public List<DayPlan> getDayPlan() {
        return dayPlan;
    }

    public void setDayPlan(List<DayPlan> dayPlan) {
        this.dayPlan = dayPlan;
    }

    public List<String> getGeneralTips() {
        return generalTips;
    }

    public void setGeneralTips(List<String> generalTips) {
        this.generalTips = generalTips;
    }

    public boolean isEmpty() {
        return (packingList == null || packingList.isEmpty()) &&
                (dayPlan == null || dayPlan.isEmpty()) &&
                (generalTips == null || generalTips.isEmpty());
    }
}


