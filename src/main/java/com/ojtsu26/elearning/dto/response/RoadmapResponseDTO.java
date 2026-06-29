package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class RoadmapResponseDTO {
    private Integer id;

    private String title;

    private String description;

    private java.time.LocalDateTime createdAt;

    private Integer instructorId;
}
