package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.AdminTeacherPayoutDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminTeacherPayoutService {
    List<AdminTeacherPayoutDTO> findPayoutReadiness(LocalDateTime fromDate, LocalDateTime toDate);
}
