package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    Optional<Quiz> findByLessonId(Integer lessonId);
    boolean existsByLessonId(Integer lessonId);

    @Query("select q from Quiz q join fetch q.lesson l join fetch l.course c where c.id = :courseId order by l.orderIndex asc, q.id asc")
    List<Quiz> findByCourseId(Integer courseId);

    @Query("select q from Quiz q join fetch q.lesson l join fetch l.course c left join fetch c.instructor where q.id = :quizId")
    Optional<Quiz> findByIdWithCourse(Integer quizId);
}
