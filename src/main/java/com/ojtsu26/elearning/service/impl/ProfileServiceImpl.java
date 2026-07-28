package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.UpdateProfileRequestDTO;
import com.ojtsu26.elearning.dto.response.ProfileOverviewResponseDTO;
import com.ojtsu26.elearning.dto.response.UserResponseDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.mapper.UserMapper;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.AuthProvider;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CertificateRepository;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.SubmissionRepository;
import com.ojtsu26.elearning.repository.UserRepository;
import com.ojtsu26.elearning.service.AvatarStorageService;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.ExternalImageService;
import com.ojtsu26.elearning.service.ProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AvatarStorageService avatarStorageService;
    private final ExternalImageService externalImageService;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CertificateRepository certificateRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public UserResponseDTO getCurrentProfile() {
        return userMapper.toDto(currentUserService.getCurrentUser());
    }

    @Override
    public ProfileOverviewResponseDTO getCurrentProfileOverview() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() == Role.TEACHER) {
            return buildTeacherOverview(user);
        }
        if (user.getRole() == Role.ADMIN) {
            return buildAdminOverview(user);
        }
        return buildStudentOverview(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateCurrentProfile(UpdateProfileRequestDTO request) {
        User user = currentUserService.getCurrentUser();
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (user.getAuthProvider() != AuthProvider.LOCAL && !normalizedEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(ErrorCode.OAUTH2_EMAIL_CANNOT_BE_CHANGED);
        }

        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, user.getId())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO uploadCurrentAvatar(MultipartFile file) {
        User user = currentUserService.getCurrentUser();
        String previousAvatar = user.getAvatarUrl();
        String avatarUrl = avatarStorageService.store(file, user.getId());
        return replaceAvatar(user, previousAvatar, avatarUrl);
    }

    @Override
    @Transactional
    public UserResponseDTO useCurrentAvatarUrl(String imageUrl) {
        User user = currentUserService.getCurrentUser();
        String previousAvatar = user.getAvatarUrl();
        ExternalImageService.DownloadedImage image = externalImageService.download(imageUrl);
        String avatarUrl = avatarStorageService.store(image.content(), image.contentType(), user.getId());
        return replaceAvatar(user, previousAvatar, avatarUrl);
    }

    @Override
    @Transactional
    public UserResponseDTO removeCurrentAvatar() {
        User user = currentUserService.getCurrentUser();
        String previousAvatar = user.getAvatarUrl();
        user.setAvatarUrl(null);
        User saved = userRepository.save(user);
        avatarStorageService.deleteManagedAvatar(previousAvatar);
        return userMapper.toDto(saved);
    }

    private UserResponseDTO replaceAvatar(User user, String previousAvatar, String avatarUrl) {
        user.setAvatarUrl(avatarUrl);
        User saved = userRepository.save(user);
        avatarStorageService.deleteManagedAvatar(previousAvatar);
        return userMapper.toDto(saved);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ProfileOverviewResponseDTO buildStudentOverview(User user) {
        Integer userId = user.getId();
        long enrolled = courseEnrollmentRepository.countByStudentId(userId);
        long completed = courseEnrollmentRepository.countByStudentIdAndIsCompletedTrue(userId);
        long lessonsCompleted = lessonProgressRepository.countByEnrollmentStudentIdAndIsCompletedTrue(userId);
        long certificates = certificateRepository.countByStudentId(userId);
        BigDecimal averageProgress = scale(lessonProgressRepository.averageStudentProgress(userId));
        BigDecimal averageScore = scale(submissionRepository.averageScoreByStudentId(userId));

        List<ProfileOverviewResponseDTO.ActivityItem> activities = new ArrayList<>();
        for (LessonProgress progress : lessonProgressRepository.findTop5ByEnrollmentStudentIdOrderByCompletedAtDesc(userId)) {
            if (progress.getCompletedAt() != null && Boolean.TRUE.equals(progress.getIsCompleted())) {
                activities.add(activity("Lesson", "Completed lesson", safeLessonTitle(progress), progress.getCompletedAt(), "/student/learning"));
            }
        }
        for (CourseEnrollment enrollment : courseEnrollmentRepository.findTop5ByStudentIdOrderByEnrolledAtDesc(userId)) {
            activities.add(activity("Enrollment", "Joined course", safeCourseTitle(enrollment.getCourse()), enrollment.getEnrolledAt(), "/student/my-courses"));
        }
        activities = newest(activities, 5);

        Map<String, List<CourseEnrollment>> byCategory = courseEnrollmentRepository.findTop5ByStudentIdOrderByEnrolledAtDesc(userId).stream()
                .collect(Collectors.groupingBy(enrollment -> safeCategory(enrollment.getCourse()), LinkedHashMap::new, Collectors.toList()));

        List<ProfileOverviewResponseDTO.SkillMetric> skillMetrics = byCategory.entrySet().stream()
                .map(entry -> ProfileOverviewResponseDTO.SkillMetric.builder()
                        .label(entry.getKey())
                        .percentage(scale(avg(entry.getValue().stream()
                                .map(CourseEnrollment::getProgressPercentage)
                                .toList())))
                        .caption(entry.getValue().size() + " enrolled course(s)")
                        .build())
                .toList();

        String advisorMessage = enrolled == 0
                ? "Coming soon. Enroll in a course to unlock learning suggestions from your real activity."
                : "Coming soon. Your current course progress is ready for future AI-powered guidance.";

        return ProfileOverviewResponseDTO.builder()
                .roleKey("student")
                .roleLabel("Student")
                .identifierLabel("Student ID")
                .identifierValue(formatId("STU", userId))
                .metaLabel("Academic Tier")
                .metaValue(certificates > 0 ? "Certificate earner" : "Learning member")
                .advisorMessage(advisorMessage)
                .statCards(List.of(
                        stat("Courses Enrolled", enrolled, "Active learning portfolio", "blue"),
                        stat("Courses Completed", completed, "Completed from your enrollments", "green"),
                        stat("Lessons Completed", lessonsCompleted, "Tracked lesson progress", "cyan"),
                        stat("Certificates Earned", certificates, "Issued credentials", "amber"),
                        stat("Average Progress", averageProgress + "%", "Across enrolled courses", "violet"),
                        stat("Average Score", averageScore + "%", "From graded submissions", "rose")
                ))
                .activities(activities)
                .skillMetrics(skillMetrics)
                .securityItems(securityItems(user))
                .build();
    }

    private ProfileOverviewResponseDTO buildTeacherOverview(User user) {
        Integer userId = user.getId();
        long totalCourses = courseRepository.countByInstructorId(userId);
        long publishedCourses = courseRepository.countByInstructorIdAndStatus(userId, CourseStatus.APPROVED);
        long drafts = courseRepository.countByInstructorIdAndStatus(userId, CourseStatus.DRAFT);
        long activeStudents = courseRepository.countDistinctStudentsByInstructorId(userId);
        long enrollments = courseRepository.countEnrollmentsByInstructorId(userId);
        long certificatesIssued = certificateRepository.countByCourseInstructorId(userId);

        List<Course> courses = courseRepository.findTop5ByInstructorIdOrderByCreatedAtDesc(userId);
        List<ProfileOverviewResponseDTO.TeacherCourseSummary> courseSummaries = courses.stream()
                .map(course -> ProfileOverviewResponseDTO.TeacherCourseSummary.builder()
                        .id(course.getId())
                        .title(course.getTitle())
                        .category(safeCategory(course))
                        .status(course.getStatus() == null ? "Unknown" : course.getStatus().name())
                        .thumbnailUrl(course.getThumbnailUrl())
                        .enrolledStudents(courseEnrollmentRepository.countByCourseId(course.getId()))
                        .averageProgress(scale(courseRepository.averageProgressByCourseId(course.getId())))
                        .createdAt(course.getCreatedAt())
                        .build())
                .toList();

        List<ProfileOverviewResponseDTO.TeacherStudentSummary> students = courseEnrollmentRepository.findByInstructorIdOrderByEnrolledAtDesc(userId).stream()
                .limit(8)
                .map(enrollment -> ProfileOverviewResponseDTO.TeacherStudentSummary.builder()
                        .studentId(enrollment.getStudent() == null ? null : enrollment.getStudent().getId())
                        .fullName(enrollment.getStudent() == null ? "Unknown student" : enrollment.getStudent().getFullName())
                        .email(enrollment.getStudent() == null ? "" : enrollment.getStudent().getEmail())
                        .avatarUrl(enrollment.getStudent() == null ? null : enrollment.getStudent().getAvatarUrl())
                        .courseTitle(safeCourseTitle(enrollment.getCourse()))
                        .progressPercentage(scale(enrollment.getProgressPercentage()))
                        .completed(enrollment.getIsCompleted())
                        .enrolledAt(enrollment.getEnrolledAt())
                        .build())
                .toList();

        List<ProfileOverviewResponseDTO.ActivityItem> activities = courses.stream()
                .map(course -> activity("Course", "Created course", course.getTitle(), course.getCreatedAt(), "/teacher/courses"))
                .toList();

        return ProfileOverviewResponseDTO.builder()
                .roleKey("teacher")
                .roleLabel("Teacher")
                .identifierLabel("Teacher ID")
                .identifierValue(formatId("TCH", userId))
                .metaLabel("Teaching Field")
                .metaValue(courseSummaries.isEmpty() ? "Not configured" : courseSummaries.get(0).getCategory())
                .statCards(List.of(
                        stat("Total Courses Created", totalCourses, "Owned by this instructor", "blue"),
                        stat("Published Courses", publishedCourses, "Approved and visible", "green"),
                        stat("Draft Courses", drafts, "In preparation", "amber"),
                        stat("Active Students", activeStudents, "Distinct enrolled students", "cyan"),
                        stat("Total Enrollments", enrollments, "Across your courses", "violet"),
                        stat("Certificates Issued", certificatesIssued, "From your courses", "green")
                ))
                .activities(newest(activities, 5))
                .teacherCourses(courseSummaries)
                .teacherStudents(students)
                .securityItems(securityItems(user))
                .build();
    }

    private ProfileOverviewResponseDTO buildAdminOverview(User user) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long totalUsers = userRepository.count();
        long activeStudents = userRepository.countByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE);
        long activeTeachers = userRepository.countByRoleAndStatus(Role.TEACHER, UserStatus.ACTIVE);
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.countByStatus(CourseStatus.APPROVED);
        long blockedUsers = userRepository.countByStatus(UserStatus.BLOCKED);
        long pendingCourses = courseRepository.countByStatus(CourseStatus.PENDING_APPROVAL);
        long newUsers = userRepository.countByCreatedAtAfter(monthStart);

        List<ProfileOverviewResponseDTO.ActivityItem> activities = new ArrayList<>();
        userRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(createdUser -> activities.add(activity("User", "New user registered", createdUser.getFullName(), createdUser.getCreatedAt(), "/admin/users")));
        courseRepository.findTop5ByOrderByCreatedAtDesc()
                .forEach(course -> activities.add(activity("Course", "Course created", course.getTitle(), course.getCreatedAt(), "/admin/courses")));

        return ProfileOverviewResponseDTO.builder()
                .roleKey("admin")
                .roleLabel("Administrator")
                .identifierLabel("Admin ID")
                .identifierValue(formatId("ADM", user.getId()))
                .metaLabel("Permission Level")
                .metaValue(user.getRole() == Role.ADMIN ? "Platform administrator" : "Restricted")
                .statCards(List.of(
                        stat("Total Users", totalUsers, "All registered accounts", "blue"),
                        stat("Active Students", activeStudents, "Student accounts", "green"),
                        stat("Active Teachers", activeTeachers, "Teacher accounts", "cyan"),
                        stat("Total Courses", totalCourses, "All course records", "violet"),
                        stat("Published Courses", publishedCourses, "Approved catalog courses", "green"),
                        stat("Blocked Users", blockedUsers, "Requires admin attention", "rose"),
                        stat("Pending Approvals", pendingCourses, "Course moderation queue", "amber"),
                        stat("New Users This Month", newUsers, "Created since month start", "blue")
                ))
                .activities(newest(activities, 6))
                .securityItems(securityItems(user))
                .build();
    }

    private ProfileOverviewResponseDTO.StatCard stat(String label, Object value, String caption, String tone) {
        return ProfileOverviewResponseDTO.StatCard.builder()
                .label(label)
                .value(String.valueOf(value))
                .caption(caption)
                .tone(tone)
                .build();
    }

    private ProfileOverviewResponseDTO.ActivityItem activity(String type, String title, String caption, LocalDateTime occurredAt, String link) {
        return ProfileOverviewResponseDTO.ActivityItem.builder()
                .type(type)
                .title(title == null ? type : title)
                .caption(caption == null ? "" : caption)
                .occurredAt(occurredAt)
                .link(link)
                .build();
    }

    private List<ProfileOverviewResponseDTO.ActivityItem> newest(List<ProfileOverviewResponseDTO.ActivityItem> activities, int limit) {
        return activities.stream()
                .filter(item -> item.getOccurredAt() != null)
                .sorted(Comparator.comparing(ProfileOverviewResponseDTO.ActivityItem::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    private List<ProfileOverviewResponseDTO.SecurityItem> securityItems(User user) {
        return List.of(
                ProfileOverviewResponseDTO.SecurityItem.builder()
                        .label("Authentication Provider")
                        .value(user.getAuthProvider() == null ? "Unknown" : user.getAuthProvider().name())
                        .caption("Email changes are restricted for OAuth2 accounts.")
                        .build(),
                ProfileOverviewResponseDTO.SecurityItem.builder()
                        .label("Account Status")
                        .value(user.getStatus() == null ? "Unknown" : user.getStatus().name())
                        .caption("Managed by protected role-specific controls.")
                        .build(),
                ProfileOverviewResponseDTO.SecurityItem.builder()
                        .label("Last Profile Update")
                        .value(user.getUpdatedAt() == null ? "Not available" : user.getUpdatedAt().toLocalDate().toString())
                        .caption("Stored from your account record.")
                        .build()
        );
    }

    private String formatId(String prefix, Integer id) {
        return prefix + "-" + String.format("%05d", id == null ? 0 : id);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream().filter(value -> value != null).toList();
        if (present.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(present.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String safeCourseTitle(Course course) {
        return course == null || course.getTitle() == null ? "Untitled course" : course.getTitle();
    }

    private String safeLessonTitle(LessonProgress progress) {
        if (progress.getLesson() == null || progress.getLesson().getTitle() == null) {
            return "Learning activity";
        }
        return progress.getLesson().getTitle();
    }

    private String safeCategory(Course course) {
        if (course == null || course.getCategory() == null || course.getCategory().getName() == null) {
            return "Uncategorized";
        }
        return course.getCategory().getName();
    }
}
