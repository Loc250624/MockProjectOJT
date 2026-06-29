package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class BlogResponseDTO {
    private Integer id;

    private String title;

    private String content;

    private BlogStatus status;

    private String rejectReason;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    private Integer authorId;
}
