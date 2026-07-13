package com.ojtsu26.elearning.repository.projection;

public interface TeacherEnrollmentCountProjection {
    Integer getCourseId();
    Long getEnrollmentCount();
    Long getStudentCount();
}
