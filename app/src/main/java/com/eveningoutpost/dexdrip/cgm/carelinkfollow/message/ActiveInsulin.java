package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import com.eveningoutpost.dexdrip.cgm.carelinkfollow.message.util.CareLinkJsonAdapter;
import com.google.gson.annotations.JsonAdapter;

import java.util.Date;

/**
 * CareLink ActiveInsulin data
 */
public class ActiveInsulin {

    public Integer code;
    @JsonAdapter(CareLinkJsonAdapter.class)
    public Date datetime;
    public long version;
    public Double amount;
    public String precision;
    public String kind;

}