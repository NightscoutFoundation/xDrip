package com.eveningoutpost.dexdrip.deposit;

// jamorham

import com.eveningoutpost.dexdrip.Models.JoH;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DepositReply1 {

    @Expose
    @SerializedName("result")
    String result;

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

}

