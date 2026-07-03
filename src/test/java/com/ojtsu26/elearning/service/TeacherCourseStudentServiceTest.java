package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.response.TeacherCourseStudentPageDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.projection.EnrollmentProgressSummaryProjection;
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
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), eq("Alex"), eq("ALL"), eq("IN_PROGRESS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(enrollment), PageRequest.of(0, 10), 1));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(4L);
        when(lessonProgressRepository.summarizeProgressForEnrollments(101, List.of(501)))
                .thenReturn(List.of(summary(501, 2L, LocalDateTime.of(2026, 6, 3, 14, 30))));

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, "  Alex  ", "all", "in_progress", "name", "asc", 0, 10);

        assertEquals("Spring Security", response.getCourseTitle());
        assertEquals(1, response.getTotalElements());
        assertEquals("Alex Rivera", response.getStudents().get(0).getStudentName());
        assertEquals("alex@example.com", response.getStudents().get(0).getStudentEmail());
        assertEquals("IN_PROGRESS", response.getStudents().get(0).getProgressState());
        assertEquals(2, response.getStudents().get(0).getCompletedLessons());
        assertEquals(4, response.getStudents().get(0).getTotalLessons());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findTeacherCourseStudents(eq(101), eq("Alex"), eq("ALL"), eq("IN_PROGRESS"), pageableCaptor.capture());
        assertEquals("student.fullName: ASC,id: ASC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void anotherTeacherIsRejectedBeforeEnrollmentQuery() {
        when(currentUserService.getCurrentUser()).thenReturn(otherTeacher);
        when(courseRepository.findById(101)).thenReturn(Optional.of(course));

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null));

        verify(enrollmentRepository, never()).findTeacherCourseStudents(any(), any(), any(), any(), any());
    }

    @Test
    void studentRoleIsRejected() {
        when(currentUserService.getCurrentUser()).thenReturn(student);

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null));

        verify(courseRepository, never()).findById(any());
    }

    @Test
    void unsupportedFiltersAndSortSafelyDefaultAndPageSizeIsCapped() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, "   ", "SUSPENDED", "PRIVATE", "passwordHash", "sideways", -4, 500);

        assertEquals("ALL", response.getEnrollmentStatus());
        assertEquals("ALL", response.getProgressState());
        assertEquals("enrolledAt", response.getSort());
        assertEquals("desc", response.getDirection());
        assertEquals(50, response.getSize());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(enrollmentRepository).findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
        assertEquals("enrolledAt: DESC,id: ASC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void tooLongSearchIsRejectedBeforeQuery() {
        stubOwnedCourse();
        String longSearch = "a".repeat(101);

        assertThrows(BusinessException.class, () -> service.findStudentsForCurrentTeacherCourse(
                101, longSearch, null, null, null, null, null, null));

        verify(enrollmentRepository, never()).findTeacherCourseStudents(any(), any(), any(), any(), any());
    }

    @Test
    void emptyCourseReturnsValidEmptyPageAndDoesNotQueryProgressSummaries() {
        stubOwnedCourse();
        when(enrollmentRepository.findTeacherCourseStudents(eq(101), isNull(), eq("ALL"), eq("ALL"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(lessonRepository.countRequiredContentLessons(101)).thenReturn(0L);

        TeacherCourseStudentPageDTO response = service.findStudentsForCurrentTeacherCourse(
                101, null, null, null, null, null, null, null);

        assertTrue(response.getStudents().isEmpty());
        assertEquals(0, response.getTotalElements());
        verify(lessonProgressRepository, never()).summarizeProgressForEnrollments(any(), any());
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
}
