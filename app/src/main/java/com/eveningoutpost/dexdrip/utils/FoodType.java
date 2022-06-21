package com.eveningoutpost.dexdrip.utils;

public enum FoodType {
    CARBS("carbs"),
    FATS("fats"),
    PROTEINS("proteins");

    public final String value;

    FoodType(String value) {
        this.value = value;
    }
}
