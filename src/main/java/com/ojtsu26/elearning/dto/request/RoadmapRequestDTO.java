package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class RoadmapRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotBlank
    private String description;

    private Integer instructorId;
}
