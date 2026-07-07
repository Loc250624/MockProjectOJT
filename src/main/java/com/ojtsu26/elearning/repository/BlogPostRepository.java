package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.BlogPost;
import com.ojtsu26.elearning.model.enums.BlogPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {
    List<BlogPost> findByAuthorIdOrderByUpdatedAtDesc(Integer authorId);
    List<BlogPost> findByStatusOrderBySubmittedAtAscUpdatedAtAsc(BlogPostStatus status);
    List<BlogPost> findAllByOrderByUpdatedAtDesc();

    List<BlogPost> findByStatusAndDeletedFalseOrderByPublishedAtDescUpdatedAtDesc(BlogPostStatus status);

    @Query("""
            select b from BlogPost b
            where b.status = :status
              and b.deleted = false
            and lower(b.title) like lower(concat('%', :keyword, '%'))
            order by b.publishedAt desc, b.updatedAt desc
            """)
    List<BlogPost> searchPublishedByTitle(@Param("status") BlogPostStatus status, @Param("keyword") String keyword);

    Optional<BlogPost> findByIdAndStatusAndDeletedFalse(Integer id, BlogPostStatus status);
}
