package com.ojtsu26.elearning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardOverviewDTO {
    private LocalDate from;
    private LocalDate to;
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalCourses;
    private long totalEnrollments;
    private long paidOrderCount;
    private String currency;
    private BigDecimal totalRevenue;
    private long newStudents;
    private long activeStudents;
    private String activeStudentMethod;
}
