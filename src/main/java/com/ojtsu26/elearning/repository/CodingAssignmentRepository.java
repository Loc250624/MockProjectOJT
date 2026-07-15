package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CodingAssignment;
import com.ojtsu26.elearning.repository.projection.StudentDeadlineProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodingAssignmentRepository extends JpaRepository<CodingAssignment, Integer> {
    Optional<CodingAssignment> findByLessonId(Integer lessonId);

    @Query("select a from CodingAssignment a join fetch a.lesson l join fetch l.course c where c.id = :courseId order by l.orderIndex asc, a.id asc")
    List<CodingAssignment> findByCourseId(Integer courseId);

    @Query("select a from CodingAssignment a join fetch a.lesson l join fetch l.course c left join fetch c.instructor where a.id = :assignmentId")
    Optional<CodingAssignment> findByIdWithCourse(Integer assignmentId);

    @Query(value = """
            select a
            from CodingAssignment a
            join fetch a.lesson l
            join fetch l.course c
            left join fetch c.instructor
            where c.instructor.id = :teacherId
            and (:courseId is null or c.id = :courseId)
            and (:status is null or lower(a.status) = lower(:status))
            and (:search is null
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(c.title) like lower(concat('%', :search, '%')))
            order by a.updatedAt desc, a.id desc
            """,
            countQuery = """
            select count(a)
            from CodingAssignment a
            join a.lesson l
            join l.course c
            where c.instructor.id = :teacherId
            and (:courseId is null or c.id = :courseId)
            and (:status is null or lower(a.status) = lower(:status))
            and (:search is null
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<CodingAssignment> findTeacherAssignments(@Param("teacherId") Integer teacherId,
                                                  @Param("courseId") Integer courseId,
                                                  @Param("status") String status,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    @Query("""
            select a.id as assignmentId,
                   a.title as title,
                   c.id as courseId,
                   c.title as courseTitle,
                   a.dueDate as dueAt
            from CodingAssignment a
            join a.lesson l
            join l.course c
            join CourseEnrollment e on e.course = c
            where e.student.id = :studentId
              and a.dueDate is not null
              and a.id not in :completedAssignmentIds
            order by a.dueDate asc, a.id asc
            """)
    List<StudentDeadlineProjection> findUpcomingDeadlinesForStudentExcludingCompleted(
            @Param("studentId") Integer studentId,
            @Param("completedAssignmentIds") Collection<Integer> completedAssignmentIds,
            Pageable pageable);
}

