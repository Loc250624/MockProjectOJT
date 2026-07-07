package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.BlogCommentRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogCommentResponseDTO;
import com.ojtsu26.elearning.model.entity.User;

import java.util.List;

public interface BlogCommentService {
    List<BlogCommentResponseDTO> findVisibleForPublishedBlog(Integer blogPostId);
    List<BlogCommentResponseDTO> findAllForAdmin(User currentUser);
    BlogCommentResponseDTO addComment(Integer blogPostId, BlogCommentRequestDTO requestDTO, User currentUser);
    BlogCommentResponseDTO hide(Integer commentId, String reason, User currentUser);
    BlogCommentResponseDTO delete(Integer commentId, String reason, User currentUser);
}
