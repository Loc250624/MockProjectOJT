package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.StudentDashboardStatsDTO;
import com.ojtsu26.elearning.dto.response.StudentDeadlineDashboardDTO;

import java.util.List;

public interface StudentDashboardService {
    StudentDashboardStatsDTO getCurrentStudentStats();
    List<StudentDeadlineDashboardDTO> getCurrentStudentDeadlines(int limit);
}
