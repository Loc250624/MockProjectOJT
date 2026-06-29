package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CourseResponseDTO {
    private Integer id;

    private String title;

    private String description;

    private String thumbnailUrl;

    private java.math.BigDecimal price;

    private CourseStatus status;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    private Integer instructorId;

    private Integer categoryId;

    private Integer roadmapId;
}
