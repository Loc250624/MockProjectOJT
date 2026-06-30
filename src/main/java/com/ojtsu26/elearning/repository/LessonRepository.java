package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Integer courseId);
}
