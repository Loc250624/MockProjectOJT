package com.ojtsu26.elearning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseEnrollmentStateResponseDTO {
    private Integer courseId;
    private boolean available;
    private boolean freeCourse;
    private boolean enrolled;
    private boolean paymentPending;
    private String action;
    private String actionLabel;
    private Integer enrollmentId;
    private String message;
}
