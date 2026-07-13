package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    Optional<Quiz> findByLessonId(Integer lessonId);
}
