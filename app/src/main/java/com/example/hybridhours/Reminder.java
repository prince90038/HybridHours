package com.example.hybridhours;

import java.util.Objects;

public class Reminder {
    private final long id;
    private final int dayOfWeek; // Calendar.SUNDAY to Calendar.SATURDAY
    private final int hour;
    private final int minute;

    public Reminder(long id, int dayOfWeek, int hour, int minute) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.minute = minute;
    }

    public long getId() { return id; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reminder reminder = (Reminder) o;
        return id == reminder.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
