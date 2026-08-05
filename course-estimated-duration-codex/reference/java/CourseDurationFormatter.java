package reference;

/**
 * Mẫu logic. Codex phải chuyển class này vào package/util/service hiện có,
 * hoặc tích hợp vào formatter đang tồn tại. Không tạo package "reference"
 * trong production source.
 */
public final class CourseDurationFormatter {

    private CourseDurationFormatter() {
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "Chưa có thời lượng video";
        }
        if (seconds < 60) {
            return "< 1 phút";
        }

        long totalMinutes = (seconds + 59) / 60; // ceil to minute
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours == 0) {
            return totalMinutes + " phút";
        }
        if (minutes == 0) {
            return hours + " giờ";
        }
        return hours + " giờ " + minutes + " phút";
    }
}
