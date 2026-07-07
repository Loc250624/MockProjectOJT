package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.BlogPostRequestDTO;
import com.ojtsu26.elearning.dto.response.BlogPostResponseDTO;
import com.ojtsu26.elearning.mapper.BlogPostMapper;
import com.ojtsu26.elearning.model.entity.BlogModerationLog;
import com.ojtsu26.elearning.model.entity.BlogPost;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.BlogModerationAction;
import com.ojtsu26.elearning.model.enums.BlogPostStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.BlogModerationLogRepository;
import com.ojtsu26.elearning.repository.BlogPostRepository;
import com.ojtsu26.elearning.service.BlogPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepository;
    private final BlogModerationLogRepository blogModerationLogRepository;
    private final BlogPostMapper blogPostMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponseDTO> findMine(User currentUser) {
        ensureCanWriteBlogs(currentUser);
        return blogPostRepository.findByAuthorIdOrderByUpdatedAtDesc(currentUser.getId()).stream()
                .map(blogPostMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponseDTO> findAllForAdmin(User currentUser) {
        ensureAdmin(currentUser);
        return blogPostRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(blogPostMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponseDTO> findPendingReviewForAdmin(User currentUser) {
        ensureAdmin(currentUser);
        return blogPostRepository.findByStatusOrderBySubmittedAtAscUpdatedAtAsc(BlogPostStatus.PENDING_REVIEW).stream()
                .map(blogPostMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostResponseDTO> findPublished(String keyword) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        List<BlogPost> blogPosts = cleanKeyword.isBlank()
                ? blogPostRepository.findByStatusAndDeletedFalseOrderByPublishedAtDescUpdatedAtDesc(BlogPostStatus.PUBLISHED)
                : blogPostRepository.searchPublishedByTitle(BlogPostStatus.PUBLISHED, cleanKeyword);
        return blogPosts.stream()
                .map(blogPostMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponseDTO findPublishedById(Integer blogPostId) {
        return blogPostRepository.findByIdAndStatusAndDeletedFalse(blogPostId, BlogPostStatus.PUBLISHED)
                .map(blogPostMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Published blog not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponseDTO findEditableRejectedByAuthor(Integer blogPostId, User currentUser) {
        BlogPost blogPost = findAuthorBlogPost(blogPostId, currentUser, "edit");
        if (blogPost.getStatus() != BlogPostStatus.REJECTED) {
            throw new IllegalStateException("Only rejected blogs can be edited here.");
        }
        return blogPostMapper.toDto(blogPost);
    }

    @Override
    public BlogPostResponseDTO createDraft(BlogPostRequestDTO requestDTO, User currentUser) {
        ensureCanWriteBlogs(currentUser);

        BlogPost blogPost = BlogPost.builder()
                .title(normalizeTitle(requestDTO))
                .content(normalizeContent(requestDTO))
                .status(BlogPostStatus.DRAFT)
                .author(currentUser)
                .build();

        return blogPostMapper.toDto(blogPostRepository.save(blogPost));
    }

    @Override
    public BlogPostResponseDTO updateRejected(Integer blogPostId, BlogPostRequestDTO requestDTO, User currentUser) {
        BlogPost blogPost = findAuthorBlogPost(blogPostId, currentUser, "edit");
        if (blogPost.getStatus() != BlogPostStatus.REJECTED) {
            throw new IllegalStateException("Only rejected blogs can be edited.");
        }

        blogPost.setTitle(normalizeTitle(requestDTO));
        blogPost.setContent(normalizeContent(requestDTO));
        return blogPostMapper.toDto(blogPostRepository.save(blogPost));
    }

    @Override
    public BlogPostResponseDTO submitForReview(Integer blogPostId, User currentUser) {
        BlogPost blogPost = findAuthorBlogPost(blogPostId, currentUser, "submit");

        if (blogPost.getStatus() != BlogPostStatus.DRAFT && blogPost.getStatus() != BlogPostStatus.REJECTED) {
            throw new IllegalStateException("Only draft or rejected blogs can be submitted for review.");
        }

        blogPost.setStatus(BlogPostStatus.PENDING_REVIEW);
        blogPost.setSubmittedAt(LocalDateTime.now());
        blogPost.setPublishedAt(null);
        blogPost.setRejectionReason(null);
        return blogPostMapper.toDto(blogPostRepository.save(blogPost));
    }

    @Override
    public BlogPostResponseDTO approve(Integer blogPostId, User currentUser) {
        ensureAdmin(currentUser);
        BlogPost blogPost = findPendingBlogPost(blogPostId);

        blogPost.setStatus(BlogPostStatus.PUBLISHED);
        blogPost.setPublishedAt(LocalDateTime.now());
        blogPost.setRejectionReason(null);
        BlogPost saved = blogPostRepository.save(blogPost);
        logModeration(saved, currentUser, BlogModerationAction.APPROVED, null);
        return blogPostMapper.toDto(saved);
    }

    @Override
    public BlogPostResponseDTO reject(Integer blogPostId, String rejectionReason, User currentUser) {
        ensureAdmin(currentUser);
        String cleanReason = normalizeRejectionReason(rejectionReason);
        BlogPost blogPost = findPendingBlogPost(blogPostId);

        blogPost.setStatus(BlogPostStatus.REJECTED);
        blogPost.setRejectionReason(cleanReason);
        BlogPost saved = blogPostRepository.save(blogPost);
        logModeration(saved, currentUser, BlogModerationAction.REJECTED, cleanReason);
        return blogPostMapper.toDto(saved);
    }

    @Override
    public BlogPostResponseDTO hide(Integer blogPostId, String reason, User currentUser) {
        ensureAdmin(currentUser);
        String cleanReason = normalizeModerationReason(reason);
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found."));
        if (blogPost.getStatus() == BlogPostStatus.ARCHIVED) {
            throw new IllegalStateException("Archived blogs cannot be hidden.");
        }

        blogPost.setStatus(BlogPostStatus.HIDDEN);
        BlogPost saved = blogPostRepository.save(blogPost);
        logModeration(saved, currentUser, BlogModerationAction.HIDDEN, cleanReason);
        return blogPostMapper.toDto(saved);
    }

    @Override
    public BlogPostResponseDTO archive(Integer blogPostId, String reason, User currentUser) {
        ensureAdmin(currentUser);
        String cleanReason = normalizeModerationReason(reason);
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found."));

        blogPost.setStatus(BlogPostStatus.ARCHIVED);
        BlogPost saved = blogPostRepository.save(blogPost);
        logModeration(saved, currentUser, BlogModerationAction.ARCHIVED, cleanReason);
        return blogPostMapper.toDto(saved);
    }

    private BlogPost findPendingBlogPost(Integer blogPostId) {
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found."));
        if (blogPost.getStatus() != BlogPostStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only blogs pending review can be moderated.");
        }
        return blogPost;
    }

    private String normalizeRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        String cleanReason = rejectionReason.trim();
        if (cleanReason.length() > 1000) {
            throw new IllegalArgumentException("Rejection reason must not exceed 1000 characters.");
        }
        return cleanReason;
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

    private void logModeration(BlogPost blogPost, User moderator, BlogModerationAction action, String reason) {
        blogModerationLogRepository.save(BlogModerationLog.builder()
                .blogPost(blogPost)
                .moderator(moderator)
                .action(action)
                .reason(reason)
                .build());
    }

    private BlogPost findAuthorBlogPost(Integer blogPostId, User currentUser, String action) {
        ensureCanWriteBlogs(currentUser);
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new IllegalArgumentException("Blog post not found."));
        if (blogPost.getAuthor() == null || !blogPost.getAuthor().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only " + action + " your own blogs.");
        }
        return blogPost;
    }

    private String normalizeTitle(BlogPostRequestDTO requestDTO) {
        String title = requestDTO == null ? null : requestDTO.getTitle();
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        String cleanTitle = title.trim();
        if (cleanTitle.length() > 180) {
            throw new IllegalArgumentException("Title must not exceed 180 characters.");
        }
        return cleanTitle;
    }

    private String normalizeContent(BlogPostRequestDTO requestDTO) {
        String content = requestDTO == null ? null : requestDTO.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content is required.");
        }
        String cleanContent = content.trim();
        if (cleanContent.length() > 20000) {
            throw new IllegalArgumentException("Content must not exceed 20000 characters.");
        }
        return cleanContent;
    }

    private void ensureCanWriteBlogs(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null) {
            throw new SecurityException("Authentication is required.");
        }
        if (currentUser.getRole() != Role.STUDENT && currentUser.getRole() != Role.TEACHER) {
            throw new SecurityException("Only students and teachers can write blogs.");
        }
    }

    private void ensureAdmin(User currentUser) {
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() == null) {
            throw new SecurityException("Authentication is required.");
        }
        if (currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can moderate blogs.");
        }
    }
}
