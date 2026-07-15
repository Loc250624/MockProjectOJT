package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRecentSubmissionDashboardDTO {
    private Integer submissionId;
    private Integer assignmentId;
    private String assessmentTitle;
    private String studentName;
    private LocalDateTime submittedAt;
    private String submittedAgoLabel;
    private String reviewStatus;
    private String reviewStatusClass;
    private String reviewUrl;
}
