package com.example.hybridhours;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TimePicker startTimePicker, endTimePicker;
    private TextView totalHoursText, wfhBeforeText, wfhAfterText;
    private View wfhBeforeCard, wfhAfterCard;
    private ViewGroup mainRoot;

    private static final String PREFS_NAME = "hybrid_hours_prefs";
    private static final String KEY_START_H = "start_h";
    private static final String KEY_START_M = "start_m";
    private static final String KEY_END_H = "end_h";
    private static final String KEY_END_M = "end_m";
    private static final String KEY_TARGET_H = "target_h";
    private static final String KEY_TARGET_M = "target_m";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mainRoot = findViewById(R.id.mainRoot);
        startTimePicker = findViewById(R.id.startTimePicker);
        endTimePicker = findViewById(R.id.endTimePicker);
        totalHoursText = findViewById(R.id.totalHours);
        wfhBeforeText = findViewById(R.id.wfhBeforeText);
        wfhAfterText = findViewById(R.id.wfhAfterText);
        wfhBeforeCard = findViewById(R.id.wfhBeforeCard);
        wfhAfterCard = findViewById(R.id.wfhAfterCard);

        startTimePicker.setIs24HourView(true);
        endTimePicker.setIs24HourView(true);

        // Load saved values
        startTimePicker.setHour(prefs.getInt(KEY_START_H, 9));
        startTimePicker.setMinute(prefs.getInt(KEY_START_M, 0));
        endTimePicker.setHour(prefs.getInt(KEY_END_H, 18));
        endTimePicker.setMinute(prefs.getInt(KEY_END_M, 15));
        
        // Initial output
        updateOutput();

        // Real-time update on change
        startTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            view.playSoundEffect(SoundEffectConstants.CLICK);
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            prefs.edit().putInt(KEY_START_H, hourOfDay).putInt(KEY_START_M, minute).apply();
            updateOutput();
        });
        endTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            view.playSoundEffect(SoundEffectConstants.CLICK);
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            prefs.edit().putInt(KEY_END_H, hourOfDay).putInt(KEY_END_M, minute).apply();
            updateOutput();
        });

        // Top-right button for Settings
        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Ensure all reminders are scheduled if enabled
        ReminderHelper.scheduleAllReminders(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh output in case target was changed in Settings
        updateOutput();
    }

    private void updateOutput() {
        int startHour = startTimePicker.getHour();
        int startMinute = startTimePicker.getMinute();
        int endHour = endTimePicker.getHour();
        int endMinute = endTimePicker.getMinute();

        int targetH = prefs.getInt(KEY_TARGET_H, 9);
        int targetM = prefs.getInt(KEY_TARGET_M, 15);
        int requiredMinutes = targetH * 60 + targetM;

        int startTotal = startHour * 60 + startMinute;
        int endTotal = endHour * 60 + endMinute;
        int officeMinutes = endTotal - startTotal;

        if (officeMinutes < 0) officeMinutes = (24 * 60 - startTotal) + endTotal;

        int remaining = requiredMinutes - officeMinutes;
        if (remaining < 0) remaining = 0;

        int beforeEnd = startTotal - 1;
        int beforeStart = beforeEnd - remaining + 1;
        int afterStart = endTotal + 1;
        int afterEnd = afterStart + remaining - 1;

        int h = officeMinutes / 60;
        int m = officeMinutes % 60;
        String totalText = String.format(Locale.getDefault(), "Total Office Hours: %2dh %02dm", h, m);
        
        String beforeRange = formatRange(beforeStart, beforeEnd);
        String afterRange = formatRange(afterStart, afterEnd);
        String bText = "Before Office: " + beforeRange + " (" + formatDuration(remaining) + ")";
        String aText = "After Office: " + afterRange + " (" + formatDuration(remaining) + ")";

        // Only animate if text changed
        boolean changed = !totalHoursText.getText().toString().equals(totalText) ||
                          !wfhBeforeText.getText().toString().equals(bText) ||
                          !wfhAfterText.getText().toString().equals(aText);

        if (changed) {
            TransitionManager.beginDelayedTransition(mainRoot);
            totalHoursText.setText(totalText);
            wfhBeforeText.setText(bText);
            wfhAfterText.setText(aText);
            
            animateCard(wfhBeforeCard);
            animateCard(wfhAfterCard);
        }
    }

    private String formatRange(int startMin, int endMin) {
        int total = 24 * 60;
        int s = ((startMin % total) + total) % total;
        int e = ((endMin % total) + total) % total;
        return minutesToHHMM(s) + " to " + minutesToHHMM(e);
    }

    private String minutesToHHMM(int minutes) {
        int mTotal = minutes % (24 * 60);
        int h = mTotal / 60;
        int m = mTotal % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    private String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h > 0) return String.format(Locale.getDefault(), "%dh %02dm", h, m);
        return String.format(Locale.getDefault(), "%02dm", m);
    }

    private void animateCard(View view) {
        // Subtle pulse instead of blink
        view.animate().cancel();
        view.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(150)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start())
                .start();
    }
}
