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

    private java.math.BigDecimal displayPrice;

    private String displayCurrency;

    private String priceDisplay;

    private CourseStatus status;

    private String rejectReason;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    private Integer instructorId;

    private String instructorName;

    private Integer categoryId;

    private String categoryName;

    private Integer roadmapId;

    private String roadmapTitle;

    private String enrollmentStatus;

    private long estimatedDurationSeconds;

    private String estimatedDurationDisplay;
}
