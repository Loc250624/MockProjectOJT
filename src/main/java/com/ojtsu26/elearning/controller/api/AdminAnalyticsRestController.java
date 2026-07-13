package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.AdminDashboardOverviewDTO;
import com.ojtsu26.elearning.dto.response.AdminRevenueAnalyticsDTO;
import com.ojtsu26.elearning.dto.response.AdminStudentAnalyticsDTO;
import com.ojtsu26.elearning.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAnalyticsRestController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewDTO>> dashboardOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(adminAnalyticsService.getDashboardOverview(from, to)));
    }

    @GetMapping("/analytics/students")
    public ResponseEntity<ApiResponse<AdminStudentAnalyticsDTO>> studentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success(adminAnalyticsService.getStudentAnalytics(from, to, groupBy)));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<ApiResponse<AdminRevenueAnalyticsDTO>> revenueAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success(adminAnalyticsService.getRevenueAnalytics(from, to, groupBy)));
    }
}
