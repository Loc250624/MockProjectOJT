package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentCodeSubmissionRequestDTO {
    private Integer submissionId;

    @NotBlank(message = "Programming language is required")
    @Size(max = 50, message = "Programming language is too long")
    private String language;

    @NotBlank(message = "Code is required")
    @Size(max = 20000, message = "Code must not exceed 20000 characters")
    private String code;
}
