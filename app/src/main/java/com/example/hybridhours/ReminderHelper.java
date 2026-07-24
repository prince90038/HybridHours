package com.example.hybridhours;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReminderHelper {

    private static final String PREFS_NAME = "hybrid_hours_prefs";
    private static final String KEY_REMINDERS = "reminders_list";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";

    public static void setRemindersEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
        if (enabled) {
            scheduleAllReminders(context);
        } else {
            cancelAllReminders(context);
        }
    }

    public static boolean areRemindersEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, false);
    }

    public static List<Reminder> getReminders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REMINDERS, null);
        if (json == null) return new ArrayList<>();

        Type listType = new TypeToken<ArrayList<Reminder>>() {}.getType();
        List<Reminder> list = new Gson().fromJson(json, listType);
        return list != null ? list : new ArrayList<>();
    }

    public static void addReminder(Context context, int dayOfWeek, int hour, int minute) {
        List<Reminder> reminders = getReminders(context);
        reminders.add(new Reminder(System.currentTimeMillis(), dayOfWeek, hour, minute));
        saveReminders(context, reminders);
        if (areRemindersEnabled(context)) {
            scheduleAllReminders(context);
        }
    }

    public static void deleteReminder(Context context, Reminder reminder) {
        List<Reminder> reminders = getReminders(context);
        reminders.remove(reminder);
        saveReminders(context, reminders);
        
        // Cancel the specific alarm
        cancelAlarm(context, (int) reminder.getId());
        
        if (areRemindersEnabled(context)) {
            scheduleAllReminders(context);
        }
    }

    public static void updateReminder(Context context, long id, int newDay, int newHour, int newMinute) {
        List<Reminder> reminders = getReminders(context);
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId() == id) {
                reminders.set(i, new Reminder(id, newDay, newHour, newMinute));
                break;
            }
        }
        saveReminders(context, reminders);
        if (areRemindersEnabled(context)) {
            scheduleAllReminders(context);
        }
    }

    private static void saveReminders(Context context, List<Reminder> reminders) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(reminders);
        prefs.edit().putString(KEY_REMINDERS, json).apply();
    }

    public static void scheduleAllReminders(Context context) {
        if (!areRemindersEnabled(context)) return;

        List<Reminder> reminders = getReminders(context);
        for (Reminder r : reminders) {
            scheduleReminder(context, r);
        }
    }

    private static void scheduleReminder(Context context, Reminder r) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, r.getDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, r.getHour());
        calendar.set(Calendar.MINUTE, r.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        setAlarm(context, calendar, (int) r.getId(), r.getDayOfWeek(), r.getHour(), r.getMinute());
    }

    public static void scheduleSnooze(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        setAlarm(context, calendar, 9999, -1, -1, -1);
    }

    private static void setAlarm(Context context, Calendar calendar, int requestCode, int d, int h, int m) {
        Intent intent = new Intent(context, TimesheetReminderReceiver.class);
        intent.putExtra("day", d);
        intent.putExtra("hour", h);
        intent.putExtra("minute", m);
        intent.putExtra("reminder_id", (long) requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // With USE_EXACT_ALARM permission, we can directly set exact alarms
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private static void cancelAlarm(Context context, int requestCode) {
        Intent intent = new Intent(context, TimesheetReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private static void cancelAllReminders(Context context) {
        List<Reminder> reminders = getReminders(context);
        for (Reminder r : reminders) {
            cancelAlarm(context, (int) r.getId());
        }
        cancelAlarm(context, 9999); // Cancel snooze if any
    }

    public static void scheduleWeeklyReminder(Context context, int dayOfWeek, int hour, int minute) {
        // This is now used by the Receiver to reschedule the next occurrence
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.WEEK_OF_YEAR, 1);

        // We need the original ID to keep the alarm unique. 
        // For simplicity during reschedule, we use (day * 100 + hour) as a unique-ish ID 
        // but since we now have dynamic IDs, we should really pass the ID through the intent.
        // I will update TimesheetReminderReceiver to pass the long ID.
    }

    public static void scheduleNext(Context context, int dayOfWeek, int hour, int minute, long id) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        setAlarm(context, calendar, (int) id, dayOfWeek, hour, minute);
    }
}
