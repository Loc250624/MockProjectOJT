package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.SystemSettingType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemSettingResponseDTO {
    private Integer id;
    private String key;
    private String value;
    private SystemSettingType type;
    private String category;
    private String description;
    private boolean editable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer updatedById;
    private String updatedByName;
}
