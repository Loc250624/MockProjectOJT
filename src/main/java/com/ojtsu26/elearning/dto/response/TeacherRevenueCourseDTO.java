package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRevenueCourseDTO {
    private Integer courseId;
    private String courseTitle;
    private BigDecimal revenue;
    private long paidOrderCount;
    private long unitsSold;
    private long enrollmentCount;
    private long studentCount;
}
