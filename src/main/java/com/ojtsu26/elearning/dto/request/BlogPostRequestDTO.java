package com.ojtsu26.elearning.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogPostRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 180, message = "Title must not exceed 180 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 20000, message = "Content must not exceed 20000 characters")
    private String content;
}
