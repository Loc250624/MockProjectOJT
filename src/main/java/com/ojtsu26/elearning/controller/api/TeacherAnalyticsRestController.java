package com.ojtsu26.elearning.controller.api;

import com.ojtsu26.elearning.common.ApiResponse;
import com.ojtsu26.elearning.dto.response.TeacherRevenueAnalyticsResponseDTO;
import com.ojtsu26.elearning.dto.response.TeacherRevenueByCourseResponseDTO;
import com.ojtsu26.elearning.service.TeacherRevenueAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/teacher/analytics")
@RequiredArgsConstructor
public class TeacherAnalyticsRestController {

    private final TeacherRevenueAnalyticsService teacherRevenueAnalyticsService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<TeacherRevenueAnalyticsResponseDTO>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                teacherRevenueAnalyticsService.getRevenueAnalytics(from, to, groupBy, courseId)));
    }

    @GetMapping("/revenue/by-course")
    public ResponseEntity<ApiResponse<TeacherRevenueByCourseResponseDTO>> revenueByCourse(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer courseId) {
        return ResponseEntity.ok(ApiResponse.success(
                teacherRevenueAnalyticsService.getRevenueByCourse(from, to, courseId)));
    }
}
