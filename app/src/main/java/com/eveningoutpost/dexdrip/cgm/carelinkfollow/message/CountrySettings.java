package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.List;

public class CountrySettings {

    public String name;
    public List<Language> languages;
    public String defaultLanguage;
    public String defaultCountryName;
    public String defaultDevice;
    public String dialCode;
    public boolean cpMobileAppAvailable;
    public boolean uploaderAllowed;
    public String techSupport;
    public String techDays;
    public String firstDayOfWeek;
    public String techHours;
    public Integer legalAge;
    public String shortDateFormat;
    public String shortTimeFormat;
    public String mediaHost;
    public String blePereodicDataEndpoint;
    public String region;
    public String carbDefaultUnit;
    public String bgUnits;
    public String timeFormat;
    public String timeUnitsDefault;
    public String recordSeparator;
    public String glucoseUnitsDefault;
    public String carbohydrateUnitsDefault;
    public Long carbExchangeRatioDefault;
    public ReportDateFormat reportDateFormat;
    public MfaRules mfa;
    public List<SupportedReport> supportedReports;
    public boolean smsSendingAllowed;
    public PostalInfo postal;
    public NumberFormat numberFormat;

}
