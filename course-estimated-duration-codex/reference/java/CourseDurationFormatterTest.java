package reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Mẫu test. Điều chỉnh package/import theo project.
 */
class CourseDurationFormatterTest {

    @Test
    void formatsMissingDuration() {
        assertEquals("Chưa có thời lượng video",
                CourseDurationFormatter.formatSeconds(0));
    }

    @Test
    void formatsSubMinuteDuration() {
        assertEquals("< 1 phút",
                CourseDurationFormatter.formatSeconds(59));
    }

    @Test
    void roundsUpToMinute() {
        assertEquals("2 phút",
                CourseDurationFormatter.formatSeconds(61));
    }

    @Test
    void formatsExactHour() {
        assertEquals("1 giờ",
                CourseDurationFormatter.formatSeconds(3600));
    }

    @Test
    void formatsHourAndMinute() {
        assertEquals("1 giờ 1 phút",
                CourseDurationFormatter.formatSeconds(3660));
    }
}
