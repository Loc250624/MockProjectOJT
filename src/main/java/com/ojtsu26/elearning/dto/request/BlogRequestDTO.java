package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class BlogRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String title;

    @jakarta.validation.constraints.NotBlank
    private String content;

    @jakarta.validation.constraints.NotNull
    private BlogStatus status;

    @jakarta.validation.constraints.NotBlank
    private String rejectReason;

    @jakarta.validation.constraints.NotNull
    private Integer authorId;
}
