package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Integer> {
}
