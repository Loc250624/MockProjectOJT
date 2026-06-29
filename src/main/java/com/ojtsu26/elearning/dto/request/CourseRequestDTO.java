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
    private java.math.BigDecimal price;

    @jakarta.validation.constraints.NotNull
    private CourseStatus status;

    @jakarta.validation.constraints.NotNull
    private Integer instructorId;

    @jakarta.validation.constraints.NotNull
    private Integer categoryId;

    @jakarta.validation.constraints.NotNull
    private Integer roadmapId;
}
