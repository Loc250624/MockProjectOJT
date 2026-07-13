package com.ojtsu26.elearning.dto.response;

import com.ojtsu26.elearning.model.enums.BlogCommentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCommentResponseDTO {
    private Integer id;
    private Integer blogPostId;
    private String blogPostTitle;
    private String content;
    private BlogCommentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer authorId;
    private String authorName;
}
