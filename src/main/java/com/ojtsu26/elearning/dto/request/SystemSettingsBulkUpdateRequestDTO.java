package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SystemSettingsBulkUpdateRequestDTO {

    @NotNull(message = "values are required")
    private Map<String, String> values;
}
