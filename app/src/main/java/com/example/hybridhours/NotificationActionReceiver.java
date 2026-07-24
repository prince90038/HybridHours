package com.example.hybridhours;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionReceiver extends BroadcastReceiver {

    public static final String ACTION_DONE = "com.example.hybridhours.ACTION_DONE";
    public static final String ACTION_SNOOZE = "com.example.hybridhours.ACTION_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notification_id", -1);

        if (ACTION_SNOOZE.equals(action)) {
            ReminderHelper.scheduleSnooze(context);
        }
        
        // ACTION_DONE just falls through to dismissing the notification below

        // Dismiss the notification
        if (notificationId != -1) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(notificationId);
            }
        }
    }
}
