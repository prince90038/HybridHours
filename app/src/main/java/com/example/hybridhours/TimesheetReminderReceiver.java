package com.example.hybridhours;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

public class TimesheetReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "timesheet_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int day = intent.getIntExtra("day", -1);
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);
        long id = intent.getLongExtra("reminder_id", -1);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Timesheet Reminder", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();

        // Intent to open the app
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Done Action
        Intent doneIntent = new Intent(context, NotificationActionReceiver.class);
        doneIntent.setAction(NotificationActionReceiver.ACTION_DONE);
        doneIntent.putExtra("notification_id", notificationId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(context, notificationId + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Snooze Action
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction(NotificationActionReceiver.ACTION_SNOOZE);
        snoozeIntent.putExtra("notification_id", notificationId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, notificationId + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Notification
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Timesheet Reminder")
                .setContentText("Please fill your timesheet today!")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.checkbox_on_background, "Done", donePendingIntent)
                .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze (1h)", snoozePendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        nm.notify(notificationId, notification);

        // Reschedule for next week (only for weekly reminders)
        if (day != -1) {
            ReminderHelper.scheduleNext(context, day, hour, minute, id);
        }
    }
}
