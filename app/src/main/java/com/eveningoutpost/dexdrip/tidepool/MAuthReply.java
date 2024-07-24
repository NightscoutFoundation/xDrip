package com.eveningoutpost.dexdrip.tidepool;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MAuthReply {

    @Expose
    @SerializedName("emailVerified")
    Boolean emailVerified;
    @Expose
    @SerializedName("emails")
    List<String> emailList;
    @Expose
    @SerializedName("termsAccepted")
    String termsDate;
    @Expose
    @SerializedName("userid")
    String userid;
    @Expose
    @SerializedName("username")
    String username;

    public MAuthReply(final String userid) {
        this.userid = userid;
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

}
