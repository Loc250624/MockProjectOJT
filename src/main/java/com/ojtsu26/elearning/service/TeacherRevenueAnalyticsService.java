package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherRevenueAnalyticsResponseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueByCourseResponseDTO;

import java.time.LocalDate;

public interface TeacherRevenueAnalyticsService {
    TeacherRevenueAnalyticsResponseDTO getRevenueAnalytics(LocalDate from,
                                                           LocalDate to,
                                                           String groupBy,
                                                           Integer courseId);

    TeacherRevenueByCourseResponseDTO getRevenueByCourse(LocalDate from,
                                                         LocalDate to,
                                                         Integer courseId);
}
