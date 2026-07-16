package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherDashboardDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.OrderStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.OrderItemRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.projection.TeacherCourseMetricProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRecentSubmissionProjection;
import com.ojtsu26.elearning.repository.projection.TeacherRevenueCourseProjection;
import com.ojtsu26.elearning.service.impl.TeacherDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherDashboardServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    private TeacherDashboardServiceImpl service;
    private User teacher;

    @BeforeEach
    void setUp() {
        service = new TeacherDashboardServiceImpl(
                currentUserService,
                courseRepository,
                enrollmentRepository,
                orderItemRepository,
                submissionRepository,
                lessonProgressRepository
        );
        teacher = User.builder().id(7).role(Role.TEACHER).fullName("Teacher").build();
    }

    @Test
    void buildsTeacherScopedDashboardFromGroupedQueries() {
        when(currentUserService.getCurrentUser()).thenReturn(teacher);
        Course draftCourse = Course.builder()
                .id(11)
                .title("Draft Course")
                .status(CourseStatus.DRAFT)
                .updatedAt(LocalDateTime.now())
                .build();
        Course approvedCourse = Course.builder()
                .id(12)
                .title("Approved Course")
                .status(CourseStatus.APPROVED)
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(courseRepository.findByInstructorId(7)).thenReturn(List.of(draftCourse, approvedCourse));
        when(enrollmentRepository.findDashboardCourseMetricsByTeacherId(7))
                .thenReturn(List.of(metric(11, 4L, "37.6"), metric(12, 2L, "101.9")));
        when(orderItemRepository.findTeacherRevenueByCourse(eq(7), eq(OrderStatus.PAID), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(List.of(revenue(11, "49.995"), revenue(12, "10.00")));
        when(submissionRepository.findRecentDashboardSubmissionsByTeacherId(eq(7), any(), any(Pageable.class)))
                .thenReturn(List.of(submission(50, 60, "Homework", "Student One", SubmissionStatus.SUBMITTED)));
        when(orderItemRepository.sumTeacherRevenue(eq(7), eq(OrderStatus.PAID), any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
                .thenReturn(new BigDecimal("59.995"));
        when(enrollmentRepository.averageProgressByTeacherId(7)).thenReturn(new BigDecimal("88.4"));
        when(enrollmentRepository.countDistinctStudentsByTeacherId(7)).thenReturn(5L);

        TeacherDashboardDTO result = service.getCurrentTeacherDashboard();

        assertEquals(5, result.getTotalActiveStudents());
        assertEquals(88, result.getAverageCompletionPercent());
        assertEquals("$60.00", result.getRevenueMonthToDateDisplay());
        assertEquals(2, result.getActiveCourses().size());
        assertEquals("Resume", result.getActiveCourses().get(0).getActionLabel());
        assertEquals("/teacher/courses/edit/11", result.getActiveCourses().get(0).getActionUrl());
        assertEquals(100, result.getActiveCourses().get(1).getAverageCompletionPercent());
        assertEquals("Needs Review", result.getRecentSubmissions().get(0).getReviewStatus());
        assertEquals("/teacher/assignments/60/submissions", result.getGradeAllUrl());
        verify(orderItemRepository).sumTeacherRevenue(eq(7), eq(OrderStatus.PAID), any(LocalDateTime.class), any(LocalDateTime.class), isNull());
    }

    @Test
    void nonTeacherCannotReadTeacherDashboardData() {
        User student = User.builder().id(8).role(Role.STUDENT).build();
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.getCurrentTeacherDashboard());

        verifyNoInteractions(courseRepository, enrollmentRepository, orderItemRepository, submissionRepository);
    }

    private TeacherCourseMetricProjection metric(Integer courseId, Long enrollmentCount, String averageProgress) {
        return new TeacherCourseMetricProjection() {
            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public Long getEnrollmentCount() {
                return enrollmentCount;
            }

            @Override
            public BigDecimal getAverageProgress() {
                return new BigDecimal(averageProgress);
            }
        };
    }

    private TeacherRevenueCourseProjection revenue(Integer courseId, String amount) {
        return new TeacherRevenueCourseProjection() {
            @Override
            public Integer getCourseId() {
                return courseId;
            }

            @Override
            public String getCourseTitle() {
                return "Course";
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(amount);
            }

            @Override
            public Long getPaidOrderCount() {
                return 1L;
            }

            @Override
            public Long getUnitsSold() {
                return 1L;
            }
        };
    }

    private TeacherRecentSubmissionProjection submission(Integer submissionId,
                                                         Integer assignmentId,
                                                         String title,
                                                         String studentName,
                                                         SubmissionStatus status) {
        return new TeacherRecentSubmissionProjection() {
            @Override
            public Integer getSubmissionId() {
                return submissionId;
            }

            @Override
            public Integer getAssignmentId() {
                return assignmentId;
            }

            @Override
            public String getAssessmentTitle() {
                return title;
            }

            @Override
            public String getStudentName() {
                return studentName;
            }

            @Override
            public LocalDateTime getSubmittedAt() {
                return LocalDateTime.now().minusHours(2);
            }

            @Override
            public SubmissionStatus getStatus() {
                return status;
            }
        };
    }
}
