package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Integer> {
    Optional<Video> findByLessonId(Integer lessonId);
}
