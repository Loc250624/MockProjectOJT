package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.BlogPostRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogPostResponseDTO;
import com.ojtsu26.elearning.model.entity.User;

import java.util.List;

public interface BlogPostService {
    List<BlogPostResponseDTO> findMine(User currentUser);
    List<BlogPostResponseDTO> findAllForAdmin(User currentUser);
    List<BlogPostResponseDTO> findPendingReviewForAdmin(User currentUser);
    List<BlogPostResponseDTO> findPublished(String keyword);
    BlogPostResponseDTO findPublishedById(Integer blogPostId);
    BlogPostResponseDTO findEditableRejectedByAuthor(Integer blogPostId, User currentUser);
    BlogPostResponseDTO createDraft(BlogPostRequestDTO requestDTO, User currentUser);
    BlogPostResponseDTO updateRejected(Integer blogPostId, BlogPostRequestDTO requestDTO, User currentUser);
    BlogPostResponseDTO submitForReview(Integer blogPostId, User currentUser);
    BlogPostResponseDTO approve(Integer blogPostId, User currentUser);
    BlogPostResponseDTO reject(Integer blogPostId, String rejectionReason, User currentUser);
    BlogPostResponseDTO hide(Integer blogPostId, String reason, User currentUser);
    BlogPostResponseDTO archive(Integer blogPostId, String reason, User currentUser);
}
