package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.response.LearningProgressDTO;
import com.ojtsu26.elearning.dto.response.StudentCourseProgressCardDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningCourseDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonDTO;
import com.ojtsu26.elearning.dto.response.StudentLearningLessonSummaryDTO;
import com.ojtsu26.elearning.exception.BusinessException;
import com.ojtsu26.elearning.exception.ErrorCode;
import com.ojtsu26.elearning.model.entity.Course;
import com.ojtsu26.elearning.model.entity.CourseEnrollment;
import com.ojtsu26.elearning.model.entity.Lesson;
import com.ojtsu26.elearning.model.entity.LessonProgress;
import com.ojtsu26.elearning.model.entity.User;
import com.ojtsu26.elearning.model.enums.CourseStatus;
import com.ojtsu26.elearning.model.enums.LessonType;
import com.ojtsu26.elearning.model.enums.Role;
import com.ojtsu26.elearning.model.enums.UserStatus;
import com.ojtsu26.elearning.repository.CourseEnrollmentRepository;
import com.ojtsu26.elearning.repository.CourseRepository;
import com.ojtsu26.elearning.repository.LessonProgressRepository;
import com.ojtsu26.elearning.repository.LessonRepository;
import com.ojtsu26.elearning.service.CertificateService;
import com.ojtsu26.elearning.service.CurrentUserService;
import com.ojtsu26.elearning.service.NotificationService;
import com.ojtsu26.elearning.service.StudentLearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentLearningServiceImpl implements StudentLearningService {

    private static final int VIDEO_COMPLETION_THRESHOLD_PERCENT = 90;
    private static final int MAX_VIDEO_ADVANCE_SECONDS = 45;
    private static final int MAX_REASONABLE_VIDEO_SECONDS = 24 * 60 * 60;
    private static final Pattern SCRIPT_OR_STYLE_BLOCK = Pattern.compile("(?is)<\\s*(script|style)[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    private static final Pattern EVENT_HANDLER_ATTRIBUTE = Pattern.compile("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");
    private static final Pattern JAVASCRIPT_URL = Pattern.compile("(?i)(href|src)\\s*=\\s*(\"|')\\s*javascript:[^\"']*(\"|')");

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final CertificateService certificateService;

    @Override
    @Transactional
    public StudentLearningCourseDTO getLearningCourse(Integer courseId, Integer lessonId) {
        AccessContext context = requireAccess(courseId);
        List<Lesson> lessons = orderedLessons(courseId);
        Integer activeLessonId = resolveActiveLessonId(context.enrollment(), lessons, lessonId);
        StudentLearningLessonDTO activeLesson = activeLessonId == null ? null : openLessonInternal(context, lessons, activeLessonId);
        LessonProgress lastAccessed = lessonProgressRepository
                .findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(context.enrollment().getId())
                .orElse(null);
        CourseProgressSnapshot courseProgress = calculateAndStoreCourseProgress(context.enrollment(), lessons);

        return StudentLearningCourseDTO.builder()
                .courseId(context.course().getId())
                .courseTitle(context.course().getTitle())
                .instructorName(context.course().getInstructor() == null ? null : context.course().getInstructor().getFullName())
                .enrollmentId(context.enrollment().getId())
                .activeLessonId(activeLessonId)
                .lastAccessedLessonId(lastAccessed == null || lastAccessed.getLesson() == null ? null : lastAccessed.getLesson().getId())
                .completedLessons(courseProgress.completedLessons())
                .totalLessons(courseProgress.totalLessons())
                .progressPercentage(courseProgress.percentage())
                .completed(courseProgress.completed())
                .activeLesson(activeLesson)
                .lessons(toLessonSummaries(context.enrollment(), lessons, activeLessonId))
                .courseResources(Collections.emptyList())
                .build();
    }

    @Override
    @Transactional
    public StudentLearningLessonDTO openLesson(Integer courseId, Integer lessonId) {
        AccessContext context = requireAccess(courseId);
        return openLessonInternal(context, orderedLessons(courseId), lessonId);
    }

    @Override
    @Transactional
    public LearningProgressDTO recordVideoProgress(Integer courseId, Integer lessonId, Integer watchedSeconds) {
        AccessContext context = requireAccess(courseId);
        List<Lesson> lessons = orderedLessons(courseId);
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        if (lesson.getType() != LessonType.VIDEO || lesson.getVideo() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only video lessons accept playback progress.");
        }
        int validWatchedSeconds = validateWatchedSeconds(watchedSeconds);
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        int previous = valueOrZero(progress.getWatchedSeconds());
        int cappedByRequestOrder = Math.max(previous, validWatchedSeconds);
        int normalized = normalizeVideoPosition(cappedByRequestOrder, previous, lesson.getVideo().getDurationSeconds());
        progress.setWatchedSeconds(Math.max(previous, normalized));
        progress.setLastAccessedAt(LocalDateTime.now());
        if (!Boolean.TRUE.equals(progress.getIsCompleted()) && isVideoComplete(progress, lesson)) {
            markCompleted(progress);
        }
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);
        return toProgressDto(context, lesson, progress, snapshot);
    }

    @Override
    @Transactional
    public LearningProgressDTO completeLesson(Integer courseId, Integer lessonId) {
        AccessContext context = requireAccess(courseId);
        List<Lesson> lessons = orderedLessons(courseId);
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        if (lesson.getType() == LessonType.VIDEO) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video lessons complete from recorded playback progress.");
        }
        if (lesson.getType() == LessonType.QUIZ || lesson.getQuiz() != null || lesson.getCodingassignment() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Assessment lessons must be completed through assessment results.");
        }
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        progress.setLastAccessedAt(LocalDateTime.now());
        markCompleted(progress);
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);
        return toProgressDto(context, lesson, progress, snapshot);
    }

    @Override
    @Transactional
    public LearningProgressDTO getCourseProgress(Integer courseId) {
        AccessContext context = requireAccess(courseId);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), orderedLessons(courseId));
        return LearningProgressDTO.builder()
                .courseId(courseId)
                .enrollmentId(context.enrollment().getId())
                .completedLessons(snapshot.completedLessons())
                .totalLessons(snapshot.totalLessons())
                .progressPercentage(snapshot.percentage())
                .completed(snapshot.completed())
                .build();
    }

    @Override
    @Transactional
    public List<StudentCourseProgressCardDTO> getCurrentStudentCourseCards() {
        User student = currentUserService.getCurrentUser();
        validateActiveStudent(student);
        return enrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .map(enrollment -> {
                    List<Lesson> lessons = orderedLessons(enrollment.getCourse().getId());
                    CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(enrollment, lessons);
                    Integer resumeLessonId = resolveActiveLessonId(enrollment, lessons, null);
                    Course course = enrollment.getCourse();
                    return StudentCourseProgressCardDTO.builder()
                            .courseId(course.getId())
                            .courseTitle(course.getTitle())
                            .instructorName(course.getInstructor() == null ? null : course.getInstructor().getFullName())
                            .thumbnailUrl(course.getThumbnailUrl())
                            .enrolledAt(enrollment.getEnrolledAt())
                            .resumeLessonId(resumeLessonId)
                            .completedLessons(snapshot.completedLessons())
                            .totalLessons(snapshot.totalLessons())
                            .progressPercentage(snapshot.percentage())
                            .completed(snapshot.completed())
                            .build();
                })
                .toList();
    }

    private AccessContext requireAccess(Integer courseId) {
        User student = currentUserService.getCurrentUser();
        validateActiveStudent(student);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getStatus() != CourseStatus.APPROVED) {
            throw new BusinessException(ErrorCode.COURSE_UNAVAILABLE);
        }

        CourseEnrollment enrollment = enrollmentRepository.findByStudentIdAndCourseIdForUpdate(student.getId(), courseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED, "You are not enrolled in this course."));
        return new AccessContext(student, course, enrollment);
    }

    private void validateActiveStudent(User user) {
        if (user.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Only active students can access learning content.");
        }
    }

    private List<Lesson> orderedLessons(Integer courseId) {
        return lessonRepository.findByCourseIdWithVideoOrderByOrderIndexAsc(courseId).stream()
                .sorted(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Lesson::getId))
                .toList();
    }

    private Integer resolveActiveLessonId(CourseEnrollment enrollment, List<Lesson> lessons, Integer requestedLessonId) {
        if (lessons.isEmpty()) {
            return null;
        }
        if (requestedLessonId != null) {
            boolean requestedBelongsToCourse = lessons.stream().anyMatch(lesson -> Objects.equals(lesson.getId(), requestedLessonId));
            if (!requestedBelongsToCourse) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available in this course.");
            }
            return requestedLessonId;
        }
        return lessonProgressRepository
                .findTopByEnrollmentIdAndLastAccessedAtIsNotNullOrderByLastAccessedAtDesc(enrollment.getId())
                .map(LessonProgress::getLesson)
                .filter(Objects::nonNull)
                .map(Lesson::getId)
                .filter(lastLessonId -> lessons.stream().anyMatch(lesson -> Objects.equals(lesson.getId(), lastLessonId)))
                .orElseGet(() -> lessons.stream()
                        .filter(lesson -> {
                            LessonProgress progress = lessonProgressRepository
                                    .findByEnrollmentIdAndLessonId(enrollment.getId(), lesson.getId())
                                    .orElse(null);
                            return progress == null || !Boolean.TRUE.equals(progress.getIsCompleted());
                        })
                        .map(Lesson::getId)
                        .findFirst()
                        .orElse(lessons.get(0).getId()));
    }

    private StudentLearningLessonDTO openLessonInternal(AccessContext context, List<Lesson> lessons, Integer lessonId) {
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);

        Integer previousLessonId = null;
        Integer nextLessonId = null;
        for (int i = 0; i < lessons.size(); i++) {
            if (Objects.equals(lessons.get(i).getId(), lesson.getId())) {
                previousLessonId = i > 0 ? lessons.get(i - 1).getId() : null;
                nextLessonId = i < lessons.size() - 1 ? lessons.get(i + 1).getId() : null;
                break;
            }
        }

        return StudentLearningLessonDTO.builder()
                .id(lesson.getId())
                .courseId(context.course().getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .orderIndex(lesson.getOrderIndex())
                .sanitizedContent(sanitizeRichText(lesson.getContent()))
                .videoUrl(lesson.getVideo() == null ? null : lesson.getVideo().getVideoUrl())
                .videoDurationSeconds(lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds())
                .watchedSeconds(valueOrZero(progress.getWatchedSeconds()))
                .previousLessonId(previousLessonId)
                .nextLessonId(nextLessonId)
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .courseProgress(toCourseOnlyProgressDto(context, snapshot))
                .resources(Collections.emptyList())
                .build();
    }

    private List<StudentLearningLessonSummaryDTO> toLessonSummaries(CourseEnrollment enrollment, List<Lesson> lessons, Integer activeLessonId) {
        Map<Integer, LessonProgress> progressByLesson = lessonProgressRepository.findByEnrollmentId(enrollment.getId()).stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(), (left, right) -> right));

        return lessons.stream()
                .map(lesson -> {
                    LessonProgress progress = progressByLesson.get(lesson.getId());
                    return StudentLearningLessonSummaryDTO.builder()
                            .id(lesson.getId())
                            .title(lesson.getTitle())
                            .type(lesson.getType())
                            .orderIndex(lesson.getOrderIndex())
                            .completed(progress != null && Boolean.TRUE.equals(progress.getIsCompleted()))
                            .current(Objects.equals(lesson.getId(), activeLessonId))
                            .watchedSeconds(progress == null ? 0 : valueOrZero(progress.getWatchedSeconds()))
                            .videoDurationSeconds(lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds())
                            .build();
                })
                .toList();
    }

    private String sanitizeRichText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String sanitized = SCRIPT_OR_STYLE_BLOCK.matcher(value).replaceAll("");
        sanitized = EVENT_HANDLER_ATTRIBUTE.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_URL.matcher(sanitized).replaceAll("$1=\"#\"");
        sanitized = removeDisallowedTags(sanitized);
        return sanitized;
    }

    private String removeDisallowedTags(String value) {
        Set<String> allowedTags = Set.of(
                "p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li",
                "h2", "h3", "h4", "blockquote", "code", "pre", "a", "span"
        );
        return Pattern.compile("(?i)</?([a-z0-9]+)(\\s[^>]*)?>")
                .matcher(value)
                .replaceAll(match -> allowedTags.contains(match.group(1).toLowerCase()) ? match.group(0) : "");
    }

    private Lesson requireLessonInCourse(List<Lesson> lessons, Integer lessonId) {
        return lessons.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), lessonId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available in this course."));
    }

    private LessonProgress findOrCreateProgressForUpdate(CourseEnrollment enrollment, Lesson lesson) {
        return lessonProgressRepository
                .findByEnrollmentIdAndLessonIdForUpdate(enrollment.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .isCompleted(false)
                        .watchedSeconds(0)
                        .build());
    }

    private int validateWatchedSeconds(Integer watchedSeconds) {
        if (watchedSeconds == null || watchedSeconds < 0 || watchedSeconds > MAX_REASONABLE_VIDEO_SECONDS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid watched position.");
        }
        return watchedSeconds;
    }

    private int normalizeVideoPosition(int requested, int previous, Integer durationSeconds) {
        int normalized = requested;
        if (durationSeconds != null && durationSeconds > 0) {
            normalized = Math.min(normalized, durationSeconds);
        }
        if (normalized > previous + MAX_VIDEO_ADVANCE_SECONDS) {
            normalized = previous + MAX_VIDEO_ADVANCE_SECONDS;
        }
        return normalized;
    }

    private boolean isVideoComplete(LessonProgress progress, Lesson lesson) {
        Integer durationSeconds = lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds();
        if (durationSeconds == null || durationSeconds <= 0) {
            return false;
        }
        return valueOrZero(progress.getWatchedSeconds()) * 100 >= durationSeconds * VIDEO_COMPLETION_THRESHOLD_PERCENT;
    }

    private void markCompleted(LessonProgress progress) {
        if (!Boolean.TRUE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }
    }

    private CourseProgressSnapshot calculateAndStoreCourseProgress(CourseEnrollment enrollment, List<Lesson> lessons) {
        List<Lesson> requiredLessons = requiredLessons(lessons);
        Map<Integer, LessonProgress> progressByLesson = lessonProgressRepository.findByEnrollmentId(enrollment.getId()).stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(), (left, right) -> right));
        int total = requiredLessons.size();
        int completed = (int) requiredLessons.stream()
                .filter(lesson -> {
                    LessonProgress progress = progressByLesson.get(lesson.getId());
                    return progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
                })
                .count();
        BigDecimal percentage = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        boolean completedCourse = total > 0 && completed == total;
        boolean newlyCompleted = !Boolean.TRUE.equals(enrollment.getIsCompleted()) && completedCourse;
        enrollment.setProgressPercentage(percentage);
        if (newlyCompleted) {
            enrollment.setIsCompleted(true);
        } else if (enrollment.getIsCompleted() == null) {
            enrollment.setIsCompleted(false);
        }
        enrollmentRepository.save(enrollment);
        if (newlyCompleted) {
            notificationService.createCourseCompletedNotification(enrollment);
            try {
                certificateService.issueAutomaticallyIfEligible(enrollment.getId());
            } catch (BusinessException ignored) {
                // Certificate issuance can be retried through the idempotent claim endpoint.
            }
        }
        return new CourseProgressSnapshot(completed, total, percentage, completedCourse);
    }

    private List<Lesson> requiredLessons(List<Lesson> lessons) {
        return lessons.stream()
                .filter(lesson -> lesson.getType() != LessonType.QUIZ && lesson.getQuiz() == null && lesson.getCodingassignment() == null)
                .toList();
    }

    private LearningProgressDTO toProgressDto(AccessContext context, Lesson lesson, LessonProgress progress, CourseProgressSnapshot snapshot) {
        return LearningProgressDTO.builder()
                .courseId(context.course().getId())
                .lessonId(lesson.getId())
                .enrollmentId(context.enrollment().getId())
                .completedLessons(snapshot.completedLessons())
                .totalLessons(snapshot.totalLessons())
                .progressPercentage(snapshot.percentage())
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .watchedSeconds(valueOrZero(progress.getWatchedSeconds()))
                .videoDurationSeconds(lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds())
                .completedAt(progress.getCompletedAt())
                .lastAccessedAt(progress.getLastAccessedAt())
                .build();
    }

    private LearningProgressDTO toCourseOnlyProgressDto(AccessContext context, CourseProgressSnapshot snapshot) {
        return LearningProgressDTO.builder()
                .courseId(context.course().getId())
                .enrollmentId(context.enrollment().getId())
                .completedLessons(snapshot.completedLessons())
                .totalLessons(snapshot.totalLessons())
                .progressPercentage(snapshot.percentage())
                .completed(snapshot.completed())
                .build();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record AccessContext(User student, Course course, CourseEnrollment enrollment) {
    }

    private record CourseProgressSnapshot(int completedLessons, int totalLessons, BigDecimal percentage, boolean completed) {
    }
}
