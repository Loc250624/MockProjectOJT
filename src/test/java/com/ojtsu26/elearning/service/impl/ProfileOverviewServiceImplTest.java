package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.AdminProfileOverviewDTO;
import com.ojtsu26.elearning.dto.response.TeacherProfileOverviewDTO;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.SubmissionStatus;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileOverviewServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    private ProfileOverviewServiceImpl profileOverviewService;

    @BeforeEach
    void setUp() {
        profileOverviewService = new ProfileOverviewServiceImpl(
                courseRepository,
                courseEnrollmentRepository,
                submissionRepository,
                userRepository
        );
    }

    @Test
    void getTeacherOverviewUsesRepositoryCounts() {
        when(courseRepository.countByInstructorId(10)).thenReturn(4L);
        when(courseRepository.countByInstructorIdAndStatus(10, CourseStatus.APPROVED)).thenReturn(3L);
        when(courseEnrollmentRepository.countDistinctStudentsByTeacherId(10)).thenReturn(25L);
        when(submissionRepository.countByTeacherIdAndStatus(10, SubmissionStatus.PENDING_REVIEW)).thenReturn(7L);

        TeacherProfileOverviewDTO overview = profileOverviewService.getTeacherOverview(10);

        assertThat(overview.getTotalCourses()).isEqualTo(4L);
        assertThat(overview.getApprovedCourses()).isEqualTo(3L);
        assertThat(overview.getTotalStudents()).isEqualTo(25L);
        assertThat(overview.getPendingSubmissions()).isEqualTo(7L);
        verify(courseRepository).countByInstructorId(10);
        verify(courseRepository).countByInstructorIdAndStatus(10, CourseStatus.APPROVED);
        verify(courseEnrollmentRepository).countDistinctStudentsByTeacherId(10);
        verify(submissionRepository).countByTeacherIdAndStatus(10, SubmissionStatus.PENDING_REVIEW);
    }

    @Test
    void getAdminOverviewUsesRepositoryCounts() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(80L);
        when(userRepository.countByStatus(UserStatus.BLOCKED)).thenReturn(20L);
        when(courseRepository.countByStatus(CourseStatus.PENDING_APPROVAL)).thenReturn(5L);

        AdminProfileOverviewDTO overview = profileOverviewService.getAdminOverview();

        assertThat(overview.getTotalUsers()).isEqualTo(100L);
        assertThat(overview.getActiveUsers()).isEqualTo(80L);
        assertThat(overview.getBlockedUsers()).isEqualTo(20L);
        assertThat(overview.getPendingCourseApprovals()).isEqualTo(5L);
        verify(userRepository).count();
        verify(userRepository).countByStatus(UserStatus.ACTIVE);
        verify(userRepository).countByStatus(UserStatus.BLOCKED);
        verify(courseRepository).countByStatus(CourseStatus.PENDING_APPROVAL);
    }
}
