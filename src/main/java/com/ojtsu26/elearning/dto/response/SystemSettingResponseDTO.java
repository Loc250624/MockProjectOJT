package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.SystemSettingType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SystemSettingResponseDTO {
    private Integer id;
    private String key;
    private String label;
    private String value;
    private SystemSettingType type;
    private String category;
    private String description;
    private String unit;
    private String storage;
    private String effectiveBehavior;
    private boolean sensitive;
    private boolean required;
    private boolean editable;
    private String minValue;
    private String maxValue;
    private List<String> options;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer updatedById;
    private String updatedByName;
}
