package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCategoryReassignRequestDTO {
    @NotNull
    private Integer replacementCategoryId;
}
