package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StudentCodingAssignmentDTO {
    private Integer courseId;
    private Integer lessonId;
    private Integer assignmentId;
    private String title;
    private String problemStatement;
    private String starterCode;
    private List<String> allowedLanguages;
    private Integer timeLimitMs;
    private List<StudentCodeExampleDTO> examples;
    private Integer submissionId;
    private String submittedLanguage;
    private String submittedCode;
    private SubmissionStatus status;
    private String submissionState;
    private Boolean submitted;
    private Boolean unavailable;
    private String unavailableMessage;
    private LocalDateTime submittedAt;
}
