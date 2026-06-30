package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CourseRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotBlank
    private String description;

    @jakarta.validation.constraints.NotBlank
    private String thumbnailUrl;

    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(value = 0, message = "Price cannot be negative")
    private java.math.BigDecimal price;

    private CourseStatus status;

    private Integer instructorId;

    @jakarta.validation.constraints.NotNull
    private Integer categoryId;

    @jakarta.validation.constraints.NotNull
    private Integer roadmapId;
}
