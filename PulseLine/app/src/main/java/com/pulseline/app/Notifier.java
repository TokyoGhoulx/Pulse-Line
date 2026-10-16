package com.pulseline.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.pulseline.app.engine.Alert;

/** ALERT step delivery: pushes the plain-language message to the operator's phone as a notification. */
public class Notifier {
    private static final String CHANNEL = "pulseline_alerts";
    private final Context ctx;
    private final NotificationManager nm;

    public Notifier(Context ctx) {
        this.ctx = ctx;
        this.nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Machine alerts", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Plain-language warnings before a machine fails");
            nm.createNotificationChannel(ch);
        }
    }

    public void post(Alert a) {
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(a.title)
                .setContentText(a.action)
                .setStyle(new Notification.BigTextStyle().bigText(a.body + "\n" + a.action))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify((int) (a.id % 100000), n);
    }
}
