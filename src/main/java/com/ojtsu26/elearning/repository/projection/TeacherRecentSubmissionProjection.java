package com.ojtsu26.elearning.repository.projection;

import com.ojtsu26.elearning.model.enums.SubmissionStatus;

import java.time.LocalDateTime;

public interface TeacherRecentSubmissionProjection {
    Integer getSubmissionId();
    Integer getAssignmentId();
    String getAssessmentTitle();
    String getStudentName();
    LocalDateTime getSubmittedAt();
    SubmissionStatus getStatus();
}
