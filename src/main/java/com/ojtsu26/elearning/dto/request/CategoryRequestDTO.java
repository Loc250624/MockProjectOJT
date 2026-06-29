package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CategoryRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String name;

    @jakarta.validation.constraints.NotBlank
    private String description;
}
