package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.SystemSettingResponseDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SystemSettingService {
    List<SystemSettingResponseDTO> findAll();
    SystemSettingResponseDTO update(String key, String value, Integer adminUserId);
    List<SystemSettingResponseDTO> updateBulk(Map<String, String> values, Integer adminUserId);
    boolean isMaintenanceModeEnabled();
    BigDecimal getTeacherCommissionRate();
}
