package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.AdminDashboardOverviewDTO;
import com.ojtsu26.elearning.dto.response.AdminRevenueAnalyticsDTO;
import com.ojtsu26.elearning.dto.response.AdminStudentAnalyticsDTO;

import java.time.LocalDate;

public interface AdminAnalyticsService {
    AdminDashboardOverviewDTO getDashboardOverview(LocalDate from, LocalDate to);

    AdminStudentAnalyticsDTO getStudentAnalytics(LocalDate from, LocalDate to, String groupBy);

    AdminRevenueAnalyticsDTO getRevenueAnalytics(LocalDate from, LocalDate to, String groupBy);
}
