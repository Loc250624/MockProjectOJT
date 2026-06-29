package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CommentResponseDTO {
    private Integer id;

    private String targetType;

    private Integer targetId;

    private String content;

    private java.time.LocalDateTime createdAt;

    private Integer userId;

    private Integer parentId;
}
