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
import com.ojtsu26.elearning.service.ProfileOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileOverviewServiceImpl implements ProfileOverviewService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    @Override
    public TeacherProfileOverviewDTO getTeacherOverview(Integer teacherId) {
        return TeacherProfileOverviewDTO.builder()
                .totalCourses(courseRepository.countByInstructorId(teacherId))
                .approvedCourses(courseRepository.countByInstructorIdAndStatus(teacherId, CourseStatus.APPROVED))
                .totalStudents(courseEnrollmentRepository.countDistinctStudentsByTeacherId(teacherId))
                .pendingSubmissions(submissionRepository.countByTeacherIdAndStatus(teacherId, SubmissionStatus.PENDING_REVIEW))
                .build();
    }

    @Override
    public AdminProfileOverviewDTO getAdminOverview() {
        return AdminProfileOverviewDTO.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .blockedUsers(userRepository.countByStatus(UserStatus.BLOCKED))
                .pendingCourseApprovals(courseRepository.countByStatus(CourseStatus.PENDING_APPROVAL))
                .build();
    }
}
