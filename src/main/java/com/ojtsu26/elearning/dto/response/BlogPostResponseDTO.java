package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.BlogPostStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogPostResponseDTO {
    private Integer id;
    private String title;
    private String content;
    private BlogPostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime publishedAt;
    private String rejectionReason;
    private boolean deleted;
    private Integer authorId;
    private String authorName;
}
