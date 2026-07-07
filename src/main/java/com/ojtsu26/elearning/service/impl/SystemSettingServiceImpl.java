package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.SystemSettingResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.SystemSetting;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.SystemSettingType;
import com.ojtsu26.elearning.repository.SystemSettingRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final int MAX_STRING_LENGTH = 500;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<SystemSettingResponseDTO> findAll() {
        ensureDefaults();
        return systemSettingRepository.findAllByOrderByCategoryAscKeyAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SystemSettingResponseDTO update(String key, String value, Integer adminUserId) {
        ensureDefaults();
        SystemSetting setting = systemSettingRepository.findByKey(normalizeKey(key))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "System setting does not exist"));
        updateSettingValue(setting, value, adminUserId);
        return toDto(systemSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public List<SystemSettingResponseDTO> updateBulk(Map<String, String> values, Integer adminUserId) {
        ensureDefaults();
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }

        Map<String, String> normalizedValues = new LinkedHashMap<>();
        values.forEach((key, value) -> normalizedValues.put(normalizeKey(key), value));

        for (Map.Entry<String, String> entry : normalizedValues.entrySet()) {
            SystemSetting setting = systemSettingRepository.findByKey(entry.getKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "System setting does not exist: " + entry.getKey()));
            updateSettingValue(setting, entry.getValue(), adminUserId);
            systemSettingRepository.save(setting);
        }

        return systemSettingRepository.findAllByOrderByCategoryAscKeyAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void updateSettingValue(SystemSetting setting, String rawValue, Integer adminUserId) {
        if (!setting.isEditable()) {
            throw new IllegalArgumentException("System setting is not editable: " + setting.getKey());
        }

        String oldValue = setting.getValue();
        String normalizedValue = validateAndNormalize(setting, rawValue);
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        setting.setValue(normalizedValue);
        setting.setUpdatedBy(admin);

        if (!oldValue.equals(normalizedValue)) {
            log.info("System setting changed. key={}, oldValue={}, newValue={}, adminUserId={}",
                    setting.getKey(), oldValue, normalizedValue, adminUserId);
        }
    }

    private String validateAndNormalize(SystemSetting setting, String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException(setting.getKey() + " value is required");
        }
        String value = rawValue.trim();

        return switch (setting.getType()) {
            case BOOLEAN -> normalizeBoolean(setting.getKey(), value);
            case INTEGER -> normalizeInteger(setting.getKey(), value);
            case DECIMAL -> normalizeDecimal(setting.getKey(), value);
            case EMAIL -> normalizeEmail(setting.getKey(), value);
            case STRING -> normalizeString(setting.getKey(), value);
        };
    }

    private String normalizeBoolean(String key, String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return normalized;
    }

    private String normalizeInteger(String key, String value) {
        try {
            return Integer.toString(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a valid integer");
        }
    }

    private String normalizeDecimal(String key, String value) {
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be a valid decimal");
        }
    }

    private String normalizeEmail(String key, String value) {
        if (value.length() > 320 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(key + " must be a valid email");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String normalizeString(String key, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        if (value.length() > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(key + " must be " + MAX_STRING_LENGTH + " characters or fewer");
        }
        return value;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("setting key is required");
        }
        return key.trim();
    }

    private void ensureDefaults() {
        for (DefaultSetting definition : defaultSettings()) {
            if (!systemSettingRepository.existsByKey(definition.key())) {
                systemSettingRepository.save(SystemSetting.builder()
                        .key(definition.key())
                        .value(definition.value())
                        .type(definition.type())
                        .category(definition.category())
                        .description(definition.description())
                        .editable(true)
                        .build());
            }
        }
    }

    private List<DefaultSetting> defaultSettings() {
        return List.of(
                new DefaultSetting("site.name", "LumiNa E-Learning", SystemSettingType.STRING, "Site", "Public site name shown across the platform."),
                new DefaultSetting("site.maintenanceMode", "false", SystemSettingType.BOOLEAN, "Site", "Temporarily place the public site in maintenance mode."),
                new DefaultSetting("site.supportEmail", "support@lumina.edu.vn", SystemSettingType.EMAIL, "Site", "Support contact email shown to learners and teachers."),
                new DefaultSetting("commerce.currency", "VND", SystemSettingType.STRING, "Commerce", "Default currency code for course commerce."),
                new DefaultSetting("commerce.teacherCommissionRate", "0.70", SystemSettingType.DECIMAL, "Commerce", "Teacher commission rate as a decimal fraction."),
                new DefaultSetting("learning.defaultEnrollmentStatus", "ACTIVE", SystemSettingType.STRING, "Learning", "Default enrollment status used by learning workflows.")
        );
    }

    private SystemSettingResponseDTO toDto(SystemSetting setting) {
        User updatedBy = setting.getUpdatedBy();
        return SystemSettingResponseDTO.builder()
                .id(setting.getId())
                .key(setting.getKey())
                .value(setting.getValue())
                .type(setting.getType())
                .category(setting.getCategory())
                .description(setting.getDescription())
                .editable(setting.isEditable())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .updatedById(updatedBy == null ? null : updatedBy.getId())
                .updatedByName(updatedBy == null ? null : updatedBy.getFullName())
                .build();
    }

    private record DefaultSetting(String key, String value, SystemSettingType type, String category, String description) {
    }
}
