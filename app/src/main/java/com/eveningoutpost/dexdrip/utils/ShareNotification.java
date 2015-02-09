package com.eveningoutpost.dexdrip.utils;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class to show a share notificaiton.
 */
public class ShareNotification {

    public static void viewOrShare(String mime, Uri uri, NotificationCompat.Builder builder, Context context) {
        final Intent viewFileIntent = new Intent(Intent.ACTION_VIEW);
        viewFileIntent.setDataAndType(uri, mime);

        ResolveInfo matches = context.getPackageManager().resolveActivity(viewFileIntent, PackageManager.MATCH_DEFAULT_ONLY);

        if (matches != null) {
            final PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, viewFileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                addShare(builder, mime, uri, context);
            }
        } else {
            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType(mime);
            final PendingIntent sharePendingIntent = PendingIntent.getActivity(context, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(sharePendingIntent);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void addShare(NotificationCompat.Builder notification, String mime, Uri uri, Context context) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType(mime);
        final PendingIntent sharePendingIntent = PendingIntent.getActivity(context, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent);
    }
}
