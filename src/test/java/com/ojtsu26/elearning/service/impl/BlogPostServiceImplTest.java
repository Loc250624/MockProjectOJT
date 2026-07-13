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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private BlogModerationLogRepository blogModerationLogRepository;

    private BlogPostServiceImpl blogPostService;

    @BeforeEach
    void setUp() {
        blogPostService = new BlogPostServiceImpl(blogPostRepository, blogModerationLogRepository, blogPostMapper);
    }

    @Test
    void createDraftSavesDraftForStudent() {
        BlogPostRequestDTO request = request("  Draft title  ", "  Draft content  ");
        User student = user(1, Role.STUDENT);
        BlogPostResponseDTO response = response(10, BlogPostStatus.DRAFT);

        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(blogPostMapper.toDto(any(BlogPost.class))).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.createDraft(request, student);

        ArgumentCaptor<BlogPost> captor = ArgumentCaptor.forClass(BlogPost.class);
        verify(blogPostRepository).save(captor.capture());
        BlogPost saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Draft title");
        assertThat(saved.getContent()).isEqualTo("Draft content");
        assertThat(saved.getStatus()).isEqualTo(BlogPostStatus.DRAFT);
        assertThat(saved.getAuthor()).isSameAs(student);
        assertThat(actual).isSameAs(response);
    }

    @Test
    void submitForReviewMovesOwnDraftToPendingReview() {
        User teacher = user(2, Role.TEACHER);
        BlogPost blogPost = BlogPost.builder()
                .id(20)
                .title("Draft")
                .content("Content")
                .status(BlogPostStatus.DRAFT)
                .author(teacher)
                .build();
        BlogPostResponseDTO response = response(20, BlogPostStatus.PENDING_REVIEW);

        when(blogPostRepository.findById(20)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.submitForReview(20, teacher);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.PENDING_REVIEW);
        assertThat(blogPost.getSubmittedAt()).isNotNull();
        assertThat(actual).isSameAs(response);
    }

    @Test
    void submitForReviewMovesOwnRejectedBlogToPendingReviewAndClearsReason() {
        User student = user(21, Role.STUDENT);
        BlogPost blogPost = BlogPost.builder()
                .id(21)
                .title("Rejected")
                .content("Content")
                .status(BlogPostStatus.REJECTED)
                .rejectionReason("Needs revision")
                .author(student)
                .build();
        BlogPostResponseDTO response = response(21, BlogPostStatus.PENDING_REVIEW);

        when(blogPostRepository.findById(21)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.submitForReview(21, student);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.PENDING_REVIEW);
        assertThat(blogPost.getSubmittedAt()).isNotNull();
        assertThat(blogPost.getRejectionReason()).isNull();
        assertThat(actual).isSameAs(response);
    }

    @Test
    void submitForReviewRejectsNonAuthor() {
        User owner = user(3, Role.STUDENT);
        User otherStudent = user(4, Role.STUDENT);
        BlogPost blogPost = BlogPost.builder()
                .id(30)
                .status(BlogPostStatus.DRAFT)
                .author(owner)
                .build();

        when(blogPostRepository.findById(30)).thenReturn(Optional.of(blogPost));

        assertThatThrownBy(() -> blogPostService.submitForReview(30, otherStudent))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("own blogs");
        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void createDraftRejectsAdmin() {
        assertThatThrownBy(() -> blogPostService.createDraft(request("Title", "Content"), user(5, Role.ADMIN)))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only students and teachers");
        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void updateRejectedAllowsAuthorToEditRejectedBlog() {
        User teacher = user(10, Role.TEACHER);
        BlogPost blogPost = BlogPost.builder()
                .id(50)
                .title("Old")
                .content("Old content")
                .status(BlogPostStatus.REJECTED)
                .author(teacher)
                .build();
        BlogPostResponseDTO response = response(50, BlogPostStatus.REJECTED);

        when(blogPostRepository.findById(50)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.updateRejected(50, request("  New title  ", "  New content  "), teacher);

        assertThat(blogPost.getTitle()).isEqualTo("New title");
        assertThat(blogPost.getContent()).isEqualTo("New content");
        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.REJECTED);
        assertThat(actual).isSameAs(response);
    }

    @Test
    void updateRejectedRejectsDraftBlog() {
        User student = user(11, Role.STUDENT);
        BlogPost blogPost = BlogPost.builder()
                .id(51)
                .status(BlogPostStatus.DRAFT)
                .author(student)
                .build();

        when(blogPostRepository.findById(51)).thenReturn(Optional.of(blogPost));

        assertThatThrownBy(() -> blogPostService.updateRejected(51, request("Title", "Content"), student))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only rejected");
        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void findPublishedUsesPublishedNotDeletedQuery() {
        BlogPost blogPost = BlogPost.builder()
                .id(52)
                .status(BlogPostStatus.PUBLISHED)
                .deleted(false)
                .build();
        BlogPostResponseDTO response = response(52, BlogPostStatus.PUBLISHED);

        when(blogPostRepository.findByStatusAndDeletedFalseOrderByPublishedAtDescUpdatedAtDesc(BlogPostStatus.PUBLISHED))
                .thenReturn(List.of(blogPost));
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        assertThat(blogPostService.findPublished(null)).containsExactly(response);
    }

    @Test
    void approvePublishesPendingBlogAndWritesLog() {
        User admin = user(6, Role.ADMIN);
        BlogPost blogPost = BlogPost.builder()
                .id(40)
                .status(BlogPostStatus.PENDING_REVIEW)
                .rejectionReason("Old reason")
                .build();
        BlogPostResponseDTO response = response(40, BlogPostStatus.PUBLISHED);

        when(blogPostRepository.findById(40)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.approve(40, admin);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.PUBLISHED);
        assertThat(blogPost.getPublishedAt()).isNotNull();
        assertThat(blogPost.getRejectionReason()).isNull();
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogModerationLog> captor = ArgumentCaptor.forClass(BlogModerationLog.class);
        verify(blogModerationLogRepository).save(captor.capture());
        BlogModerationLog log = captor.getValue();
        assertThat(log.getBlogPost()).isSameAs(blogPost);
        assertThat(log.getModerator()).isSameAs(admin);
        assertThat(log.getAction()).isEqualTo(BlogModerationAction.APPROVED);
        assertThat(log.getReason()).isNull();
    }

    @Test
    void rejectRequiresReason() {
        User admin = user(7, Role.ADMIN);

        assertThatThrownBy(() -> blogPostService.reject(41, " ", admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rejection reason is required");
        verify(blogPostRepository, never()).findById(41);
    }

    @Test
    void rejectMovesPendingBlogToRejectedAndWritesLog() {
        User admin = user(8, Role.ADMIN);
        BlogPost blogPost = BlogPost.builder()
                .id(42)
                .status(BlogPostStatus.PENDING_REVIEW)
                .build();
        BlogPostResponseDTO response = response(42, BlogPostStatus.REJECTED);

        when(blogPostRepository.findById(42)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.reject(42, " Needs citations. ", admin);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.REJECTED);
        assertThat(blogPost.getRejectionReason()).isEqualTo("Needs citations.");
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogModerationLog> captor = ArgumentCaptor.forClass(BlogModerationLog.class);
        verify(blogModerationLogRepository).save(captor.capture());
        BlogModerationLog log = captor.getValue();
        assertThat(log.getBlogPost()).isSameAs(blogPost);
        assertThat(log.getModerator()).isSameAs(admin);
        assertThat(log.getAction()).isEqualTo(BlogModerationAction.REJECTED);
        assertThat(log.getReason()).isEqualTo("Needs citations.");
    }

    @Test
    void hideMovesBlogToHiddenAndWritesLog() {
        User admin = user(12, Role.ADMIN);
        BlogPost blogPost = BlogPost.builder()
                .id(60)
                .status(BlogPostStatus.PUBLISHED)
                .build();
        BlogPostResponseDTO response = response(60, BlogPostStatus.HIDDEN);

        when(blogPostRepository.findById(60)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.hide(60, "  Policy issue  ", admin);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.HIDDEN);
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogModerationLog> captor = ArgumentCaptor.forClass(BlogModerationLog.class);
        verify(blogModerationLogRepository).save(captor.capture());
        BlogModerationLog log = captor.getValue();
        assertThat(log.getAction()).isEqualTo(BlogModerationAction.HIDDEN);
        assertThat(log.getReason()).isEqualTo("Policy issue");
    }

    @Test
    void archiveMovesBlogToArchivedAndWritesLog() {
        User admin = user(13, Role.ADMIN);
        BlogPost blogPost = BlogPost.builder()
                .id(61)
                .status(BlogPostStatus.HIDDEN)
                .build();
        BlogPostResponseDTO response = response(61, BlogPostStatus.ARCHIVED);

        when(blogPostRepository.findById(61)).thenReturn(Optional.of(blogPost));
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);
        when(blogPostMapper.toDto(blogPost)).thenReturn(response);

        BlogPostResponseDTO actual = blogPostService.archive(61, "Outdated", admin);

        assertThat(blogPost.getStatus()).isEqualTo(BlogPostStatus.ARCHIVED);
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogModerationLog> captor = ArgumentCaptor.forClass(BlogModerationLog.class);
        verify(blogModerationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(BlogModerationAction.ARCHIVED);
        assertThat(captor.getValue().getReason()).isEqualTo("Outdated");
    }

    @Test
    void hideRequiresReason() {
        User admin = user(14, Role.ADMIN);

        assertThatThrownBy(() -> blogPostService.hide(62, " ", admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Moderation reason is required");
        verify(blogPostRepository, never()).findById(62);
    }

    @Test
    void approveRejectsNonAdmin() {
        assertThatThrownBy(() -> blogPostService.approve(43, user(9, Role.TEACHER)))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only admins");
        verify(blogPostRepository, never()).findById(43);
    }

    private BlogPostRequestDTO request(String title, String content) {
        BlogPostRequestDTO request = new BlogPostRequestDTO();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private User user(Integer id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private BlogPostResponseDTO response(Integer id, BlogPostStatus status) {
        BlogPostResponseDTO response = new BlogPostResponseDTO();
        response.setId(id);
        response.setStatus(status);
        return response;
    }
}
