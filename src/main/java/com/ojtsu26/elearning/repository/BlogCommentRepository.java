package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.BlogComment;
import com.ojtsu26.elearning.model.enums.BlogCommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer> {
    List<BlogComment> findByBlogPostIdAndStatusOrderByCreatedAtAsc(Integer blogPostId, BlogCommentStatus status);
    List<BlogComment> findAllByOrderByCreatedAtDesc();
}
