package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.response.LessonResponseDTO;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.Video;
import com.ojtsu26.elearning.model.enums.LessonType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonMapperDurationTest {

    private final LessonMapper mapper = new LessonMapperImpl();

    @Test
    void mapsVideoDurationForCurriculumDisplay() {
        Lesson lesson = Lesson.builder()
                .id(12)
                .type(LessonType.VIDEO)
                .video(Video.builder()
                        .durationSeconds(8458)
                        .build())
                .build();

        LessonResponseDTO result = mapper.toDto(lesson);

        assertEquals(8458, result.getVideoDurationSeconds());
        assertEquals("2 hrs 21 mins", result.getVideoDurationDisplay());
    }
}
