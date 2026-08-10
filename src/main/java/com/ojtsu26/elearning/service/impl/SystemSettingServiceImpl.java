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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String STORAGE = "DB:SystemSettings.setting_value";
    private static final String EFFECTIVE_IMMEDIATE = "Effective immediately for code paths that read SystemSettingService; no cache layer is used.";
    private static final Map<String, SettingDefinition> DEFINITIONS = createDefinitions();

    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<SystemSettingResponseDTO> findAll() {
        ensureDefaults();
        return systemSettingRepository.findAllByOrderByCategoryAscKeyAsc()
                .stream()
                .filter(setting -> DEFINITIONS.containsKey(setting.getKey()))
                .map(this::toDto)
                .sorted(Comparator.comparing(SystemSettingResponseDTO::getCategory)
                        .thenComparing(SystemSettingResponseDTO::getKey))
                .toList();
    }

    @Override
    @Transactional
    public SystemSettingResponseDTO update(String key, String value, Integer adminUserId) {
        ensureDefaults();
        String normalizedKey = normalizeKey(key);
        SettingDefinition definition = requireDefinition(normalizedKey);
        SystemSetting setting = systemSettingRepository.findByKey(normalizedKey)
                .orElseGet(() -> createDefaultSetting(definition));
        updateSettingValue(definition, setting, value, adminUserId);
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

        Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
        Map<String, SystemSetting> settings = new LinkedHashMap<>();
        Map<String, String> normalizedSettingValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : normalizedValues.entrySet()) {
            SettingDefinition definition = requireDefinition(entry.getKey());
            SystemSetting setting = systemSettingRepository.findByKey(entry.getKey())
                    .orElseGet(() -> createDefaultSetting(definition));
            if (!definition.editable() || !setting.isEditable()) {
                throw new IllegalArgumentException("System setting is not editable: " + entry.getKey());
            }
            definitions.put(entry.getKey(), definition);
            settings.put(entry.getKey(), setting);
            normalizedSettingValues.put(entry.getKey(), validateAndNormalize(definition, entry.getValue(), setting.getValue()));
        }

        User admin = findAdmin(adminUserId);
        for (Map.Entry<String, String> entry : normalizedSettingValues.entrySet()) {
            SystemSetting setting = settings.get(entry.getKey());
            String oldValue = setting.getValue();
            String normalizedValue = entry.getValue();
            if (Objects.equals(oldValue, normalizedValue)) {
                continue;
            }
            setting.setValue(normalizedValue);
            setting.setUpdatedBy(admin);
            systemSettingRepository.save(setting);
            log.info("System setting changed. key={}, sensitive={}, adminUserId={}",
                    setting.getKey(), definitions.get(entry.getKey()).sensitive(), adminUserId);
        }

        return findAll();
    }

    private void updateSettingValue(SettingDefinition definition, SystemSetting setting, String rawValue, Integer adminUserId) {
        if (!definition.editable() || !setting.isEditable()) {
            throw new IllegalArgumentException("System setting is not editable: " + setting.getKey());
        }

        String oldValue = setting.getValue();
        String normalizedValue = validateAndNormalize(definition, rawValue, oldValue);
        if (Objects.equals(oldValue, normalizedValue)) {
            return;
        }

        setting.setValue(normalizedValue);
        setting.setUpdatedBy(findAdmin(adminUserId));
        log.info("System setting changed. key={}, sensitive={}, adminUserId={}",
                setting.getKey(), definition.sensitive(), adminUserId);
    }

    private String validateAndNormalize(SettingDefinition definition, String rawValue, String currentValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException(definition.key() + " value is required");
        }
        String value = rawValue.trim();
        if (definition.sensitive() && value.isBlank()) {
            return currentValue;
        }
        if (definition.required() && value.isBlank()) {
            throw new IllegalArgumentException(definition.key() + " must not be blank");
        }

        return switch (definition.type()) {
            case BOOLEAN -> normalizeBoolean(definition.key(), value);
            case INTEGER -> normalizeInteger(definition, value);
            case DECIMAL -> normalizeDecimal(definition, value);
            case EMAIL -> normalizeEmail(definition, value);
            case URL -> normalizeUrl(definition, value);
            case ENUM -> normalizeEnum(definition, value);
            case STRING -> normalizeString(definition, value);
        };
    }

    private String normalizeBoolean(String key, String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.equals("true") && !normalized.equals("false")) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return normalized;
    }

    private String normalizeInteger(SettingDefinition definition, String value) {
        try {
            int parsed = Integer.parseInt(value);
            validateNumberRange(definition, BigDecimal.valueOf(parsed));
            return Integer.toString(parsed);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(definition.key() + " must be a valid integer");
        }
    }

    private String normalizeDecimal(SettingDefinition definition, String value) {
        try {
            BigDecimal parsed = new BigDecimal(value).stripTrailingZeros();
            validateNumberRange(definition, parsed);
            return parsed.toPlainString();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(definition.key() + " must be a valid decimal");
        }
    }

    private void validateNumberRange(SettingDefinition definition, BigDecimal value) {
        if (definition.minValue() != null && value.compareTo(new BigDecimal(definition.minValue())) < 0) {
            throw new IllegalArgumentException(definition.key() + " must be at least " + definition.minValue());
        }
        if (definition.maxValue() != null && value.compareTo(new BigDecimal(definition.maxValue())) > 0) {
            throw new IllegalArgumentException(definition.key() + " must be at most " + definition.maxValue());
        }
    }

    private String normalizeEmail(SettingDefinition definition, String value) {
        if (value.length() > 320 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(definition.key() + " must be a valid email");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String normalizeUrl(SettingDefinition definition, String value) {
        if (value.length() > 2048) {
            throw new IllegalArgumentException(definition.key() + " must be 2048 characters or fewer");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException(definition.key() + " must be a valid HTTP or HTTPS URL");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(definition.key() + " must be a valid HTTP or HTTPS URL");
        }
    }

    private String normalizeEnum(SettingDefinition definition, String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!definition.options().contains(normalized)) {
            throw new IllegalArgumentException(definition.key() + " must be one of: " + String.join(", ", definition.options()));
        }
        return normalized;
    }

    private String normalizeString(SettingDefinition definition, String value) {
        if (value.contains("<") || value.contains(">")) {
            throw new IllegalArgumentException(definition.key() + " must not contain HTML markup");
        }
        if (definition.maxValue() != null && value.length() > Integer.parseInt(definition.maxValue())) {
            throw new IllegalArgumentException(definition.key() + " must be " + definition.maxValue() + " characters or fewer");
        }
        if (definition.minValue() != null && value.length() < Integer.parseInt(definition.minValue())) {
            throw new IllegalArgumentException(definition.key() + " must be at least " + definition.minValue() + " characters");
        }
        return value;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("setting key is required");
        }
        return key.trim();
    }

    private SettingDefinition requireDefinition(String key) {
        SettingDefinition definition = DEFINITIONS.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("System setting key is not whitelisted: " + key);
        }
        return definition;
    }

    private User findAdmin(Integer adminUserId) {
        return userRepository.findById(adminUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void ensureDefaults() {
        for (SettingDefinition definition : DEFINITIONS.values()) {
            SystemSetting setting = systemSettingRepository.findByKey(definition.key())
                    .orElseGet(() -> systemSettingRepository.save(createDefaultSetting(definition)));
            applyDefinitionMetadata(setting, definition);
        }
    }

    private SystemSetting createDefaultSetting(SettingDefinition definition) {
        return SystemSetting.builder()
                .key(definition.key())
                .value(definition.defaultValue())
                .type(definition.type())
                .category(definition.category())
                .description(definition.description())
                .editable(definition.editable())
                .build();
    }

    private void applyDefinitionMetadata(SystemSetting setting, SettingDefinition definition) {
        boolean changed = false;
        if (setting.getType() != definition.type()) {
            setting.setType(definition.type());
            changed = true;
        }
        if (!Objects.equals(setting.getCategory(), definition.category())) {
            setting.setCategory(definition.category());
            changed = true;
        }
        if (!Objects.equals(setting.getDescription(), definition.description())) {
            setting.setDescription(definition.description());
            changed = true;
        }
        if (setting.isEditable() != definition.editable()) {
            setting.setEditable(definition.editable());
            changed = true;
        }
        if (changed) {
            systemSettingRepository.save(setting);
        }
    }

    private SystemSettingResponseDTO toDto(SystemSetting setting) {
        SettingDefinition definition = DEFINITIONS.get(setting.getKey());
        User updatedBy = setting.getUpdatedBy();
        return SystemSettingResponseDTO.builder()
                .id(setting.getId())
                .key(setting.getKey())
                .label(definition.label())
                .value(definition.sensitive() ? "" : setting.getValue())
                .type(definition.type())
                .category(definition.category())
                .description(definition.description())
                .unit(definition.unit())
                .storage(definition.storage())
                .effectiveBehavior(definition.effectiveBehavior())
                .sensitive(definition.sensitive())
                .required(definition.required())
                .editable(definition.editable() && setting.isEditable())
                .minValue(definition.minValue())
                .maxValue(definition.maxValue())
                .options(definition.options())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .updatedById(updatedBy == null ? null : updatedBy.getId())
                .updatedByName(updatedBy == null ? null : updatedBy.getFullName())
                .build();
    }

    private static Map<String, SettingDefinition> createDefinitions() {
        List<SettingDefinition> definitions = List.of(
                new SettingDefinition(
                        "site.name",
                        "LumiNa E-Learning",
                        SystemSettingType.STRING,
                        "Site",
                        "Site name",
                        "Public site name shown across the platform.",
                        true,
                        false,
                        true,
                        "3",
                        "120",
                        List.of(),
                        null,
                        STORAGE,
                        EFFECTIVE_IMMEDIATE),
                new SettingDefinition(
                        "site.maintenanceMode",
                        "false",
                        SystemSettingType.BOOLEAN,
                        "Site",
                        "Maintenance mode",
                        "Operational flag reserved for maintenance-aware screens.",
                        true,
                        false,
                        true,
                        null,
                        null,
                        List.of(),
                        null,
                        STORAGE,
                        EFFECTIVE_IMMEDIATE),
                new SettingDefinition(
                        "site.supportEmail",
                        "support@lumina.edu.vn",
                        SystemSettingType.EMAIL,
                        "Site",
                        "Support email",
                        "Support contact email shown to learners and teachers.",
                        true,
                        false,
                        true,
                        null,
                        "320",
                        List.of(),
                        null,
                        STORAGE,
                        EFFECTIVE_IMMEDIATE),
                new SettingDefinition(
                        "commerce.currency",
                        "VND",
                        SystemSettingType.ENUM,
                        "Commerce",
                        "Default currency",
                        "Currency used for course prices and revenue analytics outside payment gateway screens. Orders keep USD business amounts and payment gateways settle in VND.",
                        true,
                        false,
                        true,
                        null,
                        null,
                        List.of("VND", "USD"),
                        null,
                        STORAGE,
                        "Applies immediately to catalog, admin/teacher revenue dashboards, and non-payment course price displays."),
                new SettingDefinition(
                        "commerce.teacherCommissionRate",
                        "0.70",
                        SystemSettingType.DECIMAL,
                        "Commerce",
                        "Teacher commission rate",
                        "Teacher commission rate as a decimal fraction.",
                        true,
                        false,
                        true,
                        "0",
                        "1",
                        List.of(),
                        "fraction",
                        STORAGE,
                        EFFECTIVE_IMMEDIATE),
                new SettingDefinition(
                        "learning.defaultEnrollmentStatus",
                        "ACTIVE",
                        SystemSettingType.ENUM,
                        "Learning",
                        "Default enrollment status",
                        "Administrative default used by learning workflows that opt in to this setting.",
                        true,
                        false,
                        true,
                        null,
                        null,
                        List.of("ACTIVE", "INACTIVE"),
                        null,
                        STORAGE,
                        EFFECTIVE_IMMEDIATE)
        );
        return definitions.stream()
                .collect(Collectors.toMap(SettingDefinition::key, definition -> definition, (a, b) -> a, LinkedHashMap::new));
    }

    private record SettingDefinition(
            String key,
            String defaultValue,
            SystemSettingType type,
            String category,
            String label,
            String description,
            boolean editable,
            boolean sensitive,
            boolean required,
            String minValue,
            String maxValue,
            List<String> options,
            String unit,
            String storage,
            String effectiveBehavior) {
    }
}
