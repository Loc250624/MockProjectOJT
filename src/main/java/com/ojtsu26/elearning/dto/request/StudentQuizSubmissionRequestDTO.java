package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class StudentQuizSubmissionRequestDTO {
    @NotNull(message = "Attempt is required")
    private Integer attemptId;

    private Map<Integer, String> answers;
}
