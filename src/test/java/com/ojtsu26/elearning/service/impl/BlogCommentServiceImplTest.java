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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogCommentServiceImplTest {

    @Mock
    private BlogCommentRepository blogCommentRepository;

    @Mock
    private BlogCommentModerationLogRepository blogCommentModerationLogRepository;

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private BlogCommentMapper blogCommentMapper;

    private BlogCommentServiceImpl blogCommentService;

    @BeforeEach
    void setUp() {
        blogCommentService = new BlogCommentServiceImpl(blogCommentRepository, blogCommentModerationLogRepository, blogPostRepository, blogCommentMapper);
    }

    @Test
    void findVisibleForPublishedBlogReturnsOnlyVisibleComments() {
        BlogPost blogPost = BlogPost.builder().id(1).status(BlogPostStatus.PUBLISHED).build();
        BlogComment comment = BlogComment.builder().id(2).blogPost(blogPost).status(BlogCommentStatus.VISIBLE).build();
        BlogCommentResponseDTO response = new BlogCommentResponseDTO();
        response.setId(2);

        when(blogPostRepository.findByIdAndStatusAndDeletedFalse(1, BlogPostStatus.PUBLISHED)).thenReturn(Optional.of(blogPost));
        when(blogCommentRepository.findByBlogPostIdAndStatusOrderByCreatedAtAsc(1, BlogCommentStatus.VISIBLE)).thenReturn(List.of(comment));
        when(blogCommentMapper.toDto(comment)).thenReturn(response);

        assertThat(blogCommentService.findVisibleForPublishedBlog(1)).containsExactly(response);
    }

    @Test
    void addCommentRequiresAuthenticatedUser() {
        assertThatThrownBy(() -> blogCommentService.addComment(1, request("Nice post"), null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("log in");
        verify(blogCommentRepository, never()).save(any(BlogComment.class));
    }

    @Test
    void addCommentRequiresPublishedBlog() {
        User user = new User();
        user.setId(3);

        when(blogPostRepository.findByIdAndStatusAndDeletedFalse(9, BlogPostStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogCommentService.addComment(9, request("Nice post"), user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Published blog not found");
        verify(blogCommentRepository, never()).save(any(BlogComment.class));
    }

    @Test
    void addCommentSavesVisibleComment() {
        User user = new User();
        user.setId(4);
        BlogPost blogPost = BlogPost.builder().id(5).status(BlogPostStatus.PUBLISHED).build();
        BlogCommentResponseDTO response = new BlogCommentResponseDTO();
        response.setId(6);

        when(blogPostRepository.findByIdAndStatusAndDeletedFalse(5, BlogPostStatus.PUBLISHED)).thenReturn(Optional.of(blogPost));
        when(blogCommentRepository.save(any(BlogComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(blogCommentMapper.toDto(any(BlogComment.class))).thenReturn(response);

        BlogCommentResponseDTO actual = blogCommentService.addComment(5, request("  Useful article  "), user);

        ArgumentCaptor<BlogComment> captor = ArgumentCaptor.forClass(BlogComment.class);
        verify(blogCommentRepository).save(captor.capture());
        BlogComment saved = captor.getValue();
        assertThat(saved.getBlogPost()).isSameAs(blogPost);
        assertThat(saved.getAuthor()).isSameAs(user);
        assertThat(saved.getContent()).isEqualTo("Useful article");
        assertThat(saved.getStatus()).isEqualTo(BlogCommentStatus.VISIBLE);
        assertThat(actual).isSameAs(response);
    }

    @Test
    void findAllForAdminRequiresAdmin() {
        assertThatThrownBy(() -> blogCommentService.findAllForAdmin(user(7, Role.STUDENT)))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void hideMovesVisibleCommentToHiddenAndWritesLog() {
        User admin = user(8, Role.ADMIN);
        BlogComment comment = BlogComment.builder()
                .id(10)
                .status(BlogCommentStatus.VISIBLE)
                .build();
        BlogCommentResponseDTO response = new BlogCommentResponseDTO();
        response.setId(10);
        response.setStatus(BlogCommentStatus.HIDDEN);

        when(blogCommentRepository.findById(10)).thenReturn(Optional.of(comment));
        when(blogCommentRepository.save(comment)).thenReturn(comment);
        when(blogCommentMapper.toDto(comment)).thenReturn(response);

        BlogCommentResponseDTO actual = blogCommentService.hide(10, "  Spam  ", admin);

        assertThat(comment.getStatus()).isEqualTo(BlogCommentStatus.HIDDEN);
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogCommentModerationLog> captor = ArgumentCaptor.forClass(BlogCommentModerationLog.class);
        verify(blogCommentModerationLogRepository).save(captor.capture());
        BlogCommentModerationLog log = captor.getValue();
        assertThat(log.getBlogComment()).isSameAs(comment);
        assertThat(log.getModerator()).isSameAs(admin);
        assertThat(log.getAction()).isEqualTo(BlogCommentModerationAction.HIDDEN);
        assertThat(log.getReason()).isEqualTo("Spam");
    }

    @Test
    void deleteMovesCommentToDeletedAndWritesLog() {
        User admin = user(9, Role.ADMIN);
        BlogComment comment = BlogComment.builder()
                .id(11)
                .status(BlogCommentStatus.HIDDEN)
                .build();
        BlogCommentResponseDTO response = new BlogCommentResponseDTO();
        response.setId(11);
        response.setStatus(BlogCommentStatus.DELETED);

        when(blogCommentRepository.findById(11)).thenReturn(Optional.of(comment));
        when(blogCommentRepository.save(comment)).thenReturn(comment);
        when(blogCommentMapper.toDto(comment)).thenReturn(response);

        BlogCommentResponseDTO actual = blogCommentService.delete(11, "Abuse", admin);

        assertThat(comment.getStatus()).isEqualTo(BlogCommentStatus.DELETED);
        assertThat(actual).isSameAs(response);

        ArgumentCaptor<BlogCommentModerationLog> captor = ArgumentCaptor.forClass(BlogCommentModerationLog.class);
        verify(blogCommentModerationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(BlogCommentModerationAction.DELETED);
        assertThat(captor.getValue().getReason()).isEqualTo("Abuse");
    }

    @Test
    void deleteRequiresReason() {
        User admin = user(10, Role.ADMIN);

        assertThatThrownBy(() -> blogCommentService.delete(12, " ", admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Moderation reason is required");
        verify(blogCommentRepository, never()).findById(12);
    }

    private BlogCommentRequestDTO request(String content) {
        BlogCommentRequestDTO requestDTO = new BlogCommentRequestDTO();
        requestDTO.setContent(content);
        return requestDTO;
    }

    private User user(Integer id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
