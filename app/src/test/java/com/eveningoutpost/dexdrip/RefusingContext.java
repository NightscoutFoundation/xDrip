package com.eveningoutpost.dexdrip;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the platform when it refuses to start a service.
 * <p>
 * Records what was asked for, then refuses it the way Android refuses a background process from
 * target SDK 26. Everything else, {@code stopService} and preferences included, goes to the real
 * context underneath.
 *
 * @author Asbjørn Aarrestad
 */
public final class RefusingContext extends ContextWrapper {

    private final List<Intent> attempted = new ArrayList<>();

    public RefusingContext(final Context base) {
        super(base);
    }

    /** Every intent {@code startService} was called with, in the order it was called. */
    public List<Intent> attempted() {
        return attempted;
    }

    /** The class an intent targets, or null if it carries no explicit component. */
    public static String targetClassOf(final Intent intent) {
        final ComponentName component = intent.getComponent();
        return component == null ? null : component.getClassName();
    }

    @Override
    public ComponentName startService(final Intent service) {
        attempted.add(service);
        throw new IllegalStateException("Not allowed to start service " + service + ": app is in background");
    }
}
