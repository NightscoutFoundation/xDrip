package com.eveningoutpost.dexdrip.sharemodels.useragentinfo;

import com.google.gson.annotations.Expose;

/**
 * Created by Emma Black on 6/29/15.
 */
public class RuntimeInfo {

    @Expose
    public String DeviceManufacturer = "Apple";

    @Expose
    public String DeviceModel = "iPhone5,2";

    @Expose
    public String DeviceOsVersion = "7.0.2";

    @Expose
    public String AppVersion = "3.0.2.11";

    @Expose
    public String AppName = "DexcomShare";

    @Expose
    public String AppNumber = "SW10569";

    @Expose
    public String DeviceOsName ="iPhone OS";
 }
