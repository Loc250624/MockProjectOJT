package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.repository.projection.TeacherEnrollmentCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Integer> {
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
    Optional<CourseEnrollment> findByStudentIdAndCourseId(Integer studentId, Integer courseId);
    List<CourseEnrollment> findByStudentId(Integer studentId);
    long countByStudentId(Integer studentId);
    long countByStudentIdAndIsCompletedTrue(Integer studentId);
    long countByCourseId(Integer courseId);
    long countByCourseIdAndIsCompletedTrue(Integer courseId);
    List<CourseEnrollment> findTop5ByStudentIdOrderByEnrolledAtDesc(Integer studentId);

    @Query("select e from CourseEnrollment e where e.course.instructor.id = :instructorId order by e.enrolledAt desc")
    List<CourseEnrollment> findByInstructorIdOrderByEnrolledAtDesc(Integer instructorId);

    @Query("""
            select count(distinct ce.student.id)
            from CourseEnrollment ce
            where ce.course.instructor.id = :teacherId
            """)
    long countDistinctStudentsByTeacherId(@Param("teacherId") Integer teacherId);

    @Query(value = """
            select e from CourseEnrollment e
            join fetch e.student s
            where e.course.id = :courseId
              and (:search is null
                   or lower(s.fullName) like lower(concat('%', :search, '%'))
                   or lower(s.email) like lower(concat('%', :search, '%')))
              and (:enrollmentStatus = 'ALL'
                   or (:enrollmentStatus = 'COMPLETED' and e.isCompleted = true)
                   or (:enrollmentStatus = 'ACTIVE' and (e.isCompleted = false or e.isCompleted is null)))
              and (:progressState = 'ALL'
                   or (:progressState = 'COMPLETED' and e.isCompleted = true)
                   or (:progressState = 'NOT_STARTED' and (e.progressPercentage is null or e.progressPercentage = 0) and (e.isCompleted = false or e.isCompleted is null))
                   or (:progressState = 'IN_PROGRESS' and e.progressPercentage > 0 and (e.isCompleted = false or e.isCompleted is null)))
              and (:lastActivityFrom is null
                   or (select max(p.lastAccessedAt) from LessonProgress p where p.enrollment.id = e.id and p.lesson.course.id = :courseId) >= :lastActivityFrom)
              and (:lastActivityTo is null
                   or (select max(p.lastAccessedAt) from LessonProgress p where p.enrollment.id = e.id and p.lesson.course.id = :courseId) < :lastActivityTo)
            """,
            countQuery = """
            select count(e) from CourseEnrollment e
            join e.student s
            where e.course.id = :courseId
              and (:search is null
                   or lower(s.fullName) like lower(concat('%', :search, '%'))
                   or lower(s.email) like lower(concat('%', :search, '%')))
              and (:enrollmentStatus = 'ALL'
                   or (:enrollmentStatus = 'COMPLETED' and e.isCompleted = true)
                   or (:enrollmentStatus = 'ACTIVE' and (e.isCompleted = false or e.isCompleted is null)))
              and (:progressState = 'ALL'
                   or (:progressState = 'COMPLETED' and e.isCompleted = true)
                   or (:progressState = 'NOT_STARTED' and (e.progressPercentage is null or e.progressPercentage = 0) and (e.isCompleted = false or e.isCompleted is null))
                   or (:progressState = 'IN_PROGRESS' and e.progressPercentage > 0 and (e.isCompleted = false or e.isCompleted is null)))
              and (:lastActivityFrom is null
                   or (select max(p.lastAccessedAt) from LessonProgress p where p.enrollment.id = e.id and p.lesson.course.id = :courseId) >= :lastActivityFrom)
              and (:lastActivityTo is null
                   or (select max(p.lastAccessedAt) from LessonProgress p where p.enrollment.id = e.id and p.lesson.course.id = :courseId) < :lastActivityTo)
            """)
    Page<CourseEnrollment> findTeacherCourseStudents(@Param("courseId") Integer courseId,
                                                     @Param("search") String search,
                                                     @Param("enrollmentStatus") String enrollmentStatus,
                                                     @Param("progressState") String progressState,
                                                     @Param("lastActivityFrom") LocalDateTime lastActivityFrom,
                                                     @Param("lastActivityTo") LocalDateTime lastActivityTo,
                                                     Pageable pageable);

    @Query("select count(e) from CourseEnrollment e where e.course.id = :courseId and (e.isCompleted = false or e.isCompleted is null)")
    long countActiveByCourseId(@Param("courseId") Integer courseId);

    @Query("select count(e) from CourseEnrollment e where e.course.id = :courseId and (e.progressPercentage is null or e.progressPercentage = 0) and (e.isCompleted = false or e.isCompleted is null)")
    long countNotStartedByCourseId(@Param("courseId") Integer courseId);

    @Query("select count(e) from CourseEnrollment e where e.course.id = :courseId and e.progressPercentage > 0 and (e.isCompleted = false or e.isCompleted is null)")
    long countInProgressByCourseId(@Param("courseId") Integer courseId);

    @Query("select e from CourseEnrollment e join fetch e.student s join fetch e.course c left join fetch c.instructor where e.course.id = :courseId and e.student.id = :studentId")
    Optional<CourseEnrollment> findCourseStudentEnrollmentForReport(@Param("courseId") Integer courseId,
                                                                    @Param("studentId") Integer studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CourseEnrollment e where e.student.id = :studentId and e.course.id = :courseId")
    Optional<CourseEnrollment> findByStudentIdAndCourseIdForUpdate(Integer studentId, Integer courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CourseEnrollment e join fetch e.student join fetch e.course c left join fetch c.instructor where e.id = :enrollmentId")
    Optional<CourseEnrollment> findByIdForCertificateIssue(Integer enrollmentId);

    @Query("""
            select count(e)
            from CourseEnrollment e
            where e.course.instructor.id = :teacherId
              and e.enrolledAt >= :from
              and e.enrolledAt < :to
              and (:courseId is null or e.course.id = :courseId)
            """)
    long countTeacherEnrollmentsForAnalytics(@Param("teacherId") Integer teacherId,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to,
                                             @Param("courseId") Integer courseId);

    @Query("""
            select count(distinct e.student.id)
            from CourseEnrollment e
            where e.course.instructor.id = :teacherId
              and e.enrolledAt >= :from
              and e.enrolledAt < :to
              and (:courseId is null or e.course.id = :courseId)
            """)
    long countTeacherStudentsForAnalytics(@Param("teacherId") Integer teacherId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("courseId") Integer courseId);

    @Query("""
            select e.course.id as courseId,
                   count(e) as enrollmentCount,
                   count(distinct e.student.id) as studentCount
            from CourseEnrollment e
            where e.course.instructor.id = :teacherId
              and e.enrolledAt >= :from
              and e.enrolledAt < :to
              and (:courseId is null or e.course.id = :courseId)
            group by e.course.id
            """)
    List<TeacherEnrollmentCountProjection> countTeacherEnrollmentsByCourseForAnalytics(@Param("teacherId") Integer teacherId,
                                                                                       @Param("from") LocalDateTime from,
                                                                                       @Param("to") LocalDateTime to,
                                                                                       @Param("courseId") Integer courseId);
}
