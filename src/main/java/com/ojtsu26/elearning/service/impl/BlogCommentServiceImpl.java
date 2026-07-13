package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.BlogCommentRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogCommentResponseDTO;
import com.ojtsu26.elearning.mapper.BlogCommentMapper;
import com.ojtsu26.elearning.model.entity.BlogComment;
import com.ojtsu26.elearning.model.entity.BlogCommentModerationLog;
import com.ojtsu26.elearning.model.entity.BlogPost;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.BlogCommentModerationAction;
import com.ojtsu26.elearning.model.enums.BlogCommentStatus;
import com.ojtsu26.elearning.model.enums.BlogPostStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.BlogCommentModerationLogRepository;
import com.ojtsu26.elearning.repository.BlogCommentRepository;
import com.ojtsu26.elearning.repository.BlogPostRepository;
import com.ojtsu26.elearning.service.BlogCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentRepository blogCommentRepository;
    private final BlogCommentModerationLogRepository blogCommentModerationLogRepository;
    private final BlogPostRepository blogPostRepository;
    private final BlogCommentMapper blogCommentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BlogCommentResponseDTO> findVisibleForPublishedBlog(Integer blogPostId) {
        ensurePublishedBlog(blogPostId);
        return blogCommentRepository.findByBlogPostIdAndStatusOrderByCreatedAtAsc(blogPostId, BlogCommentStatus.VISIBLE)
                .stream()
                .map(blogCommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogCommentResponseDTO> findAllForAdmin(User currentUser) {
        ensureAdmin(currentUser);
        return blogCommentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(blogCommentMapper::toDto)
                .toList();
    }

    @Override
    public BlogCommentResponseDTO addComment(Integer blogPostId, BlogCommentRequestDTO requestDTO, User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new SecurityException("Please log in to comment.");
        }
        String cleanContent = normalizeContent(requestDTO == null ? null : requestDTO.getContent());
        BlogPost blogPost = ensurePublishedBlog(blogPostId);

        BlogComment comment = BlogComment.builder()
                .blogPost(blogPost)
                .author(currentUser)
                .content(cleanContent)
                .status(BlogCommentStatus.VISIBLE)
                .build();

        return blogCommentMapper.toDto(blogCommentRepository.save(comment));
    }

    @Override
    public BlogCommentResponseDTO hide(Integer commentId, String reason, User currentUser) {
        ensureAdmin(currentUser);
        String cleanReason = normalizeModerationReason(reason);
        BlogComment comment = findComment(commentId);
        if (comment.getStatus() == BlogCommentStatus.DELETED) {
            throw new IllegalStateException("Deleted comments cannot be hidden.");
        }

        comment.setStatus(BlogCommentStatus.HIDDEN);
        BlogComment saved = blogCommentRepository.save(comment);
        logModeration(saved, currentUser, BlogCommentModerationAction.HIDDEN, cleanReason);
        return blogCommentMapper.toDto(saved);
    }

    @Override
    public BlogCommentResponseDTO delete(Integer commentId, String reason, User currentUser) {
        ensureAdmin(currentUser);
        String cleanReason = normalizeModerationReason(reason);
        BlogComment comment = findComment(commentId);

        comment.setStatus(BlogCommentStatus.DELETED);
        BlogComment saved = blogCommentRepository.save(comment);
        logModeration(saved, currentUser, BlogCommentModerationAction.DELETED, cleanReason);
        return blogCommentMapper.toDto(saved);
    }

    private BlogPost ensurePublishedBlog(Integer blogPostId) {
        return blogPostRepository.findByIdAndStatusAndDeletedFalse(blogPostId, BlogPostStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Published blog not found."));
    }

    private BlogComment findComment(Integer commentId) {
        return blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Blog comment not found."));
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment is required.");
        }
        String cleanContent = content.trim();
        if (cleanContent.length() > 2000) {
            throw new IllegalArgumentException("Comment must not exceed 2000 characters.");
        }
        return cleanContent;
    }

    private String normalizeModerationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Moderation reason is required.");
        }
        String cleanReason = reason.trim();
        if (cleanReason.length() > 1000) {
            throw new IllegalArgumentException("Moderation reason must not exceed 1000 characters.");
        }
        return cleanReason;
    }

    private void logModeration(BlogComment comment, User moderator, BlogCommentModerationAction action, String reason) {
        blogCommentModerationLogRepository.save(BlogCommentModerationLog.builder()
                .blogComment(comment)
                .moderator(moderator)
                .action(action)
                .reason(reason)
                .build());
    }

    private void ensureAdmin(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null) {
            throw new SecurityException("Authentication is required.");
        }
        if (currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can moderate comments.");
        }
    }
}
