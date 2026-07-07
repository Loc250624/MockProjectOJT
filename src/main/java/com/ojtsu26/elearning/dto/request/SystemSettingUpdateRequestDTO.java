package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemSettingUpdateRequestDTO {

    @NotNull(message = "value is required")
    private String value;
}
