package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.AdminProfileOverviewDTO;
import com.ojtsu26.elearning.dto.response.TeacherProfileOverviewDTO;

public interface ProfileOverviewService {
    TeacherProfileOverviewDTO getTeacherOverview(Integer teacherId);
    AdminProfileOverviewDTO getAdminOverview();
}
