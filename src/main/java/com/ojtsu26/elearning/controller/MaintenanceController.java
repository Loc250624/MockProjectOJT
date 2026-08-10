package com.ojtsu26.elearning.controller;

import com.ojtsu26.elearning.dto.response.SystemSettingResponseDTO;
import com.ojtsu26.elearning.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
public class MaintenanceController {

    private static final String SUPPORT_EMAIL_KEY = "site.supportEmail";

    private final SystemSettingService systemSettingService;

    @GetMapping("/maintenance")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String maintenance(Model model) {
        model.addAttribute("supportEmail", supportEmail());
        return "maintenance";
    }

    private String supportEmail() {
        try {
            return systemSettingService.findAll().stream()
                    .filter(setting -> SUPPORT_EMAIL_KEY.equals(setting.getKey()))
                    .map(SystemSettingResponseDTO::getValue)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
