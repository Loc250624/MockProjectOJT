package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class LessonResponseDTO {
    private Integer id;

    private String title;

    private String content;

    private LessonType type;

    private Integer orderIndex;

    private java.time.LocalDateTime createdAt;

    private Integer courseId;

    private Integer quizId;

    private Integer codingAssignmentId;

    private Boolean hasQuiz;

    private Boolean hasCodingAssignment;
}
