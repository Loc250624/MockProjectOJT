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
    boolean existsByLessonId(Integer lessonId);

    @Query("""
            select a
            from CodingAssignment a
            left join fetch a.course ac
            left join fetch a.lesson l
            left join fetch l.course lc
            where ac.id = :courseId or lc.id = :courseId
            order by a.dueDate asc, a.id asc
            """)
    List<CodingAssignment> findByCourseId(Integer courseId);

    @Query("""
            select distinct a
            from CodingAssignment a
            left join fetch a.course ac
            left join fetch ac.instructor
            left join fetch a.lesson l
            left join fetch l.course lc
            left join fetch lc.instructor
            left join fetch a.assignees assignee
            left join fetch assignee.student
            where a.id = :assignmentId
            """)
    Optional<CodingAssignment> findByIdWithCourse(Integer assignmentId);

    @Query(value = """
            select a
            from CodingAssignment a
            left join fetch a.course ac
            left join fetch ac.instructor
            left join fetch a.lesson l
            left join fetch l.course lc
            left join fetch lc.instructor
            where (ac.instructor.id = :teacherId or lc.instructor.id = :teacherId)
            and (:courseId is null or ac.id = :courseId or lc.id = :courseId)
            and (:status is null or lower(a.status) = lower(:status))
            and (:search is null
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(ac.title) like lower(concat('%', :search, '%'))
                or lower(lc.title) like lower(concat('%', :search, '%')))
            order by a.updatedAt desc, a.id desc
            """,
            countQuery = """
            select count(a)
            from CodingAssignment a
            left join a.course ac
            left join a.lesson l
            left join l.course lc
            where (ac.instructor.id = :teacherId or lc.instructor.id = :teacherId)
            and (:courseId is null or ac.id = :courseId or lc.id = :courseId)
            and (:status is null or lower(a.status) = lower(:status))
            and (:search is null
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(ac.title) like lower(concat('%', :search, '%'))
                or lower(lc.title) like lower(concat('%', :search, '%')))
            """)
    Page<CodingAssignment> findTeacherAssignments(@Param("teacherId") Integer teacherId,
                                                  @Param("courseId") Integer courseId,
                                                  @Param("status") String status,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    @Query("""
            select distinct a
            from CodingAssignment a
            left join fetch a.course ac
            left join fetch a.lesson l
            left join fetch l.course lc
            left join fetch a.assignees assignee
            left join fetch assignee.student
            join CourseEnrollment e on e.course.id = coalesce(ac.id, lc.id)
            where e.student.id = :studentId
              and (:courseId is null or ac.id = :courseId or lc.id = :courseId)
              and (a.status is null or upper(a.status) = 'PUBLISHED')
              and (assignee.id is not null or not exists (
                  select aa.id from AssignmentAssignee aa where aa.assignment = a
              ))
            order by a.dueDate asc, a.id asc
            """)
    List<CodingAssignment> findPublishedAssignmentsForStudent(@Param("studentId") Integer studentId,
                                                              @Param("courseId") Integer courseId);

    @Query("""
            select a.id as assignmentId,
                   a.title as title,
                   coalesce(ac.id, lc.id) as courseId,
                   coalesce(ac.title, lc.title) as courseTitle,
                   a.dueDate as dueAt
            from CodingAssignment a
            left join a.course ac
            left join a.lesson l
            left join l.course lc
            join CourseEnrollment e on e.course.id = coalesce(ac.id, lc.id)
            left join a.assignees assignee on assignee.student.id = :studentId
            where e.student.id = :studentId
              and a.dueDate is not null
              and a.id not in :completedAssignmentIds
              and (a.status is null or upper(a.status) = 'PUBLISHED')
              and (assignee.id is not null or not exists (
                  select aa.id from AssignmentAssignee aa where aa.assignment = a
              ))
            order by a.dueDate asc, a.id asc
            """)
    List<StudentDeadlineProjection> findUpcomingDeadlinesForStudentExcludingCompleted(
            @Param("studentId") Integer studentId,
            @Param("completedAssignmentIds") Collection<Integer> completedAssignmentIds,
            Pageable pageable);
}

