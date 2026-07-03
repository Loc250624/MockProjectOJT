package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.TeacherCourseStudentDTO;
import com.ojtsu26.elearning.dto.response.TeacherCourseStudentPageDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.repository.projection.EnrollmentProgressSummaryProjection;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.TeacherCourseStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherCourseStudentServiceImpl implements TeacherCourseStudentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 100;
    private static final Set<String> ENROLLMENT_STATUSES = Set.of("ALL", "ACTIVE", "COMPLETED");
    private static final Set<String> PROGRESS_STATES = Set.of("ALL", "NOT_STARTED", "IN_PROGRESS", "COMPLETED");
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "student.fullName",
            "email", "student.email",
            "enrolledAt", "enrolledAt",
            "progress", "progressPercentage",
            "status", "isCompleted"
    );

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public TeacherCourseStudentPageDTO findStudentsForCurrentTeacherCourse(Integer courseId,
                                                                          String search,
                                                                          String enrollmentStatus,
                                                                          String progressState,
                                                                          String sort,
                                                                          String direction,
                                                                          Integer page,
                                                                          Integer size) {
        User teacher = currentUserService.getCurrentUser();
        requireTeacher(teacher);
        Course course = requireOwnedCourse(courseId, teacher.getId());

        String cleanSearch = normalizeSearch(search);
        String cleanEnrollmentStatus = normalizeFilter(enrollmentStatus, ENROLLMENT_STATUSES, "ALL");
        String cleanProgressState = normalizeFilter(progressState, PROGRESS_STATES, "ALL");
        String cleanSort = sort != null && SORT_FIELDS.containsKey(sort) ? sort : "enrolledAt";
        Sort.Direction cleanDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int cleanPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int cleanSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Sort pageSort = Sort.by(cleanDirection, SORT_FIELDS.get(cleanSort)).and(Sort.by(Sort.Direction.ASC, "id"));
        Page<CourseEnrollment> enrollmentPage = enrollmentRepository.findTeacherCourseStudents(
                courseId,
                cleanSearch,
                cleanEnrollmentStatus,
                cleanProgressState,
                PageRequest.of(cleanPage, cleanSize, pageSort)
        );

        long totalRequiredLessons = lessonRepository.countRequiredContentLessons(courseId);
        Map<Integer, EnrollmentProgressSummaryProjection> summaries = loadProgressSummaries(courseId, enrollmentPage.getContent());

        List<TeacherCourseStudentDTO> students = enrollmentPage.getContent().stream()
                .map(enrollment -> toDto(enrollment, summaries.get(enrollment.getId()), totalRequiredLessons))
                .toList();

        return TeacherCourseStudentPageDTO.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .totalElements(enrollmentPage.getTotalElements())
                .totalPages(enrollmentPage.getTotalPages())
                .page(enrollmentPage.getNumber())
                .size(enrollmentPage.getSize())
                .search(cleanSearch == null ? "" : cleanSearch)
                .enrollmentStatus(cleanEnrollmentStatus)
                .progressState(cleanProgressState)
                .sort(cleanSort)
                .direction(cleanDirection.name().toLowerCase(Locale.ROOT))
                .emailVisible(true)
                .students(students)
                .build();
    }

    private void requireTeacher(User user) {
        if (user.getRole() != Role.TEACHER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private Course requireOwnedCourse(Integer courseId, Integer teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getInstructor() == null || !Objects.equals(course.getInstructor().getId(), teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "You do not have access to this course.");
        }
        return course;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String clean = search.trim();
        if (clean.length() > MAX_SEARCH_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Search is too long.");
        }
        return clean;
    }

    private String normalizeFilter(String value, Set<String> allowed, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String clean = value.trim().toUpperCase(Locale.ROOT);
        return allowed.contains(clean) ? clean : defaultValue;
    }

    private Map<Integer, EnrollmentProgressSummaryProjection> loadProgressSummaries(Integer courseId, List<CourseEnrollment> enrollments) {
        List<Integer> enrollmentIds = enrollments.stream()
                .map(CourseEnrollment::getId)
                .toList();
        if (enrollmentIds.isEmpty()) {
            return Map.of();
        }
        return lessonProgressRepository.summarizeProgressForEnrollments(courseId, enrollmentIds).stream()
                .collect(Collectors.toMap(EnrollmentProgressSummaryProjection::getEnrollmentId, Function.identity()));
    }

    private TeacherCourseStudentDTO toDto(CourseEnrollment enrollment,
                                          EnrollmentProgressSummaryProjection summary,
                                          long totalRequiredLessons) {
        User student = enrollment.getStudent();
        int completedLessons = summary == null || summary.getCompletedLessons() == null
                ? 0
                : summary.getCompletedLessons().intValue();
        BigDecimal progressPercentage = enrollment.getProgressPercentage() == null
                ? BigDecimal.ZERO
                : enrollment.getProgressPercentage();
        boolean completed = Boolean.TRUE.equals(enrollment.getIsCompleted());

        return TeacherCourseStudentDTO.builder()
                .enrollmentId(enrollment.getId())
                .studentId(student == null ? null : student.getId())
                .studentName(displayName(student))
                .studentEmail(student == null ? null : student.getEmail())
                .avatarUrl(student == null ? null : student.getAvatarUrl())
                .enrollmentStatus(completed ? "COMPLETED" : "ACTIVE")
                .enrolledAt(enrollment.getEnrolledAt())
                .progressPercentage(progressPercentage)
                .completedLessons(completedLessons)
                .totalLessons(Math.toIntExact(totalRequiredLessons))
                .progressState(progressState(progressPercentage, completed))
                .lastActivityAt(summary == null ? null : summary.getLastActivityAt())
                .courseCompleted(completed)
                .build();
    }

    private String displayName(User student) {
        if (student == null || student.getFullName() == null || student.getFullName().isBlank()) {
            return student == null || student.getId() == null ? "Unknown student" : "Student #" + student.getId();
        }
        return student.getFullName();
    }

    private String progressState(BigDecimal progressPercentage, boolean completed) {
        if (completed) {
            return "COMPLETED";
        }
        if (progressPercentage == null || progressPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return "NOT_STARTED";
        }
        return "IN_PROGRESS";
    }
}
