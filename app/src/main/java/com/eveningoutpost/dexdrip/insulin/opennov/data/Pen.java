package com.eveningoutpost.dexdrip.insulin.opennov.data;

import com.google.gson.annotations.Expose;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Pen {
    @Expose
    String serial;
    @Expose
    String type;
}
