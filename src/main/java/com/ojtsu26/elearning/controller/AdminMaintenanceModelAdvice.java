package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = AdminViewController.class)
@RequiredArgsConstructor
public class AdminMaintenanceModelAdvice {

    private final SystemSettingService systemSettingService;

    @ModelAttribute("maintenanceModeActive")
    public boolean maintenanceModeActive() {
        return systemSettingService.isMaintenanceModeEnabled();
    }
}
