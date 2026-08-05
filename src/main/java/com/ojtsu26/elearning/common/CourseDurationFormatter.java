package com.ojtsu26.elearning.common;

public final class CourseDurationFormatter {

    private CourseDurationFormatter() {
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "No video duration";
        }
        if (seconds < 60) {
            return "Less than 1 min";
        }

        long totalMinutes = (seconds + 59) / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return totalMinutes + (totalMinutes == 1 ? " min" : " mins");
        }
        if (minutes == 0) {
            return hours + (hours == 1 ? " hr" : " hrs");
        }
        return hours + (hours == 1 ? " hr " : " hrs ")
                + minutes + (minutes == 1 ? " min" : " mins");
    }

    public static String formatVideoSeconds(Integer seconds) {
        return seconds == null || seconds <= 0 ? null : formatSeconds(seconds);
    }
}
