package com.example.hybridhours;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ReminderAdapter adapter;
    private List<Reminder> reminderList;
    private SwitchCompat enableSwitch;
    private TextView targetHoursText, reliabilityStatus;
    private View fixReliabilityButton;
    private boolean isUpdatingSwitch = false;

    private static final String PREFS_NAME = "hybrid_hours_prefs";
    private static final String KEY_TARGET_H = "target_h";
    private static final String KEY_TARGET_M = "target_m";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        RecyclerView recyclerView = findViewById(R.id.reminderRecyclerView);
        enableSwitch = findViewById(R.id.enableSwitch);
        targetHoursText = findViewById(R.id.targetHoursText);
        reliabilityStatus = findViewById(R.id.reliabilityStatus);
        fixReliabilityButton = findViewById(R.id.fixReliabilityButton);

        reminderList = ReminderHelper.getReminders(this);
        adapter = new ReminderAdapter(reminderList, new ReminderAdapter.OnReminderClickListener() {
            @Override
            public void onDeleteClick(Reminder reminder) {
                ReminderHelper.deleteReminder(SettingsActivity.this, reminder);
                reminderList.remove(reminder);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onEditClick(Reminder reminder) {
                showEditReminderDialog(reminder);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateTargetText();
        syncSwitchState();
        updateReliabilityUI();
        
        enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) return;
            
            if (isChecked) {
                if (hasAllPermissions()) {
                    ReminderHelper.setRemindersEnabled(this, true);
                } else {
                    isUpdatingSwitch = true;
                    enableSwitch.setChecked(false);
                    isUpdatingSwitch = false;
                    requestMissingPermissions();
                }
            } else {
                ReminderHelper.setRemindersEnabled(this, false);
            }
        });

        findViewById(R.id.targetBlock).setOnClickListener(v -> showTargetPickerDialog());

        findViewById(R.id.addButton).setOnClickListener(v -> {
            if (ReminderHelper.areRemindersEnabled(this)) {
                showAddReminderDialog();
            } else {
                Toast.makeText(this, "Please enable reminders first", Toast.LENGTH_SHORT).show();
            }
        });

        fixReliabilityButton.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
    }

    private void showTargetPickerDialog() {
        int h = prefs.getInt(KEY_TARGET_H, 9);
        int m = prefs.getInt(KEY_TARGET_M, 15);

        TimePickerDialog picker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            prefs.edit().putInt(KEY_TARGET_H, hourOfDay).putInt(KEY_TARGET_M, minute).apply();
            updateTargetText();
        }, h, m, true);
        
        picker.setTitle("Set Work Target (Hours/Minutes)");
        picker.show();
    }

    private void updateTargetText() {
        int h = prefs.getInt(KEY_TARGET_H, 9);
        int m = prefs.getInt(KEY_TARGET_M, 15);
        targetHoursText.setText(String.format(Locale.getDefault(), "Target: %dh %02dm", h, m));
    }

    private void updateReliabilityUI() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                reliabilityStatus.setText("Optimization: Disabled (Reminders are reliable)");
                fixReliabilityButton.setVisibility(View.GONE);
            } else {
                reliabilityStatus.setText("System might freeze reminders to save battery.");
                fixReliabilityButton.setVisibility(View.VISIBLE);
            }
        } else {
            findViewById(R.id.reliabilityCard).setVisibility(View.GONE);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(fallback);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasAllPermissions() && !ReminderHelper.areRemindersEnabled(this)) {
            ReminderHelper.setRemindersEnabled(this, true);
        }
        syncSwitchState();
        updateReliabilityUI();
    }

    private void syncSwitchState() {
        isUpdatingSwitch = true;
        enableSwitch.setChecked(ReminderHelper.areRemindersEnabled(this));
        isUpdatingSwitch = false;
    }

    private void showAddReminderDialog() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        int[] calendarDays = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Day");
        builder.setItems(days, (dialog, which) -> {
            int selectedDay = calendarDays[which];
            showTimePicker(selectedDay, 10, 0, false, -1);
        });
        builder.show();
    }

    private void showEditReminderDialog(Reminder reminder) {
        showTimePicker(reminder.getDayOfWeek(), reminder.getHour(), reminder.getMinute(), true, reminder.getId());
    }

    private void showTimePicker(int dayOfWeek, int h, int m, boolean isEdit, long id) {
        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (isEdit) {
                ReminderHelper.updateReminder(this, id, dayOfWeek, hourOfDay, minute);
            } else {
                ReminderHelper.addReminder(this, dayOfWeek, hourOfDay, minute);
            }
            refreshList();
        }, h, m, true);
        timePicker.setTitle(isEdit ? "Edit Time" : "Select Time");
        timePicker.show();
    }

    private void refreshList() {
        reminderList.clear();
        reminderList.addAll(ReminderHelper.getReminders(this));
        adapter.notifyDataSetChanged();
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        // USE_EXACT_ALARM is auto-granted on install
        return true;
    }

    private void requestMissingPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (hasAllPermissions()) {
                    ReminderHelper.setRemindersEnabled(this, true);
                    syncSwitchState();
                } else {
                    requestMissingPermissions();
                }
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
