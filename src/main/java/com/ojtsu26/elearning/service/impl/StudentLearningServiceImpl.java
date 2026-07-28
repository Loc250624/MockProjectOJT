package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.dto.request.VideoProgressRequestDTO;
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
import com.ojtsu26.elearning.service.ai.AiTutorLessonContext;
import com.ojtsu26.elearning.common.VideoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentLearningServiceImpl implements StudentLearningService {

    private static final int VIDEO_COMPLETION_THRESHOLD_PERCENT = 90;
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
                .courseStatus(courseStatus(courseProgress))
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
    public AiTutorLessonContext getAuthorizedAiTutorLessonContext(Integer lessonId) {
        if (lessonId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lesson ID is required.");
        }
        Lesson referencedLesson = lessonRepository.findByIdWithCourseAndAssessment(lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available."));
        if (referencedLesson.getCourse() == null || referencedLesson.getCourse().getId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Lesson is not available.");
        }
        AccessContext context = requireAccess(referencedLesson.getCourse().getId());
        List<Lesson> lessons = orderedLessons(context.course().getId());
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        assertLessonAccessible(context.enrollment(), lessons, lesson);
        return new AiTutorLessonContext(
                context.course().getId(),
                lesson.getId(),
                context.course().getTitle(),
                lesson.getOrderIndex() == null ? "" : "Lesson " + lesson.getOrderIndex(),
                lesson.getTitle(),
                plainText(context.course().getDescription()),
                plainText(lesson.getContent())
        );
    }

    @Override
    @Transactional
    public LearningProgressDTO recordVideoProgress(Integer courseId, Integer lessonId, VideoProgressRequestDTO request) {
        AccessContext context = requireAccess(courseId);
        List<Lesson> lessons = orderedLessons(courseId);
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        assertLessonAccessible(context.enrollment(), lessons, lesson);
        if (lesson.getType() != LessonType.VIDEO || lesson.getVideo() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only video lessons accept playback progress.");
        }
        VideoProgressInput input = validateVideoProgressRequest(request, lesson.getVideo().getDurationSeconds());
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        int previousMax = maxStoredReachedSeconds(progress);
        int maxReached = Math.max(previousMax, input.maxReachedSeconds());
        progress.setLastPositionSeconds(input.currentTimeSeconds());
        progress.setMaxReachedSeconds(maxReached);
        progress.setWatchedSeconds(maxReached);
        progress.setDurationSeconds(input.durationSeconds());
        progress.setLastAccessedAt(LocalDateTime.now());
        if (!Boolean.TRUE.equals(progress.getIsCompleted()) && isVideoComplete(progress, lesson, input.eventType())) {
            markCompleted(progress);
        }
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);
        return toProgressDto(context, lessons, lesson, progress, snapshot);
    }

    @Override
    @Transactional
    public LearningProgressDTO completeLesson(Integer courseId, Integer lessonId) {
        AccessContext context = requireAccess(courseId);
        List<Lesson> lessons = orderedLessons(courseId);
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        assertLessonAccessible(context.enrollment(), lessons, lesson);
        if (lesson.getType() == LessonType.VIDEO) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video lessons complete from recorded playback progress.");
        }
        if (lesson.getType() == LessonType.QUIZ || lesson.getQuiz() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Assessment lessons must be completed through assessment results.");
        }
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        progress.setLastAccessedAt(LocalDateTime.now());
        markCompleted(progress);
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);
        return toProgressDto(context, lessons, lesson, progress, snapshot);
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
                    List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
                    CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(enrollment, lessons, progresses);
                    Integer resumeLessonId = resolveActiveLessonId(enrollment, lessons, null, progresses);
                    
                    // Resolve last accessed lesson title if present
                    Optional<LessonProgress> lastAccessedProgress = progresses.stream()
                            .filter(p -> p.getLastAccessedAt() != null && p.getLesson() != null)
                            .max(Comparator.comparing(LessonProgress::getLastAccessedAt));
                    String lastAccessedLessonTitle = null;
                    if (lastAccessedProgress.isPresent()) {
                        Lesson lastLesson = lastAccessedProgress.get().getLesson();
                        if (lessons.stream().anyMatch(l -> Objects.equals(l.getId(), lastLesson.getId()))) {
                            lastAccessedLessonTitle = lastLesson.getTitle();
                        }
                    }
                    
                    Course course = enrollment.getCourse();
                    return StudentCourseProgressCardDTO.builder()
                            .courseId(course.getId())
                            .courseTitle(course.getTitle())
                            .instructorName(course.getInstructor() == null ? null : course.getInstructor().getFullName())
                            .thumbnailUrl(course.getThumbnailUrl())
                            .enrolledAt(enrollment.getEnrolledAt())
                            .resumeLessonId(resumeLessonId)
                            .lastAccessedLessonTitle(lastAccessedLessonTitle)
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
                .filter(lesson -> lesson.getType() != LessonType.RETIRED)
                .sorted(Comparator.comparing(Lesson::getOrderIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private Integer resolveActiveLessonId(CourseEnrollment enrollment, List<Lesson> lessons, Integer requestedLessonId) {
        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
        return resolveActiveLessonId(enrollment, lessons, requestedLessonId, progresses);
    }

    private Integer resolveActiveLessonId(CourseEnrollment enrollment, List<Lesson> lessons, Integer requestedLessonId, List<LessonProgress> progresses) {
        if (lessons.isEmpty()) {
            return null;
        }
        Map<Integer, LessonAccessState> accessByLesson = lessonAccessStates(lessons, progresses);
        if (requestedLessonId != null) {
            Lesson requestedLesson = requireLessonInCourse(lessons, requestedLessonId);
            assertLessonAccessible(accessByLesson.get(requestedLesson.getId()));
            return requestedLessonId;
        }

        Set<Integer> completedLessonIds = progresses.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCompleted()) && p.getLesson() != null)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        Optional<LessonProgress> lastAccessed = progresses.stream()
                .filter(p -> p.getLastAccessedAt() != null && p.getLesson() != null)
                .max(Comparator.comparing(LessonProgress::getLastAccessedAt));

        if (lastAccessed.isPresent()) {
            Lesson lastLesson = lastAccessed.get().getLesson();
            LessonAccessState state = accessByLesson.get(lastLesson.getId());
            if (lessons.stream().anyMatch(l -> Objects.equals(l.getId(), lastLesson.getId()))
                    && state != null
                    && state.accessible()
                    && !completedLessonIds.contains(lastLesson.getId())) {
                return lastLesson.getId();
            }
        }

        return requiredLessons(lessons).stream()
                .filter(lesson -> {
                    LessonAccessState state = accessByLesson.get(lesson.getId());
                    return state != null && state.accessible();
                })
                .filter(lesson -> !completedLessonIds.contains(lesson.getId()))
                .map(Lesson::getId)
                .findFirst()
                .or(() -> lastAccessed
                        .map(LessonProgress::getLesson)
                        .filter(lastLesson -> lessons.stream().anyMatch(l -> Objects.equals(l.getId(), lastLesson.getId())))
                        .filter(lastLesson -> {
                            LessonAccessState state = accessByLesson.get(lastLesson.getId());
                            return state != null && state.accessible();
                        })
                        .map(Lesson::getId))
                .orElseGet(() -> lessons.stream()
                        .filter(lesson -> {
                            LessonAccessState state = accessByLesson.get(lesson.getId());
                            return state != null && state.accessible();
                        })
                        .map(Lesson::getId)
                        .findFirst()
                        .orElse(lessons.get(0).getId()));
    }

    private StudentLearningLessonDTO openLessonInternal(AccessContext context, List<Lesson> lessons, Integer lessonId) {
        Lesson lesson = requireLessonInCourse(lessons, lessonId);
        List<LessonProgress> existingProgresses = lessonProgressRepository.findByEnrollmentId(context.enrollment().getId());
        Map<Integer, LessonAccessState> accessByLesson = lessonAccessStates(lessons, existingProgresses);
        assertLessonAccessible(accessByLesson.get(lesson.getId()));
        LessonProgress progress = findOrCreateProgressForUpdate(context.enrollment(), lesson);
        progress.setLastAccessedAt(LocalDateTime.now());
        lessonProgressRepository.save(progress);
        CourseProgressSnapshot snapshot = calculateAndStoreCourseProgress(context.enrollment(), lessons);

        Lesson previousLesson = previousRequiredLesson(lessons, lesson);
        Lesson nextLesson = nextRequiredLesson(lessons, lesson);
        LessonAccessState activeAccess = accessByLesson.get(lesson.getId());
        LessonAccessState nextAccess = nextLesson == null ? null : accessByLesson.get(nextLesson.getId());

        String rawVideoUrl = lesson.getVideo() == null ? null : lesson.getVideo().getVideoUrl();
        VideoUtils.VideoSourceInfo videoSource = VideoUtils.classifyVideoSource(rawVideoUrl);
        String embedUrl = videoSource.playable() ? VideoUtils.generateEmbedUrl(rawVideoUrl) : null;

        return StudentLearningLessonDTO.builder()
                .id(lesson.getId())
                .courseId(context.course().getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .orderIndex(lesson.getOrderIndex())
                .sanitizedContent(sanitizeRichText(lesson.getContent()))
                .videoUrl(rawVideoUrl)
                .embedUrl(embedUrl)
                .videoSourceType(videoSource.type().name())
                .videoPlayable(videoSource.playable())
                .videoUnavailableReason(videoSource.message())
                .videoDurationSeconds(lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds())
                .watchedSeconds(maxStoredReachedSeconds(progress))
                .lastPositionSeconds(lastStoredPositionSeconds(progress))
                .maxReachedSeconds(maxStoredReachedSeconds(progress))
                .previousLessonId(previousLesson == null ? null : previousLesson.getId())
                .nextLessonId(nextLesson == null ? null : nextLesson.getId())
                .nextLessonAccessible(nextAccess == null ? null : nextAccess.accessible())
                .nextLessonLockReason(nextAccess == null ? null : nextAccess.lockReason())
                .required(activeAccess == null ? false : activeAccess.required())
                .accessible(activeAccess == null ? true : activeAccess.accessible())
                .locked(activeAccess != null && activeAccess.locked())
                .lockReason(activeAccess == null ? null : activeAccess.lockReason())
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .courseProgress(toCourseOnlyProgressDto(context, snapshot))
                .resources(Collections.emptyList())
                .build();
    }

    private List<StudentLearningLessonSummaryDTO> toLessonSummaries(CourseEnrollment enrollment, List<Lesson> lessons, Integer activeLessonId) {
        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
        Map<Integer, LessonProgress> progressByLesson = progresses.stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(), (left, right) -> right));
        Map<Integer, LessonAccessState> accessByLesson = lessonAccessStates(lessons, progresses);

        return lessons.stream()
                .map(lesson -> {
                    LessonProgress progress = progressByLesson.get(lesson.getId());
                    LessonAccessState access = accessByLesson.get(lesson.getId());
                    return StudentLearningLessonSummaryDTO.builder()
                            .id(lesson.getId())
                            .title(lesson.getTitle())
                            .type(lesson.getType())
                            .orderIndex(lesson.getOrderIndex())
                            .completed(progress != null && Boolean.TRUE.equals(progress.getIsCompleted()))
                            .current(Objects.equals(lesson.getId(), activeLessonId))
                            .required(access == null ? false : access.required())
                            .accessible(access == null ? true : access.accessible())
                            .locked(access != null && access.locked())
                            .lockReason(access == null ? null : access.lockReason())
                            .watchedSeconds(progress == null ? 0 : maxStoredReachedSeconds(progress))
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

    private String plainText(String value) {
        String sanitized = sanitizeRichText(value);
        String withoutTags = Pattern.compile("(?is)<[^>]+>").matcher(sanitized).replaceAll(" ");
        return HtmlUtils.htmlUnescape(withoutTags).replaceAll("\\s+", " ").trim();
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

    private void assertLessonAccessible(CourseEnrollment enrollment, List<Lesson> lessons, Lesson lesson) {
        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
        assertLessonAccessible(lessonAccessStates(lessons, progresses).get(lesson.getId()));
    }

    private void assertLessonAccessible(LessonAccessState state) {
        if (state != null && !state.accessible()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, state.lockReason());
        }
    }

    private Map<Integer, LessonAccessState> lessonAccessStates(List<Lesson> lessons, List<LessonProgress> progresses) {
        Set<Integer> requiredLessonIds = requiredLessons(lessons).stream()
                .map(Lesson::getId)
                .collect(Collectors.toSet());
        Set<Integer> completedLessonIds = progresses.stream()
                .filter(progress -> progress.getLesson() != null && Boolean.TRUE.equals(progress.getIsCompleted()))
                .map(progress -> progress.getLesson().getId())
                .collect(Collectors.toSet());

        return lessons.stream()
                .collect(Collectors.toMap(
                        Lesson::getId,
                        lesson -> {
                            Lesson previousRequired = previousRequiredLesson(lessons, lesson);
                            boolean accessible = previousRequired == null || completedLessonIds.contains(previousRequired.getId());
                            return new LessonAccessState(
                                    requiredLessonIds.contains(lesson.getId()),
                                    accessible,
                                    !accessible,
                                    accessible ? null : lockedLessonReason(previousRequired)
                            );
                        },
                        (left, right) -> right
                ));
    }

    private String lockedLessonReason(Lesson previousRequired) {
        String title = previousRequired == null || previousRequired.getTitle() == null
                ? "the previous required lesson"
                : "\"" + previousRequired.getTitle() + "\"";
        return "Complete " + title + " to unlock this lesson.";
    }

    private Lesson previousRequiredLesson(List<Lesson> lessons, Lesson lesson) {
        int lessonIndex = indexOfLesson(lessons, lesson);
        Lesson previous = null;
        for (Lesson candidate : requiredLessons(lessons)) {
            int candidateIndex = indexOfLesson(lessons, candidate);
            if (candidateIndex >= 0 && candidateIndex < lessonIndex) {
                previous = candidate;
            }
        }
        return previous;
    }

    private Lesson nextRequiredLesson(List<Lesson> lessons, Lesson lesson) {
        int lessonIndex = indexOfLesson(lessons, lesson);
        return requiredLessons(lessons).stream()
                .filter(candidate -> indexOfLesson(lessons, candidate) > lessonIndex)
                .findFirst()
                .orElse(null);
    }

    private int indexOfLesson(List<Lesson> lessons, Lesson lesson) {
        for (int i = 0; i < lessons.size(); i++) {
            if (Objects.equals(lessons.get(i).getId(), lesson.getId())) {
                return i;
            }
        }
        return -1;
    }

    private LessonProgress findOrCreateProgressForUpdate(CourseEnrollment enrollment, Lesson lesson) {
        return lessonProgressRepository
                .findByEnrollmentIdAndLessonIdForUpdate(enrollment.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .isCompleted(false)
                        .watchedSeconds(0)
                        .lastPositionSeconds(0)
                        .maxReachedSeconds(0)
                        .build());
    }

    private VideoProgressInput validateVideoProgressRequest(VideoProgressRequestDTO request, Integer storedDurationSeconds) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video progress is required.");
        }

        int current = firstNonNull(request.getCurrentTimeSeconds(), request.getWatchedSeconds(), 0);
        int requestedMax = firstNonNull(request.getMaxReachedSeconds(), request.getWatchedSeconds(), current);
        int maxReached = Math.max(current, requestedMax);
        int duration = firstNonNull(request.getDurationSeconds(), storedDurationSeconds, 0);

        validateVideoSeconds(current, "Invalid video position.");
        validateVideoSeconds(maxReached, "Invalid watched position.");
        if (duration < 0 || duration > MAX_REASONABLE_VIDEO_SECONDS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid video duration.");
        }
        if (duration > 0 && (current > duration || maxReached > duration)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Video progress cannot exceed the lesson duration.");
        }

        String eventType = request.getEventType() == null ? "TIME_UPDATE" : request.getEventType().trim().toUpperCase();
        Set<String> allowedEvents = Set.of("TIME_UPDATE", "PAUSE", "SEEKED", "ENDED", "UNLOAD");
        if (!allowedEvents.contains(eventType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported video progress event.");
        }

        return new VideoProgressInput(current, maxReached, duration, eventType);
    }

    private void validateVideoSeconds(int seconds, String message) {
        if (seconds < 0 || seconds > MAX_REASONABLE_VIDEO_SECONDS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private boolean isVideoComplete(LessonProgress progress, Lesson lesson, String eventType) {
        Integer durationSeconds = progress.getDurationSeconds() != null && progress.getDurationSeconds() > 0
                ? progress.getDurationSeconds()
                : lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds();
        if (durationSeconds == null || durationSeconds <= 0) {
            return false;
        }
        if ("ENDED".equals(eventType)) {
            return true;
        }
        return maxStoredReachedSeconds(progress) * 100 >= durationSeconds * VIDEO_COMPLETION_THRESHOLD_PERCENT;
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
        boolean alreadyCompleted = Boolean.TRUE.equals(enrollment.getIsCompleted());
        BigDecimal percentage = alreadyCompleted ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP) : (total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        boolean completedCourse = alreadyCompleted || (total > 0 && completed == total);
        boolean newlyCompleted = !alreadyCompleted && completedCourse;
        enrollment.setProgressPercentage(percentage);
        enrollment.setIsCompleted(completedCourse);
        enrollmentRepository.save(enrollment);
        if (newlyCompleted) {
            notificationService.createCourseCompletedNotification(enrollment);
            try {
                certificateService.issueAutomaticallyIfEligible(enrollment.getId());
            } catch (BusinessException ignored) {
                // Certificate issuance can be retried through the idempotent claim endpoint.
            }
        }
        boolean startedCourse = progressByLesson.values().stream().anyMatch(this::hasStartedProgress);
        return new CourseProgressSnapshot(completed, total, percentage, completedCourse, startedCourse);
    }

    private CourseProgressSnapshot calculateAndStoreCourseProgress(CourseEnrollment enrollment, List<Lesson> lessons, List<LessonProgress> progresses) {
        List<Lesson> requiredLessons = requiredLessons(lessons);
        Map<Integer, LessonProgress> progressByLesson = progresses.stream()
                .filter(progress -> progress.getLesson() != null)
                .collect(Collectors.toMap(progress -> progress.getLesson().getId(), Function.identity(), (left, right) -> right));
        int total = requiredLessons.size();
        int completed = (int) requiredLessons.stream()
                .filter(lesson -> {
                    LessonProgress progress = progressByLesson.get(lesson.getId());
                    return progress != null && Boolean.TRUE.equals(progress.getIsCompleted());
                })
                .count();
        boolean alreadyCompleted = Boolean.TRUE.equals(enrollment.getIsCompleted());
        BigDecimal percentage = alreadyCompleted ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP) : (total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        boolean completedCourse = alreadyCompleted || (total > 0 && completed == total);
        boolean newlyCompleted = !alreadyCompleted && completedCourse;
        enrollment.setProgressPercentage(percentage);
        enrollment.setIsCompleted(completedCourse);
        enrollmentRepository.save(enrollment);
        if (newlyCompleted) {
            notificationService.createCourseCompletedNotification(enrollment);
            try {
                certificateService.issueAutomaticallyIfEligible(enrollment.getId());
            } catch (BusinessException ignored) {
                // Certificate issuance can be retried through the idempotent claim endpoint.
            }
        }
        boolean startedCourse = progressByLesson.values().stream().anyMatch(this::hasStartedProgress);
        return new CourseProgressSnapshot(completed, total, percentage, completedCourse, startedCourse);
    }

    private List<Lesson> requiredLessons(List<Lesson> lessons) {
        return lessons.stream()
                .filter(this::isRequiredLesson)
                .toList();
    }

    private boolean isRequiredLesson(Lesson lesson) {
        return lesson != null;
    }

    private LearningProgressDTO toProgressDto(AccessContext context, List<Lesson> lessons, Lesson lesson, LessonProgress progress, CourseProgressSnapshot snapshot) {
        List<LessonProgress> progresses = lessonProgressRepository.findByEnrollmentId(context.enrollment().getId());
        Map<Integer, LessonAccessState> accessByLesson = lessonAccessStates(lessons, progresses);
        Lesson nextLesson = nextRequiredLesson(lessons, lesson);
        LessonAccessState nextAccess = nextLesson == null ? null : accessByLesson.get(nextLesson.getId());
        return LearningProgressDTO.builder()
                .courseId(context.course().getId())
                .lessonId(lesson.getId())
                .enrollmentId(context.enrollment().getId())
                .completedLessons(snapshot.completedLessons())
                .totalLessons(snapshot.totalLessons())
                .progressPercentage(snapshot.percentage())
                .completed(Boolean.TRUE.equals(progress.getIsCompleted()))
                .lessonCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .courseCompleted(snapshot.completed())
                .courseStatus(courseStatus(snapshot))
                .lessonProgressPercentage(videoLessonProgressPercentage(progress, lesson))
                .watchedSeconds(maxStoredReachedSeconds(progress))
                .lastPositionSeconds(lastStoredPositionSeconds(progress))
                .maxReachedSeconds(maxStoredReachedSeconds(progress))
                .videoDurationSeconds(lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds())
                .durationSeconds(progress.getDurationSeconds() == null ? lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds() : progress.getDurationSeconds())
                .nextLessonId(nextLesson == null ? null : nextLesson.getId())
                .nextLessonAccessible(nextAccess == null ? null : nextAccess.accessible())
                .nextLessonLockReason(nextAccess == null ? null : nextAccess.lockReason())
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
                .courseCompleted(snapshot.completed())
                .courseStatus(courseStatus(snapshot))
                .build();
    }

    private BigDecimal videoLessonProgressPercentage(LessonProgress progress, Lesson lesson) {
        Integer durationSeconds = progress.getDurationSeconds() == null || progress.getDurationSeconds() <= 0
                ? lesson.getVideo() == null ? null : lesson.getVideo().getDurationSeconds()
                : progress.getDurationSeconds();
        if (durationSeconds == null || durationSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal percentage = BigDecimal.valueOf(maxStoredReachedSeconds(progress))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(durationSeconds), 2, RoundingMode.HALF_UP);
        return percentage.compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.valueOf(100) : percentage;
    }

    private String courseStatus(CourseProgressSnapshot snapshot) {
        if (snapshot.completed()) {
            return "COMPLETED";
        }
        if (snapshot.started() || snapshot.percentage().compareTo(BigDecimal.ZERO) > 0) {
            return "IN_PROGRESS";
        }
        return "NOT_STARTED";
    }

    private boolean hasStartedProgress(LessonProgress progress) {
        return progress != null
                && (progress.getLastAccessedAt() != null
                || Boolean.TRUE.equals(progress.getIsCompleted())
                || maxStoredReachedSeconds(progress) > 0
                || valueOrZero(progress.getLastPositionSeconds()) > 0);
    }

    private int maxStoredReachedSeconds(LessonProgress progress) {
        return Math.max(valueOrZero(progress.getMaxReachedSeconds()), valueOrZero(progress.getWatchedSeconds()));
    }

    private int lastStoredPositionSeconds(LessonProgress progress) {
        return progress.getLastPositionSeconds() == null ? maxStoredReachedSeconds(progress) : progress.getLastPositionSeconds();
    }

    private int firstNonNull(Integer first, Integer second, int fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }

    private int firstNonNull(Integer first, Integer second, Integer fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback == null ? 0 : fallback;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record AccessContext(User student, Course course, CourseEnrollment enrollment) {
    }

    private record CourseProgressSnapshot(int completedLessons, int totalLessons, BigDecimal percentage, boolean completed, boolean started) {
    }

    private record VideoProgressInput(int currentTimeSeconds, int maxReachedSeconds, int durationSeconds, String eventType) {
    }

    private record LessonAccessState(boolean required, boolean accessible, boolean locked, String lockReason) {
    }
}
