package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

import java.util.Date;

public class User {

    public static final String ROLE_CARE_PARTNER_US = "CARE_PARTNER";
    public static final String ROLE_CARE_PARTNER_OUS = "CARE_PARTNER_OUS";
    public static final String ROLE_CARE_PATIENT_US = "CARE_PARTNER";
    public static final String ROLE_CARE_PATIENT_OUS = "CARE_PARTNER_OUS";
    public static final String USER_ROLE_CARE_PARTNER = "carepartner";
    public static final String USER_ROLE_PATIENT = "patient";

    public Date loginDateUTC;
    public String id;
    public String country;
    public String language;
    public String lastName;
    public String firstName;
    public Integer accountId;
    public String role;
    public String cpRegistrationStatus;
    public String accountSuspended;
    public boolean needToReconsent;
    public boolean mfaRequired;
    public boolean mfaEnabled;

    public boolean isCarePartner() {
        return (this.role.equals(ROLE_CARE_PARTNER_US) || this.role.equals(ROLE_CARE_PARTNER_OUS));
    }

    public String getUserRole() {
        if (isCarePartner())
            return USER_ROLE_CARE_PARTNER;
        else
            return USER_ROLE_PATIENT;
    }

}
