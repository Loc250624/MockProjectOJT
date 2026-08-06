package com.ojtsu26.elearning.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseDurationFormatterTest {

    @ParameterizedTest
    @CsvSource(value = {
            "0|No video duration",
            "-1|No video duration",
            "1|Less than 1 min",
            "59|Less than 1 min",
            "60|1 min",
            "61|2 mins",
            "3540|59 mins",
            "3599|1 hr",
            "3600|1 hr",
            "3660|1 hr 1 min",
            "7200|2 hrs",
            "8458|2 hrs 21 mins"
    }, delimiter = '|')
    void formatsCourseVideoDuration(long seconds, String expected) {
        assertEquals(expected, CourseDurationFormatter.formatSeconds(seconds));
    }
}
