package com.eveningoutpost.dexdrip;

// jamorham

import android.content.Context;

import com.google.firebase.messaging.FirebaseMessagingService;

import lombok.Getter;

public class JamListenerSvc extends FirebaseMessagingService {

    @Getter
    private boolean injectable;

    @Override
    protected void attachBaseContext(Context base) {
      super.attachBaseContext(base);
    }

    public void setInjectable() {
        attachBaseContext(xdrip.getAppContext());
        injectable = true;
    }



}
