package com.vaatu.tripmate.utils.ai;

import com.google.gson.annotations.SerializedName;

public class PackingItem {

    @SerializedName("item")
    private String item;

    @SerializedName("category")
    private String category;

    @SerializedName("reason")
    private String reason;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}









