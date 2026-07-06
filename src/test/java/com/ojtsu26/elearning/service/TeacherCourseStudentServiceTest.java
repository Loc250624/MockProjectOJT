package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherCourseStudentPageDTO;
import com.ojtsu26.elearning.dto.response.TeacherProgressOverviewDTO;
import com.ojtsu26.elearning.dto.response.TeacherStudentProgressDetailDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.projection.EnrollmentProgressSummaryProjection;
import com.ojtsu26.elearning.repository.projection.LessonProgressDetailProjection;
import com.ojtsu26.elearning.service.impl.TeacherCourseStudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherCourseStudentServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CurrentUserService currentUserService;

    private TeacherCourseStudentServiceImpl service;
    private User teacher;
    private User otherTeacher;
    private User student;
    private Course course;
    private CourseEnrollment enrollment;

    @BeforeEach
    void setUp() {
        service = new TeacherCourseStudentServiceImpl(
                courseRepository,
                enrollmentRepository,
                lessonRepository,
                lessonProgressRepository,
                submissionRepository,
                currentUserService
        );
        teacher = User.builder().id(7).role(Role.TEACHER).fullName("Teacher").build();
        otherTeacher = User.builder().id(8).role(Role.TEACHER).fullName("Other Teacher").build();
        student = User.builder()
                .id(21)
                .role(Role.STUDENT)
                .fullName("Alex Rivera")
                .email("alex@example.com")
                .avatarUrl("/images/alex.png")
                .passwordHash("secret")
                .build();
        course = Course.builder().id(101).title("Spring Security").instructor(teacher).build();
        enrollment = CourseEnrollment.builder()
                .id(501)
                .student(student)
                .course(course)
                .progressPercentage(new BigDecimal("50.00"))
                .isCompleted(false)
                .enrolledAt(LocalDateTime.of(2026, 6, 1, 9, 0))
                .build();
    }

    @Test
    void courseOwnerTeacherCanViewPagedStudentsWithProgressSummary() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), eq("Alex"), eq("ALL"), eq("IN_PROGRESS"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(enrollment), PageRequest.of(0, 10), 1));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(4L);
        when(lessonProgressRepository.summarizeProgressForEnrollments(101, List.of(501)))
                .thenReturn(List.of(summary(501, 2L, LocalDateTime.of(2026, 6, 3, 14, 30))));

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, "  Alex  ", "all", "in_progress", null, null, "name", "asc", 0, 10);

        assertEquals("Spring Security", response.getCourseTitle());
        assertEquals(1, response.getTotalElements());
        assertEquals("Alex Rivera", response.getStudents().get(0).getStudentName());
        assertEquals("alex@example.com", response.getStudents().get(0).getStudentEmail());
        assertEquals("IN_PROGRESS", response.getStudents().get(0).getProgressState());
        assertEquals(2, response.getStudents().get(0).getCompletedLessons());
        assertEquals(4, response.getStudents().get(0).getTotalLessons());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findTeacherCourseStudents(eq(101), eq("Alex"), eq("ALL"), eq("IN_PROGRESS"), isNull(), isNull(), pageableCaptor.capture());
        assertEquals("student.fullName: ASC,id: ASC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void anotherTeacherIsRejectedBeforeEnrollmentQuery() {
        when(currentUserService.getCurrentUser()).thenReturn(otherTeacher);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null, null, null));

        verify(enrollmentRepository, never()).findTeacherCourseStudents(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void studentRoleIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null, null, null));

        verify(courseRepository, never()).findById(any());
    }

    @Test
    void unsupportedFiltersAndSortSafelyDefaultAndPageSizeIsCapped() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, "   ", "SUSPENDED", "PRIVATE", null, null, "passwordHash", "sideways", -4, 500);

        assertEquals("ALL", response.getEnrollmentStatus());
        assertEquals("ALL", response.getProgressState());
        assertEquals("enrolledAt", response.getSort());
        assertEquals("desc", response.getDirection());
        assertEquals(50, response.getSize());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), isNull(), isNull(), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
        assertEquals("enrolledAt: DESC,id: ASC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void activityDateRangeIsConvertedToInclusiveDateBoundaries() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(
                eq(101),
                isNull(),
                eq("ALL"),
                eq("ALL"),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 4, 0, 0)),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                null,
                null,
                null,
                null);

        assertEquals("2026-06-01", response.getLastActivityFrom());
        assertEquals("2026-06-03", response.getLastActivityTo());
    }

    @Test
    void invalidActivityDateRangeIsRejectedBeforeQuery() {
        stubOwnedCourse();

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 4),
                LocalDate.of(2026, 6, 3),
                null,
                null,
                null,
                null));

        verify(enrollmentRepository, never()).findTeacherCourseStudents(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void tooLongSearchIsRejectedBeforeQuery() {
        stubOwnedCourse();
        String longSearch = "a".repeat(101);

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, longSearch, null, null, null, null, null, null, null, null));

        verify(enrollmentRepository, never()).findTeacherCourseStudents(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void emptyCourseReturnsValidEmptyPageAndDoesNotQueryProgressSummaries() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null, null, null);

        assertTrue(response.getStudents().isEmpty());
        assertEquals(0, response.getTotalElements());
        verify(lessonProgressRepository, never()).summarizeProgressForEnrollments(any(), any());
    }

    @Test
    void courseOwnerTeacherCanViewProgressOverview() {
        stubOwnedCourse();
        when(enrollmentRepository.countByCourseId(101)).thenReturn(4L);
        when(enrollmentRepository.countByCourseIdAndIsCompletedTrue(101)).thenReturn(1L);
        when(enrollmentRepository.countActiveByCourseId(101)).thenReturn(3L);
        when(enrollmentRepository.countNotStartedByCourseId(101)).thenReturn(1L);
        when(enrollmentRepository.countInProgressByCourseId(101)).thenReturn(2L);
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(5L);
        when(lessonRepository.countRequiredAssessmentLessons(101)).thenReturn(1L);
        when(courseRepository.averageProgressByCourseId(101)).thenReturn(new BigDecimal("37.505"));
        when(lessonProgressRepository.findLatestActivityAtByCourseId(101))
                .thenReturn(Optional.of(LocalDateTime.of(2026, 6, 4, 10, 15)));

        TeacherProgressOverviewDTO overview = service.getProgressOverviewForCurrentTeacherCourse(101);

        assertEquals(4, overview.getTotalStudents());
        assertEquals(1, overview.getNotStartedStudents());
        assertEquals(2, overview.getInProgressStudents());
        assertEquals(1, overview.getCompletedStudents());
        assertEquals(new BigDecimal("37.51"), overview.getAverageProgress());
        assertEquals(new BigDecimal("25.00"), overview.getCompletionRate());
        assertTrue(overview.isAssessmentSummaryAvailable());
    }

    @Test
    void courseWithoutStudentsReturnsZeroOverviewMetricsSafely() {
        stubOwnedCourse();
        when(enrollmentRepository.countByCourseId(101)).thenReturn(0L);
        when(enrollmentRepository.countByCourseIdAndIsCompletedTrue(101)).thenReturn(0L);
        when(enrollmentRepository.countActiveByCourseId(101)).thenReturn(0L);
        when(enrollmentRepository.countNotStartedByCourseId(101)).thenReturn(0L);
        when(enrollmentRepository.countInProgressByCourseId(101)).thenReturn(0L);
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);
        when(lessonRepository.countRequiredAssessmentLessons(101)).thenReturn(0L);
        when(courseRepository.averageProgressByCourseId(101)).thenReturn(BigDecimal.ZERO);
        when(lessonProgressRepository.findLatestActivityAtByCourseId(101)).thenReturn(Optional.empty());

        TeacherProgressOverviewDTO overview = service.getProgressOverviewForCurrentTeacherCourse(101);

        assertEquals(0, overview.getTotalStudents());
        assertEquals(BigDecimal.ZERO, overview.getCompletionRate());
        assertEquals(BigDecimal.ZERO.setScale(2), overview.getAverageProgress());
        assertFalse(overview.isAssessmentSummaryAvailable());
    }

    @Test
    void studentProgressDetailIsScopedToSelectedCourse() {
        stubOwnedCourse();
        when(enrollmentRepository.findCourseStudentEnrollmentForReport(101, 21)).thenReturn(Optional.of(enrollment));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(4L);
        when(lessonProgressRepository.summarizeProgressForEnrollments(101, List.of(501)))
                .thenReturn(List.of(summary(501, 2L, LocalDateTime.of(2026, 6, 3, 14, 30))));
        when(lessonRepository.countRequiredAssessmentLessons(101)).thenReturn(1L);
        when(submissionRepository.countPassedRequiredAssessmentLessons(21, 101)).thenReturn(1L);
        when(lessonProgressRepository.findCourseLessonProgressDetail(101, 501))
                .thenReturn(List.of(lessonDetail(11, "Introduction", true)));

        TeacherStudentProgressDetailDTO detail = service.getStudentProgressDetailForCurrentTeacherCourse(101, 21);

        assertEquals(21, detail.getStudentId());
        assertEquals(101, detail.getCourseId());
        assertEquals("IN_PROGRESS", detail.getProgressState());
        assertEquals(1, detail.getPassedAssessments());
        assertEquals(1, detail.getLessons().size());
        assertEquals("Introduction", detail.getLessons().get(0).getTitle());
    }

    @Test
    void changingStudentIdDoesNotRevealAnotherCourseProgress() {
        stubOwnedCourse();
        when(enrollmentRepository.findCourseStudentEnrollmentForReport(101, 99)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getStudentProgressDetailForCurrentTeacherCourse(101, 99));

        verify(lessonProgressRepository, never()).findCourseLessonProgressDetail(any(), any());
    }

    private void stubOwnedCourse() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));
    }

    private EnrollmentProgressSummaryProjection summary(Integer enrollmentId, Long completedLessons, LocalDateTime lastActivityAt) {
        return new EnrollmentProgressSummaryProjection() {
            @Override
            public Integer getEnrollmentId() {
                return enrollmentId;
            }

            @Override
            public Long getCompletedLessons() {
                return completedLessons;
            }

            @Override
            public LocalDateTime getLastActivityAt() {
                return lastActivityAt;
            }
        };
    }

    private LessonProgressDetailProjection lessonDetail(Integer lessonId, String title, Boolean completed) {
        return new LessonProgressDetailProjection() {
            @Override
            public Integer getLessonId() {
                return lessonId;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public com.ojtsu26.elearning.model.enums.LessonType getType() {
                return null;
            }

            @Override
            public Integer getOrderIndex() {
                return 1;
            }

            @Override
            public Boolean getCompleted() {
                return completed;
            }

            @Override
            public LocalDateTime getCompletedAt() {
                return completed ? LocalDateTime.of(2026, 6, 2, 10, 0) : null;
            }

            @Override
            public LocalDateTime getLastAccessedAt() {
                return LocalDateTime.of(2026, 6, 2, 9, 30);
            }

            @Override
            public Integer getWatchedSeconds() {
                return 120;
            }
        };
    }
}
