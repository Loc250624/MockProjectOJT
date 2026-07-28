package com.ojtsu26.elearning.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonTypeConverterTest {

    private final LessonTypeConverter converter = new LessonTypeConverter();

    @Test
    void legacyCodingValueLoadsAsRetiredContent() {
        assertEquals(LessonType.RETIRED,
                converter.convertToEntityAttribute("coding"));
    }

    @Test
    void activeLessonTypesStillRoundTrip() {
        assertEquals("video", converter.convertToDatabaseColumn(LessonType.VIDEO));
        assertEquals("quiz", converter.convertToDatabaseColumn(LessonType.QUIZ));
        assertEquals(LessonType.VIDEO, converter.convertToEntityAttribute("video"));
        assertEquals(LessonType.QUIZ, converter.convertToEntityAttribute("quiz"));
    }
}
