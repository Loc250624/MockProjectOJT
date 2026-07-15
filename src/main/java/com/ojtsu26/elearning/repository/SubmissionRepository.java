package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.Submission;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.projection.TeacherRecentSubmissionProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Integer> {
    interface AssignmentSubmissionStats {
        Integer getAssignmentId();
        Long getSubmissionCount();
        Long getPendingCount();
    }

    @Query("""
            select count(s)
            from Submission s
            where s.lesson.course.instructor.id = :teacherId
            and s.status = :status
            """)
    long countByTeacherIdAndStatus(@Param("teacherId") Integer teacherId, @Param("status") SubmissionStatus status);

    long countByStudentId(Integer studentId);

    @Query("select coalesce(avg(s.score), 0) from Submission s where s.student.id = :studentId and s.score is not null")
    BigDecimal averageScoreByStudentId(Integer studentId);

    @Query("select count(s) from Submission s where s.lesson.course.instructor.id = :instructorId and s.status = :status")
    long countByInstructorIdAndStatus(Integer instructorId, SubmissionStatus status);

    @Query("select count(distinct s.lesson.id) from Submission s where s.student.id = :studentId and s.lesson.course.id = :courseId and s.status = com.ojtsu26.elearning.model.enums.SubmissionStatus.PASSED and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or s.lesson.quiz is not null or s.lesson.codingassignment is not null)")
    long countPassedRequiredAssessmentLessons(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    @Query("select count(distinct s.lesson.id) from Submission s where s.student.id = :studentId and s.lesson.course.id = :courseId and (s.lesson.type = com.ojtsu26.elearning.model.enums.LessonType.QUIZ or s.lesson.quiz is not null or s.lesson.codingassignment is not null)")
    long countSubmittedAssessmentLessons(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    Optional<Submission> findTopByStudentIdAndLessonIdAndStatusOrderByIdDesc(Integer studentId, Integer lessonId, SubmissionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Submission s
            where s.student.id = :studentId
              and s.lesson.id = :lessonId
              and s.status = :status
            order by s.id desc
            """)
    List<Submission> findDraftsForUpdate(@Param("studentId") Integer studentId,
                                          @Param("lessonId") Integer lessonId,
                                          @Param("status") SubmissionStatus status);

    Optional<Submission> findTopByStudentIdAndLessonIdOrderByIdDesc(Integer studentId, Integer lessonId);

    List<Submission> findByStudentIdAndLessonIdOrderByIdDesc(Integer studentId, Integer lessonId);

    @Query("select s from Submission s where s.id = :submissionId and s.student.id = :studentId and s.lesson.id = :lessonId")
    Optional<Submission> findOwnedLessonSubmission(@Param("submissionId") Integer submissionId,
                                                   @Param("studentId") Integer studentId,
                                                   @Param("lessonId") Integer lessonId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Integer studentId);

    Optional<Submission> findTopByAssignmentIdAndStudentIdOrderByUpdatedAtDesc(Integer assignmentId, Integer studentId);

    @Query("""
            select distinct s.assignment.id
            from Submission s
            where s.student.id = :studentId
              and s.assignment.id in :assignmentIds
              and s.status in :completedStatuses
            """)
    List<Integer> findCompletedAssignmentIdsForStudent(@Param("studentId") Integer studentId,
                                                       @Param("assignmentIds") Collection<Integer> assignmentIds,
                                                       @Param("completedStatuses") Collection<SubmissionStatus> completedStatuses);

    @Query("""
            select distinct s.assignment.id
            from Submission s
            where s.student.id = :studentId
              and s.assignment is not null
              and s.status in :completedStatuses
            """)
    List<Integer> findCompletedAssignmentIdsForStudent(@Param("studentId") Integer studentId,
                                                       @Param("completedStatuses") Collection<SubmissionStatus> completedStatuses);

    @Query("select s from Submission s join fetch s.assignment a join fetch a.lesson l join fetch l.course c left join fetch c.instructor where s.id = :submissionId")
    Optional<Submission> findByIdWithAssignmentCourse(@Param("submissionId") Integer submissionId);

    @Query("""
            select s
            from Submission s
            join fetch s.assignment a
            where s.student.id = :studentId
              and s.assignment.id in :assignmentIds
              and s.id in (
                  select max(s2.id)
                  from Submission s2
                  where s2.student.id = :studentId
                    and s2.assignment.id in :assignmentIds
                  group by s2.assignment.id
              )
            """)
    List<Submission> findLatestByAssignmentIdsAndStudentId(@Param("assignmentIds") Collection<Integer> assignmentIds,
                                                           @Param("studentId") Integer studentId);

    @Query("select s from Submission s join fetch s.student join fetch s.assignment a join fetch a.lesson l where a.id = :assignmentId order by s.submittedAt desc")
    List<Submission> findByAssignmentIdWithStudent(@Param("assignmentId") Integer assignmentId);

    @Query("""
            select s.assignment.id as assignmentId,
                   count(s.id) as submissionCount,
                   coalesce(sum(case when s.status in :pendingStatuses then 1 else 0 end), 0) as pendingCount
            from Submission s
            where s.assignment.id in :assignmentIds
            group by s.assignment.id
            """)
    List<AssignmentSubmissionStats> countSubmissionStatsByAssignmentIds(
            @Param("assignmentIds") List<Integer> assignmentIds,
            @Param("pendingStatuses") List<SubmissionStatus> pendingStatuses);

    @Query(value = """
            select s
            from Submission s
            join fetch s.student st
            join fetch s.assignment a
            join fetch a.lesson l
            join fetch l.course c
            left join fetch c.instructor
            where c.instructor.id = :teacherId
            and (:courseId is null or c.id = :courseId)
            and (:assignmentId is null or a.id = :assignmentId)
            and (:status is null or s.status = :status)
            and (:search is null
                or lower(st.fullName) like lower(concat('%', :search, '%'))
                or lower(st.email) like lower(concat('%', :search, '%'))
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(c.title) like lower(concat('%', :search, '%')))
            order by s.submittedAt desc, s.updatedAt desc, s.id desc
            """,
            countQuery = """
            select count(s)
            from Submission s
            join s.student st
            join s.assignment a
            join a.lesson l
            join l.course c
            where c.instructor.id = :teacherId
            and (:courseId is null or c.id = :courseId)
            and (:assignmentId is null or a.id = :assignmentId)
            and (:status is null or s.status = :status)
            and (:search is null
                or lower(st.fullName) like lower(concat('%', :search, '%'))
                or lower(st.email) like lower(concat('%', :search, '%'))
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    Page<Submission> findTeacherSubmissions(@Param("teacherId") Integer teacherId,
                                            @Param("courseId") Integer courseId,
                                            @Param("assignmentId") Integer assignmentId,
                                            @Param("status") SubmissionStatus status,
                                            @Param("search") String search,
                                            Pageable pageable);

    @Query("""
            select count(s)
            from Submission s
            join s.student st
            join s.assignment a
            join a.lesson l
            join l.course c
            where c.instructor.id = :teacherId
            and (:courseId is null or c.id = :courseId)
            and (:assignmentId is null or a.id = :assignmentId)
            and s.status in :statuses
            and (:search is null
                or lower(st.fullName) like lower(concat('%', :search, '%'))
                or lower(st.email) like lower(concat('%', :search, '%'))
                or lower(a.title) like lower(concat('%', :search, '%'))
                or lower(c.title) like lower(concat('%', :search, '%')))
            """)
    long countTeacherSubmissionsByStatuses(@Param("teacherId") Integer teacherId,
                                           @Param("courseId") Integer courseId,
                                           @Param("assignmentId") Integer assignmentId,
                                           @Param("statuses") List<SubmissionStatus> statuses,
                                           @Param("search") String search);

    @Query("""
            select s.id as submissionId,
                   a.id as assignmentId,
                   a.title as assessmentTitle,
                   st.fullName as studentName,
                   s.submittedAt as submittedAt,
                   s.status as status
            from Submission s
            join s.student st
            join s.assignment a
            join a.lesson l
            join l.course c
            where c.instructor.id = :teacherId
            order by case when s.status in :reviewStatuses then 0 else 1 end,
                     s.submittedAt desc,
                     s.id desc
            """)
    List<TeacherRecentSubmissionProjection> findRecentDashboardSubmissionsByTeacherId(
            @Param("teacherId") Integer teacherId,
            @Param("reviewStatuses") Collection<SubmissionStatus> reviewStatuses,
            Pageable pageable);

}
