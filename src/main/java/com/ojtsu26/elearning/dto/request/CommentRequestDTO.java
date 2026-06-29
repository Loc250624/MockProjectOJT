package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class CommentRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String targetType;

    @jakarta.validation.constraints.NotNull
    private Integer targetId;

    @jakarta.validation.constraints.NotBlank
    private String content;

    @jakarta.validation.constraints.NotNull
    private Integer userId;

    @jakarta.validation.constraints.NotNull
    private Integer parentId;
}
